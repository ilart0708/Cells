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
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        // Used to spend the same time on unknown emails as on real ones.
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
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

        TokenService.AccessToken token = tokenService.createAccessToken(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        return UserResponse.from(user);
    }
}