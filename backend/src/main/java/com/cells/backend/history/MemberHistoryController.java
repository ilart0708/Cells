package com.cells.backend.history;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Scoped to the logged-in member: the id always comes from the token. Nobody else's history is reachable. */
@RestController
@RequestMapping("/api/v1/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberHistoryController {

    private final HistoryService historyService;

    public MemberHistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    /** My totals this week, this month and all time, and my streak in each room. */
    @GetMapping("/participation/summary")
    public HistorySummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return historyService.summary(memberId(jwt));
    }

    /** One line per devotion in this room, newest first. */
    @GetMapping("/rooms/{roomId}/history")
    public RoomHistoryResponse roomHistory(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.roomHistory(memberId(jwt), roomId, page, size);
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}