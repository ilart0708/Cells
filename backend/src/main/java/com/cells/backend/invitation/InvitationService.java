package com.cells.backend.invitation;

import com.cells.backend.auth.AuthResult;
import com.cells.backend.auth.AuthService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.church.ChurchStatus;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.InvalidInvitationException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.common.TokenHasher;
import com.cells.backend.config.InvitationProperties;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InvitationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InvitationRepository invitationRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final InvitationProperties properties;

    public InvitationService(InvitationRepository invitationRepository,
                             ChurchRepository churchRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             AuthService authService,
                             InvitationProperties properties) {
        this.invitationRepository = invitationRepository;
        this.churchRepository = churchRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.properties = properties;
    }

    @Transactional
    public InvitationResponse invitePastor(Long adminId, PastorInvitationRequest request) {
        Church church = churchRepository.findById(request.churchId())
                .orElseThrow(() -> new ResourceNotFoundException("Church not found."));
        if (church.getStatus() != ChurchStatus.ACTIVE) {
            throw new BusinessRuleException("This church is not active.");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with this email already exists.");
        }

        Instant now = Instant.now();
        invitationRepository.revokePendingForEmail(email, now); // a new link replaces the old one

        String token = TokenHasher.newToken();
        Invitation invitation = invitationRepository.save(new Invitation(
                TokenHasher.sha256(token), email, request.fullName().trim(),
                Role.PASTOR, church, adminId, now.plus(properties.ttl())));

        return InvitationResponse.from(invitation, now, inviteUrl(token));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvitationResponse> list(int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Instant now = Instant.now();
        return PageResponse.from(invitationRepository.findAll(pageable)
                .map(invitation -> InvitationResponse.from(invitation, now, null)));
    }

    @Transactional
    public void revoke(Long id) {
        Invitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        Instant now = Instant.now();
        if (invitation.getStatus(now) == InvitationStatus.ACCEPTED) {
            throw new BusinessRuleException("An accepted invitation cannot be revoked.");
        }
        invitation.markRevoked(now);
    }

    @Transactional(readOnly = true)
    public InvitationPreviewResponse preview(String token) {
        Invitation invitation = invitationRepository.findByTokenHash(TokenHasher.sha256(token))
                .orElseThrow(InvalidInvitationException::new);
        if (invitation.getStatus(Instant.now()) != InvitationStatus.PENDING) {
            throw new InvalidInvitationException();
        }
        return new InvitationPreviewResponse(
                invitation.getEmail(), invitation.getFullName(), invitation.getRole(),
                invitation.getChurch().getName(), invitation.getExpiresAt());
    }

    @Transactional
    public AuthResult accept(AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository
                .findByTokenHashForUpdate(TokenHasher.sha256(request.token()))
                .orElseThrow(InvalidInvitationException::new);

        Instant now = Instant.now();
        if (invitation.getStatus(now) != InvitationStatus.PENDING) {
            throw new InvalidInvitationException();
        }
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User user = userRepository.save(new User(
                invitation.getEmail(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                invitation.getRole()));
        invitation.markAccepted(now);

        // Step 3 adds the role-specific record (the pastor's church link) here.

        return authService.startSession(user);
    }

    private String inviteUrl(String token) {
        String base = properties.baseUrl().replaceAll("/+$", "");
        return base + "/accept-invitation#token=" + token;
    }
}