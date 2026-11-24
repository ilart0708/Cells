package com.cells.backend.auth;

import com.cells.backend.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionResponseFactory sessions;

    public AuthController(AuthService authService, SessionResponseFactory sessions) {
        this.authService = authService;
        this.sessions = sessions;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestHeader(name = SessionResponseFactory.CLIENT_HEADER, required = false) String clientType,
            @RequestHeader(name = SessionResponseFactory.ORIGIN_HEADER, required = false) String origin,
            @Valid @RequestBody LoginRequest request) {
        return sessions.create(authService.login(request), clientType, origin);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader(name = SessionResponseFactory.CLIENT_HEADER, required = false) String clientType,
            @RequestHeader(name = SessionResponseFactory.ORIGIN_HEADER, required = false) String origin,
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String refreshCookie,
            @Valid @RequestBody(required = false) RefreshRequest body) {
        String token = sessions.refreshTokenFrom(clientType, origin, refreshCookie, body);
        return sessions.create(authService.refresh(token), clientType, origin);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(name = SessionResponseFactory.CLIENT_HEADER, required = false) String clientType,
            @RequestHeader(name = SessionResponseFactory.ORIGIN_HEADER, required = false) String origin,
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String refreshCookie,
            @Valid @RequestBody(required = false) RefreshRequest body) {
        authService.logout(sessions.refreshTokenFrom(clientType, origin, refreshCookie, body));
        return sessions.loggedOut();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(Long.valueOf(jwt.getSubject()));
    }
}
