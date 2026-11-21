package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.stats.StatsRepository.LeaderRow;
import com.cells.backend.stats.StatsRepository.MemberCounts;
import com.cells.backend.stats.StatsRepository.RoomRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The pastor's statistics. The pastor id always comes from the token, and every room id used below comes
 * from the ownership query, so another pastor's leaders and rooms are never reachable.
 */
@Service
public class PastorStatisticsService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StatsRepository repository;
    private final StatsAssembler assembler;
    private final DevotionRepository devotionRepository;
    private final Clock clock;

    public PastorStatisticsService(StatsRepository repository, StatsAssembler assembler,
                                   DevotionRepository devotionRepository, Clock clock) {
        this.repository = repository;
        this.assembler = assembler;
        this.devotionRepository = devotionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PastorDashboardResponse dashboard(Long pastorId, int page, int size) {
        StatsPeriods periods = StatsPeriods.of(LocalDate.now(clock));

        List<LeaderRow> leaders = repository.findLeadersOfPastor(pastorId);
        List<RoomRow> rooms = repository.findRoomsOfPastor(pastorId);
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);

        List<Long> roomIds = rooms.stream().map(RoomRow::roomId).toList();
        Map<Long, MemberCounts> peopleByLeader = repository.memberCountsByLeader(roomIds);
        Map<Long, List<RoomStatsResponse>> roomsByLeader =
                roomStats.stream().collect(Collectors.groupingBy(RoomStatsResponse::leaderId));

        List<LeaderStatsRow> rows = leaders.stream()
                .map(leader -> assembler.leaderRow(
                        leader,
                        roomsByLeader.getOrDefault(leader.leaderId(), List.of()),
                        peopleByLeader.getOrDefault(leader.leaderId(), MemberCounts.NONE)))
                .toList();

        DashboardTotalsResponse totals = assembler.totals(leaders, roomStats, repository.memberCounts(roomIds));

        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 0);
        int total = rows.size();
        int from = (int) Math.min((long) pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);
        PageResponse<LeaderStatsRow> leaderPage = new PageResponse<>(
                List.copyOf(rows.subList(from, to)), pageIndex, pageSize, total, (total + pageSize - 1) / pageSize);

        return new PastorDashboardResponse(periods, totals, leaderPage);
    }

    @Transactional(readOnly = true)
    public LeaderStatsDetailResponse leader(Long pastorId, Long leaderId) {
        // Another pastor's leader looks exactly like a leader that does not exist.
        LeaderRow leader = repository.findLeaderOfPastor(pastorId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Leader not found."));
        StatsPeriods periods = StatsPeriods.of(LocalDate.now(clock));

        List<RoomRow> rooms = repository.findRoomsOfPastor(pastorId).stream()
                .filter(room -> room.leaderId() == leaderId.longValue())
                .toList();
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);

        MemberCounts people = repository
                .memberCountsByLeader(rooms.stream().map(RoomRow::roomId).toList())
                .getOrDefault(leader.leaderId(), MemberCounts.NONE);

        return new LeaderStatsDetailResponse(periods, assembler.leaderRow(leader, roomStats, people), roomStats);
    }

    @Transactional(readOnly = true)
    public RoomStatsDetailResponse room(Long pastorId, Long roomId) {
        // Another pastor's room (or an archived one) looks exactly like a room that does not exist.
        RoomRow room = repository.findRoomOfPastor(pastorId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        StatsPeriods periods = StatsPeriods.of(LocalDate.now(clock));

        RoomStatsResponse stats = assembler.roomStats(List.of(room), periods).get(0);

        TodayDevotionInfo devotion = devotionRepository
                .findByRoomIdAndDevotionDateAndStatus(roomId, periods.today(), DevotionStatus.ACTIVE)
                .map(d -> new TodayDevotionInfo(
                        d.getBibleBook().reference(d.getChapter(), d.getVerseStart(), d.getVerseEnd()),
                        d.getTitle(), d.getLearningLevel()))
                .orElse(null);

        List<DayStatsResponse> days = repository.dayRows(roomId, periods.last30From(), periods.through()).stream()
                .map(day -> {
                    int expected = day.present() - day.exempt();
                    return new DayStatsResponse(
                            day.date(), day.reference(), day.title(),
                            expected, day.shared(), StatsMath.rate(day.shared(), expected));
                })
                .toList();

        return new RoomStatsDetailResponse(periods, stats, devotion, days);
    }
}