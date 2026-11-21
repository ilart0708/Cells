package com.cells.backend.stats;

import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.stats.AdminStatsRepository.ChurchRow;
import com.cells.backend.stats.AdminStatsRepository.Paged;
import com.cells.backend.stats.AdminStatsRepository.PastorRow;
import com.cells.backend.stats.AdminStatsRepository.PlatformCounts;
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
 * The administrator's statistics. Counts only: no answer, note or instruction text is read anywhere here.
 * Lists are alphabetical on purpose: nothing ranks one church, leader or room against another.
 */
@Service
public class AdminStatisticsService {

    private final AdminStatsRepository admin;
    private final StatsRepository stats;
    private final StatsAssembler assembler;
    private final DevotionRepository devotionRepository;
    private final Clock clock;

    public AdminStatisticsService(AdminStatsRepository admin, StatsRepository stats, StatsAssembler assembler,
                                  DevotionRepository devotionRepository, Clock clock) {
        this.admin = admin;
        this.stats = stats;
        this.assembler = assembler;
        this.devotionRepository = devotionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        StatsPeriods periods = periods();
        PlatformCounts counts = admin.platformCounts();

        List<RoomRow> rooms = admin.allActiveRooms();
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);
        MemberCounts people = stats.memberCounts(roomIds(rooms));

        DashboardTotalsResponse totals = new DashboardTotalsResponse(
                counts.leaders(), counts.disabledLeaders(), rooms.size(), people.members(), people.disabled(),
                StatsSums.today(roomStats),
                StatsSums.period(roomStats, RoomStatsResponse::last7),
                StatsSums.period(roomStats, RoomStatsResponse::last30));

