package com.cells.backend.invitation;

import com.cells.backend.auth.SessionResponseFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints: the invited person has no account yet. The link token is the credential. */
@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final SessionResponseFactory sessions;

    public InvitationController(InvitationService invitationService, SessionResponseFactory sessions) {
        this.invitationService = invitationService;
        this.sessions = sessions;
    }

    @PostMapping("/preview")
    public InvitationPreviewResponse preview(@Valid @RequestBody TokenRequest request) {
        return invitationService.preview(request.token());
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(
            @RequestHeader(name = SessionResponseFactory.CLIENT_HEADER, required = false) String clientType,
            @Valid @RequestBody AcceptInvitationRequest request) {
        return sessions.create(invitationService.accept(request), clientType);
    }
}
