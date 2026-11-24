package com.cells.backend.account;

import com.cells.backend.auth.SessionResponseFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in person deletes their own account. The user id always comes from the token. */
@RestController
@RequestMapping("/api/v1/account")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final AccountService accountService;
    private final SessionResponseFactory sessions;

    public AccountController(AccountService accountService, SessionResponseFactory sessions) {
        this.accountService = accountService;
        this.sessions = sessions;
    }

    /** POST rather than DELETE-with-a-body, which some proxies drop. Answers 204 and clears the website cookie. */
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteOwnAccount(Long.valueOf(jwt.getSubject()), request.password());
        return sessions.loggedOut();
    }
}
