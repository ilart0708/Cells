package com.cells.backend.leader;

import com.cells.backend.common.PageResponse;
import com.cells.backend.invitation.InvitationResponse;
import com.cells.backend.invitation.InvitationService;
import com.cells.backend.invitation.LeaderInvitationRequest;
import com.cells.backend.user.UserStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Everything here is scoped to the logged-in pastor: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/pastor")
@PreAuthorize("hasRole('PASTOR')")
public class PastorLeaderController {

    private final LeaderService leaderService;
    private final InvitationService invitationService;

    public PastorLeaderController(LeaderService leaderService, InvitationService invitationService) {
        this.leaderService = leaderService;
        this.invitationService = invitationService;
    }

    @PostMapping("/invitations")
    public ResponseEntity<InvitationResponse> inviteLeader(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LeaderInvitationRequest request) {
        InvitationResponse created = invitationService.inviteLeader(pastorId(jwt), request);
        return ResponseEntity
                .created(URI.create("/api/v1/pastor/invitations/" + created.id()))
                .header("Cache-Control", "no-store")
                .body(created);
    }

    @GetMapping("/invitations")
    public PageResponse<InvitationResponse> invitations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invitationService.listForInviter(pastorId(jwt), page, size);
    }

    @DeleteMapping("/invitations/{id}")
    public ResponseEntity<Void> revokeInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        invitationService.revokeOwn(id, pastorId(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/leaders")
    public PageResponse<LeaderResponse> leaders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return leaderService.listForPastor(pastorId(jwt), q, page, size);
    }

    @GetMapping("/leaders/{id}")
    public LeaderResponse leader(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return leaderService.getForPastor(pastorId(jwt), id);
    }

    @PatchMapping("/leaders/{id}/status")
    public LeaderResponse setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @Valid @RequestBody UserStatusRequest request) {
        return leaderService.setStatusForPastor(pastorId(jwt), id, request.status());
    }

    private static Long pastorId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}