package com.cells.backend.reaction;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** A member reacts to a fellow member's shared answer. The member id comes from the token. */
@RestController
@RequestMapping("/api/v1/member/rooms/{roomId}/submissions/{submissionId}/reaction")
@PreAuthorize("hasRole('MEMBER')")
public class MemberReactionController {

    private final ReactionService reactionService;

    public MemberReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PutMapping
    public ReactionSummary react(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                 @PathVariable Long submissionId, @Valid @RequestBody ReactionRequest request) {
        return reactionService.memberReacts(Long.valueOf(jwt.getSubject()), roomId, submissionId, request.type());
    }

    @DeleteMapping
    public ReactionSummary remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                  @PathVariable Long submissionId) {
        return reactionService.memberRemoves(Long.valueOf(jwt.getSubject()), roomId, submissionId);
    }
}
