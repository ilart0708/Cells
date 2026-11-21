package com.cells.backend.stats;

import java.util.List;
import java.util.function.Function;

/** Adds up the rooms of a church, a pastor, a leader or the whole platform. */
final class StatsSums {

    private StatsSums() {
    }

    static TodayStats today(List<RoomStatsResponse> rooms) {
        return rooms.stream().map(RoomStatsResponse::today).reduce(TodayStats.none(), TodayStats::plus);
    }

    static PeriodStats period(List<RoomStatsResponse> rooms, Function<RoomStatsResponse, PeriodStats> pick) {
        return rooms.stream().map(pick).reduce(PeriodStats.none(0), PeriodStats::plus);
    }
}