package com.cells.backend.auth;

import com.cells.backend.config.RefreshProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final RefreshProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, RefreshProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Starts a new token family (used at login). Returns the raw token for the cookie. */
    @Transactional
    public String issue(Long userId) {
        return create(userId, UUID.randomUUID());
    }

    /**
     * Validates and rotates a token. Returns empty if the token is unknown, expired,
     * or already used. A reused token revokes its whole family.
     * Never throws on purpose: see the note about rollback in the Step 5 gotchas.
     */
    @Transactional
    public Optional<Rotation> rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken current = repository.findByTokenHash(hash(rawToken)).orElse(null);

        if (current == null) {
            return Optional.empty();
        }
        if (current.isRevoked()) {
            repository.revokeFamily(current.getFamilyId(), now); // reuse detected
            return Optional.empty();
        }
        if (current.isExpired(now)) {
            return Optional.empty();
        }

        current.revoke(now);
        String newRaw = create(current.getUserId(), current.getFamilyId());
        return Optional.of(new Rotation(current.getUserId(), current.getFamilyId(), newRaw));
    }

    /** Logout: revokes the whole family of the presented token. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeFamily(UUID familyId) {
        repository.revokeFamily(familyId, Instant.now());
    }

    private String create(Long userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(new RefreshToken(
                userId, hash(raw), familyId, Instant.now().plus(properties.ttl())));
        return raw;
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record Rotation(Long userId, UUID familyId, String newToken) {
    }
}