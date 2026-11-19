package com.cells.backend.participation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reading the room's answers. It shares its URL with "submit an answer" (POST, in
 * MemberParticipationController): the two differ by HTTP method.
 */
@RestController
@RequestMapping("/api/v1/member/rooms/{roomId}/devotions/{devotionId}/submissions")
@PreAuthorize("hasRole('MEMBER')")
public class MemberSubmissionController {

    private final SubmissionReadService submissionReadService;

    public MemberSubmissionController(SubmissionReadService submissionReadService) {
        this.submissionReadService = submissionReadService;
    }

    @GetMapping
    public MemberSubmissionsResponse read(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                          @PathVariable Long devotionId) {
        return submissionReadService.forMember(Long.valueOf(jwt.getSubject()), roomId, devotionId);
    }
}