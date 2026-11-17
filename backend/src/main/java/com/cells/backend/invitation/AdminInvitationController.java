package com.cells.backend.invitation;

import com.cells.backend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/invitations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInvitationController {

    private final InvitationService invitationService;

    public AdminInvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> invitePastor(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PastorInvitationRequest request) {
        InvitationResponse created = invitationService.invitePastor(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity
                .created(URI.create("/api/v1/admin/invitations/" + created.id()))
                .header("Cache-Control", "no-store")
                .body(created);
    }

    @GetMapping
    public PageResponse<InvitationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invitationService.list(page, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        invitationService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}