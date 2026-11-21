package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;

/** totals cover all of the pastor's leaders. Only the leader lines are paged. */
public record PastorDashboardResponse(
        StatsPeriods periods, DashboardTotalsResponse totals, PageResponse<LeaderStatsRow> leaders) {
}