package com.cells.backend.invitation;

import com.cells.backend.auth.AuthResult;
import com.cells.backend.auth.LoginResponse;
import com.cells.backend.auth.RefreshCookieFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints: the invited person has no account yet. The link token is the credential. */
@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final RefreshCookieFactory cookieFactory;

    public InvitationController(InvitationService invitationService, RefreshCookieFactory cookieFactory) {
        this.invitationService = invitationService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/preview")
    public InvitationPreviewResponse preview(@Valid @RequestBody TokenRequest request) {
        return invitationService.preview(request.token());
    }

    @PostMapping("/accept")
    public ResponseEntity<LoginResponse> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        AuthResult result = invitationService.accept(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.body());
    }
}