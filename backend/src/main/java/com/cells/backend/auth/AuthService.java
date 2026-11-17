package com.cells.backend.auth;

import com.cells.backend.common.AccountNotActiveException;
import com.cells.backend.common.InvalidCredentialsException;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserResponse;
import com.cells.backend.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }

        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResult(buildResponse(user), refreshToken);
    }

    // noRollbackFor: when a reused token is detected, the family revocation
    // must be committed even though we then answer 401.
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountNotActiveException.class})
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException();
        }

        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawToken)
                .orElseThrow(InvalidCredentialsException::new);

        User user = userRepository.findById(rotation.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeFamily(rotation.familyId());
            throw new AccountNotActiveException();
        }

        return new AuthResult(buildResponse(user), rotation.newToken());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokenService.revoke(rawToken);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        return UserResponse.from(user);
    }

    private LoginResponse buildResponse(User user) {
        TokenService.AccessToken token = tokenService.createAccessToken(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }

    /** Starts a session for an already-verified user (used after accepting an invitation). */
    @Transactional
    public AuthResult startSession(User user) {
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResult(buildResponse(user), refreshToken);
    }
}