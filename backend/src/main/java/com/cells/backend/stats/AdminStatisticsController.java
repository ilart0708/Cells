package com.cells.backend.stats;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrator only. Counts only: nothing here reads the text of an answer. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    public AdminStatisticsController(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return statisticsService.dashboard();
    }

    @GetMapping("/stats/churches")
    public StatsPageResponse<ChurchStatsRow> churches(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return statisticsService.churches(q, page, size);
    }

    @GetMapping("/stats/pastors")
    public StatsPageResponse<PastorStatsRow> pastors(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return statisticsService.pastors(q, page, size);
    }

    @GetMapping("/stats/leaders")
    public StatsPageResponse<LeaderStatsRow> leaders(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return statisticsService.leaders(q, page, size);
    }

    @GetMapping("/stats/leaders/{leaderId}")
    public LeaderStatsDetailResponse leader(@PathVariable Long leaderId) {
        return statisticsService.leader(leaderId);
    }

    @GetMapping("/stats/rooms")
    public StatsPageResponse<RoomStatsResponse> rooms(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return statisticsService.rooms(q, page, size);
    }

    @GetMapping("/stats/rooms/{roomId}")
    public RoomStatsDetailResponse room(@PathVariable Long roomId) {
        return statisticsService.room(roomId);
    }
}