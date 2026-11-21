package com.cells.backend.experience;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Scoped to the logged-in member: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/member/experience")
@PreAuthorize("hasRole('MEMBER')")
public class MemberExperienceController {

    private final ExperienceService experienceService;

    public MemberExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    /** My total EXP, level, progress to the next level, and EXP per room. */
    @GetMapping
    public ExperienceSummaryResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return experienceService.summary(Long.valueOf(jwt.getSubject()));
    }
}