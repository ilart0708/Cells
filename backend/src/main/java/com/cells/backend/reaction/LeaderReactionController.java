package com.cells.backend.reaction;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** The leader reacts to a member's answer in their own room. The leader id comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/submissions/{submissionId}/reaction")
@PreAuthorize("hasRole('LEADER')")
public class LeaderReactionController {

    private final ReactionService reactionService;

    public LeaderReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PutMapping
    public ReactionSummary react(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                 @PathVariable Long submissionId, @Valid @RequestBody ReactionRequest request) {
        return reactionService.leaderReacts(Long.valueOf(jwt.getSubject()), roomId, submissionId, request.type());
    }

    @DeleteMapping
    public ReactionSummary remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                  @PathVariable Long submissionId) {
        return reactionService.leaderRemoves(Long.valueOf(jwt.getSubject()), roomId, submissionId);
    }
}