        return new AdminDashboardResponse(periods, counts.churches(), counts.pastors(), counts.disabledPastors(), totals);
    }

    @Transactional(readOnly = true)
    public StatsPageResponse<ChurchStatsRow> churches(String query, int page, int size) {
        StatsPeriods periods = periods();
        int pageSize = StatsPaging.pageSize(size);
        int pageIndex = StatsPaging.pageIndex(page);
        Paged<ChurchRow> found = admin.searchChurches(
                StatsPaging.pattern(query), pageSize, StatsPaging.offset(pageIndex, pageSize));

        List<RoomRow> rooms = admin.roomsOfChurches(found.items().stream().map(ChurchRow::churchId).toList());
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);

        Map<Long, Long> churchOfRoom = rooms.stream().collect(Collectors.toMap(RoomRow::roomId, RoomRow::churchId));
        Map<Long, List<RoomStatsResponse>> byChurch = roomStats.stream()
                .collect(Collectors.groupingBy(room -> churchOfRoom.get(room.roomId())));
        Map<Long, MemberCounts> people = admin.memberCountsByChurch(roomIds(rooms));

        List<ChurchStatsRow> rows = found.items().stream().map(church -> {
            List<RoomStatsResponse> mine = byChurch.getOrDefault(church.churchId(), List.of());
            MemberCounts count = people.getOrDefault(church.churchId(), MemberCounts.NONE);
            return new ChurchStatsRow(
                    church.churchId(), church.name(), church.status(), church.pastors(), church.leaders(),
                    mine.size(), count.members(), count.disabled(),
                    StatsSums.today(mine),
                    StatsSums.period(mine, RoomStatsResponse::last7),
                    StatsSums.period(mine, RoomStatsResponse::last30));
        }).toList();

        return new StatsPageResponse<>(periods, StatsPaging.page(rows, found.total(), pageIndex, pageSize));
    }

    @Transactional(readOnly = true)
    public StatsPageResponse<PastorStatsRow> pastors(String query, int page, int size) {
        StatsPeriods periods = periods();
        int pageSize = StatsPaging.pageSize(size);
        int pageIndex = StatsPaging.pageIndex(page);
        Paged<PastorRow> found = admin.searchPastors(
                StatsPaging.pattern(query), pageSize, StatsPaging.offset(pageIndex, pageSize));

        Map<Long, Long> pastorOfLeader = admin.pastorIdByLeader(found.items().stream().map(PastorRow::pastorId).toList());
        List<RoomRow> rooms = admin.roomsOfLeaders(pastorOfLeader.keySet());
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);

        Map<Long, List<RoomStatsResponse>> byPastor = roomStats.stream()
                .collect(Collectors.groupingBy(room -> pastorOfLeader.get(room.leaderId())));
        Map<Long, MemberCounts> people = admin.memberCountsByPastor(roomIds(rooms));

        List<PastorStatsRow> rows = found.items().stream().map(pastor -> {
            List<RoomStatsResponse> mine = byPastor.getOrDefault(pastor.pastorId(), List.of());
            MemberCounts count = people.getOrDefault(pastor.pastorId(), MemberCounts.NONE);
            return new PastorStatsRow(
                    pastor.pastorId(), pastor.fullName(), pastor.email(), pastor.status(), pastor.churchName(),
                    pastor.leaders(), mine.size(), count.members(), count.disabled(),
                    StatsSums.today(mine),
                    StatsSums.period(mine, RoomStatsResponse::last7),
                    StatsSums.period(mine, RoomStatsResponse::last30));
        }).toList();

        return new StatsPageResponse<>(periods, StatsPaging.page(rows, found.total(), pageIndex, pageSize));
    }

    @Transactional(readOnly = true)
    public StatsPageResponse<LeaderStatsRow> leaders(String query, int page, int size) {
        StatsPeriods periods = periods();
        int pageSize = StatsPaging.pageSize(size);
        int pageIndex = StatsPaging.pageIndex(page);
        Paged<LeaderRow> found = admin.searchLeaders(
                StatsPaging.pattern(query), pageSize, StatsPaging.offset(pageIndex, pageSize));

        List<RoomRow> rooms = admin.roomsOfLeaders(found.items().stream().map(LeaderRow::leaderId).toList());
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);
        Map<Long, List<RoomStatsResponse>> byLeader =
                roomStats.stream().collect(Collectors.groupingBy(RoomStatsResponse::leaderId));
        Map<Long, MemberCounts> people = stats.memberCountsByLeader(roomIds(rooms));

        List<LeaderStatsRow> rows = found.items().stream()
                .map(leader -> assembler.leaderRow(
                        leader,
                        byLeader.getOrDefault(leader.leaderId(), List.of()),
                        people.getOrDefault(leader.leaderId(), MemberCounts.NONE)))
                .toList();

        return new StatsPageResponse<>(periods, StatsPaging.page(rows, found.total(), pageIndex, pageSize));
    }

    @Transactional(readOnly = true)
    public LeaderStatsDetailResponse leader(Long leaderId) {
        LeaderRow leader = admin.findLeader(leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Leader not found."));
        StatsPeriods periods = periods();

        List<RoomRow> rooms = admin.roomsOfLeaders(List.of(leaderId));
        List<RoomStatsResponse> roomStats = assembler.roomStats(rooms, periods);
        MemberCounts people = stats.memberCountsByLeader(roomIds(rooms)).getOrDefault(leaderId, MemberCounts.NONE);

        return new LeaderStatsDetailResponse(periods, assembler.leaderRow(leader, roomStats, people), roomStats);
    }

    @Transactional(readOnly = true)
    public StatsPageResponse<RoomStatsResponse> rooms(String query, int page, int size) {
        StatsPeriods periods = periods();
        int pageSize = StatsPaging.pageSize(size);
        int pageIndex = StatsPaging.pageIndex(page);
        Paged<RoomRow> found = admin.searchRooms(
                StatsPaging.pattern(query), pageSize, StatsPaging.offset(pageIndex, pageSize));

        List<RoomStatsResponse> rows = assembler.roomStats(found.items(), periods);
        return new StatsPageResponse<>(periods, StatsPaging.page(rows, found.total(), pageIndex, pageSize));
    }

    @Transactional(readOnly = true)
    public RoomStatsDetailResponse room(Long roomId) {
        // An archived room looks exactly like a room that does not exist.
        RoomRow room = admin.findActiveRoom(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        StatsPeriods periods = periods();

        RoomStatsResponse roomStats = assembler.roomStats(List.of(room), periods).get(0);

        TodayDevotionInfo devotion = devotionRepository
                .findByRoomIdAndDevotionDateAndStatus(roomId, periods.today(), DevotionStatus.ACTIVE)
                .map(d -> new TodayDevotionInfo(
                        d.getBibleBook().reference(d.getChapter(), d.getVerseStart(), d.getVerseEnd()),
                        d.getTitle(), d.getLearningLevel()))
                .orElse(null);

        List<DayStatsResponse> days = stats.dayRows(roomId, periods.last30From(), periods.through()).stream()
                .map(day -> {
                    int expected = day.present() - day.exempt();
                    return new DayStatsResponse(
                            day.date(), day.reference(), day.title(),
                            expected, day.shared(), StatsMath.rate(day.shared(), expected));
                })
                .toList();

        return new RoomStatsDetailResponse(periods, roomStats, devotion, days);
    }

    private StatsPeriods periods() {
        return StatsPeriods.of(LocalDate.now(clock));
    }

    private static List<Long> roomIds(List<RoomRow> rooms) {
        return rooms.stream().map(RoomRow::roomId).toList();
    }
}