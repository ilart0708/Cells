package com.cells.backend.stats;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Scoped to the logged-in pastor: the id always comes from the token. Counts only: no member is named. */
@RestController
@RequestMapping("/api/v1/pastor")
@PreAuthorize("hasRole('PASTOR')")
public class PastorStatisticsController {

    private final PastorStatisticsService statisticsService;

    public PastorStatisticsController(PastorStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /** Totals for all my leaders, and one line per leader (paged). */
    @GetMapping("/dashboard")
    public PastorDashboardResponse dashboard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return statisticsService.dashboard(pastorId(jwt), page, size);
    }

    @GetMapping("/leaders/{leaderId}/stats")
    public LeaderStatsDetailResponse leader(@AuthenticationPrincipal Jwt jwt, @PathVariable Long leaderId) {
        return statisticsService.leader(pastorId(jwt), leaderId);
    }

    @GetMapping("/rooms/{roomId}/stats")
    public RoomStatsDetailResponse room(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return statisticsService.room(pastorId(jwt), roomId);
    }

    private static Long pastorId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}