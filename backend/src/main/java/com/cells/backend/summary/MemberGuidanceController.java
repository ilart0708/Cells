package com.cells.backend.summary;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Scoped to the logged-in member: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/member/rooms/{roomId}/devotions/{devotionId}/guidance")
@PreAuthorize("hasRole('MEMBER')")
public class MemberGuidanceController {

    private final MemberGuidanceService guidanceService;

    public MemberGuidanceController(MemberGuidanceService guidanceService) {
        this.guidanceService = guidanceService;
    }

    /** The leader's summary and reflection, once I have shared my own (or I'm exempt, or the day is over). */
    @GetMapping
    public MemberGuidanceResponse read(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                       @PathVariable Long devotionId) {
        return guidanceService.read(Long.valueOf(jwt.getSubject()), roomId, devotionId);
    }
}