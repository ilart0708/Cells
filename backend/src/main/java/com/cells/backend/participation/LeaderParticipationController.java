package com.cells.backend.participation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}")
@PreAuthorize("hasRole('LEADER')")
public class LeaderParticipationController {

    private final LeaderParticipationService participationService;
    private final SubmissionReadService submissionReadService;

    public LeaderParticipationController(LeaderParticipationService participationService,
                                         SubmissionReadService submissionReadService) {
        this.participationService = participationService;
        this.submissionReadService = submissionReadService;
    }

    /** Today's devotion and everyone's status, with email and account status. */
    @GetMapping("/participation/today")
    public LeaderBoardResponse board(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return participationService.board(leaderId(jwt), roomId);
    }

    /** "Exempt for Today" on. */
    @PutMapping("/members/{memberId}/exemption")
    public ExemptionResponse exempt(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                    @PathVariable Long memberId) {
        return participationService.exempt(leaderId(jwt), roomId, memberId);
    }

    /** "Exempt for Today" off. */
    @DeleteMapping("/members/{memberId}/exemption")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                       @PathVariable Long memberId) {
        participationService.revoke(leaderId(jwt), roomId, memberId);
        return ResponseEntity.noContent().build();
    }

    /** Every answer to one devotion, oldest first. */
    @GetMapping("/devotions/{devotionId}/submissions")
    public LeaderSubmissionsResponse submissions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                                 @PathVariable Long devotionId) {
        return submissionReadService.forLeader(leaderId(jwt), roomId, devotionId);
    }

    private static Long leaderId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}