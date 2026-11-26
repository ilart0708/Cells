package com.cells.backend.participation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Scoped to the logged-in member: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/member/rooms/{roomId}")
@PreAuthorize("hasRole('MEMBER')")
public class MemberParticipationController {

    private final MemberParticipationService participationService;

    public MemberParticipationController(MemberParticipationService participationService) {
        this.participationService = participationService;
    }

    /** Today's devotion, my status, and everyone's status in participation order. */
    @GetMapping("/participation/today")
    public MemberBoardResponse board(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return participationService.board(memberId(jwt), roomId);
    }

    @PostMapping("/devotions/{devotionId}/submissions")
    public ResponseEntity<SubmissionResponse> submit(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long devotionId,
            @Valid @RequestBody SubmissionRequest request) {
        SubmissionResponse saved = participationService.submit(
                memberId(jwt), roomId, devotionId, request.content(), request.takeaway(), request.highlights());
        return ResponseEntity.status(HttpStatus.CREATED).header("Cache-Control", "no-store").body(saved);
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}