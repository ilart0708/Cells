package com.cells.backend.stats;

import java.util.List;

/** todayDevotion is null when nothing is posted today. days holds the last 30 completed days that had a devotion, newest first. */
public record RoomStatsDetailResponse(
        StatsPeriods periods, RoomStatsResponse room, TodayDevotionInfo todayDevotion, List<DayStatsResponse> days) {
}