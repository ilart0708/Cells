package com.cells.backend.summary;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/devotions/{devotionId}/guidance")
@PreAuthorize("hasRole('LEADER')")
public class LeaderGuidanceController {

    private final LeaderGuidanceService guidanceService;

    public LeaderGuidanceController(LeaderGuidanceService guidanceService) {
        this.guidanceService = guidanceService;
    }

    /** The summary and the reflection (each null until written), and whether writing is allowed. */
    @GetMapping
    public LeaderGuidanceResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                      @PathVariable Long devotionId) {
        return guidanceService.get(leaderId(jwt), roomId, devotionId);
    }

    /** Option A: the group's insights. Creates it the first time, and replaces the text after that. */
    @PutMapping("/summary")
    public SummaryNoteResponse saveSummary(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                           @PathVariable Long devotionId, @Valid @RequestBody GuidanceRequest request) {
        return guidanceService.save(leaderId(jwt), roomId, devotionId, SummaryType.SUMMARY, request.content());
    }

    /** Option B: the leader's own understanding of the passage. */
    @PutMapping("/reflection")
    public SummaryNoteResponse saveReflection(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                              @PathVariable Long devotionId, @Valid @RequestBody GuidanceRequest request) {
        return guidanceService.save(leaderId(jwt), roomId, devotionId, SummaryType.REFLECTION, request.content());
    }

    private static Long leaderId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}