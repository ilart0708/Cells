package com.cells.backend.invitation;

import com.cells.backend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Invitations into one room. Scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/invitations")
@PreAuthorize("hasRole('LEADER')")
public class LeaderMemberInvitationController {

    private final InvitationService invitationService;

    public LeaderMemberInvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> invite(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @Valid @RequestBody MemberInvitationRequest request) {
        InvitationResponse created = invitationService.inviteMember(leaderId(jwt), roomId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/leader/rooms/" + roomId + "/invitations/" + created.id()))
                .header("Cache-Control", "no-store")
                .body(created);
    }

    @GetMapping
    public PageResponse<InvitationResponse> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invitationService.listForRoom(leaderId(jwt), roomId, page, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id) {
        invitationService.revokeForRoom(leaderId(jwt), roomId, id);
        return ResponseEntity.noContent().build();
    }

    private static Long leaderId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}