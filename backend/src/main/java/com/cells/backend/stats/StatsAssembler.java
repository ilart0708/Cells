package com.cells.backend.stats;

import com.cells.backend.stats.StatsRepository.LeaderRow;
import com.cells.backend.stats.StatsRepository.MemberCounts;
import com.cells.backend.stats.StatsRepository.RoomRow;
import com.cells.backend.stats.StatsRepository.TodayRow;
import com.cells.backend.stats.StatsRepository.WindowRow;
import com.cells.backend.user.UserStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Turns query rows into responses and adds them up. It takes a list of rooms and does not care how they
 * were chosen, so the administrator's statistics (Step 2) reuse it with a different set of rooms.
 */
@Component
public class StatsAssembler {

    private final StatsRepository repository;

    public StatsAssembler(StatsRepository repository) {
        this.repository = repository;
    }

    public List<RoomStatsResponse> roomStats(List<RoomRow> rooms, StatsPeriods periods) {
        if (rooms.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rooms.stream().map(RoomRow::roomId).toList();
        Map<Long, TodayRow> today = index(repository.todayRows(ids, periods.today()), TodayRow::roomId);
        Map<Long, WindowRow> week = index(
                repository.windowRows(ids, periods.last7From(), periods.through()), WindowRow::roomId);
        Map<Long, WindowRow> month = index(
                repository.windowRows(ids, periods.last30From(), periods.through()), WindowRow::roomId);

        return rooms.stream().map(room -> {
            TodayRow row = today.get(room.roomId());
            boolean hasDevotion = row != null && row.devotionId() != null;
            TodayStats todayStats = hasDevotion
                    ? TodayStats.of(1, row.shared(), row.exempt(), row.members() - row.shared() - row.exempt())
                    : TodayStats.none();

            return new RoomStatsResponse(
                    room.roomId(), room.roomName(), room.churchName(), room.leaderId(), room.leaderName(),
                    row == null ? 0 : row.members(), row == null ? 0 : row.disabled(),
                    todayStats,
                    period(room, week.get(room.roomId()), periods.last7From(), periods.through()),
                    period(room, month.get(room.roomId()), periods.last30From(), periods.through()));
        }).toList();
    }

    public LeaderStatsRow leaderRow(LeaderRow leader, List<RoomStatsResponse> rooms, MemberCounts people) {
        return new LeaderStatsRow(
                leader.leaderId(), leader.fullName(), leader.email(), leader.status(), leader.churchName(),
                rooms.size(), people.members(), people.disabled(),
                sumToday(rooms), sumPeriod(rooms, RoomStatsResponse::last7), sumPeriod(rooms, RoomStatsResponse::last30));
    }

    public DashboardTotalsResponse totals(List<LeaderRow> leaders, List<RoomStatsResponse> rooms, MemberCounts people) {
        int disabledLeaders = (int) leaders.stream().filter(leader -> leader.status() != UserStatus.ACTIVE).count();
        return new DashboardTotalsResponse(
                leaders.size(), disabledLeaders, rooms.size(), people.members(), people.disabled(),
                sumToday(rooms), sumPeriod(rooms, RoomStatsResponse::last7), sumPeriod(rooms, RoomStatsResponse::last30));
    }

    // ---------- helpers ----------

    private static PeriodStats period(RoomRow room, WindowRow window, LocalDate from, LocalDate through) {
        int possible = possibleDays(room.createdOn(), from, through);
        return window == null
                ? PeriodStats.none(possible)
                : PeriodStats.of(window.devotionDays(), possible, window.present() - window.exempt(), window.shared());
    }

    /** Completed days in the window on or after the day the room was created. */
    static int possibleDays(LocalDate createdOn, LocalDate from, LocalDate through) {
        LocalDate start = createdOn.isAfter(from) ? createdOn : from;
        return start.isAfter(through) ? 0 : (int) ChronoUnit.DAYS.between(start, through) + 1;
    }

    private static TodayStats sumToday(List<RoomStatsResponse> rooms) {
        return rooms.stream().map(RoomStatsResponse::today).reduce(TodayStats.none(), TodayStats::plus);
    }

    private static PeriodStats sumPeriod(List<RoomStatsResponse> rooms, Function<RoomStatsResponse, PeriodStats> pick) {
        return rooms.stream().map(pick).reduce(PeriodStats.none(0), PeriodStats::plus);
    }

    private static <T> Map<Long, T> index(List<T> rows, Function<T, Long> key) {
        Map<Long, T> map = new HashMap<>();
        rows.forEach(row -> map.put(key.apply(row), row));
        return map;
    }
}