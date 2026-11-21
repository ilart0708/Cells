package com.cells.backend.stats;

import java.util.List;

public record LeaderStatsDetailResponse(StatsPeriods periods, LeaderStatsRow leader, List<RoomStatsResponse> rooms) {
}