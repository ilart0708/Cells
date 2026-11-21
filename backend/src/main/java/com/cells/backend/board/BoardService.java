package com.cells.backend.board;

import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.experience.ExperienceLevel;
import com.cells.backend.experience.LevelResponse;
import com.cells.backend.history.HistoryService;
import com.cells.backend.member.MemberRepository;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import com.cells.backend.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The room board and the opt-out. Member and leader ids always come from the token. */
@Service
public class BoardService {

    static final String MESSAGE =
            "Growing Together is not a competition. It is here to encourage each of us to keep doing what is "
                    + "from the Lord, faithfully and continually, and to cheer one another on.";

    private final BoardRepository boardRepository;
    private final MemberBoardSettingRepository settingRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberRepository memberRepository;
    private final HistoryService historyService;
    private final Clock clock;

    public BoardService(BoardRepository boardRepository,
                        MemberBoardSettingRepository settingRepository,
                        RoomRepository roomRepository,
                        RoomMemberRepository roomMemberRepository,
                        MemberRepository memberRepository,
                        HistoryService historyService,
                        Clock clock) {
        this.boardRepository = boardRepository;
        this.settingRepository = settingRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.memberRepository = memberRepository;
        this.historyService = historyService;
        this.clock = clock;
    }

    // ---------- the member's choice ----------

    @Transactional(readOnly = true)
    public BoardSettingResponse setting(Long memberId) {
        return new BoardSettingResponse(isShown(memberId));
    }

    /** Creates the choice the first time and replaces it after that, so repeating a request is harmless. */
    @Transactional
    public BoardSettingResponse saveSetting(Long memberId, boolean show) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found.");
        }
        MemberBoardSetting setting = settingRepository.findById(memberId).orElse(null);
        if (setting == null) {
            setting = settingRepository.save(new MemberBoardSetting(memberId, show));
        } else {
            setting.change(show, Instant.now());
        }
        return new BoardSettingResponse(setting.isShowOnBoard());
    }

    // ---------- the board ----------

    @Transactional(readOnly = true)
    public GrowthBoardResponse forMember(Long memberId, Long roomId) {
        // A room the member is not in (or no longer in, or that is archived) looks like it does not exist.
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw roomNotFound();
        }
        return build(roomId, memberId, isShown(memberId));
    }

    @Transactional(readOnly = true)
    public GrowthBoardResponse forLeader(Long leaderId, Long roomId) {
        // Another leader's room looks exactly like a room that does not exist.
        roomRepository.findByIdAndLeaderId(roomId, leaderId).orElseThrow(BoardService::roomNotFound);
        return build(roomId, null, null);
    }

    private record Scored(BoardMemberRow member, long points, long shared, long total, int streak) {
    }

    private GrowthBoardResponse build(Long roomId, Long viewerId, Boolean iAmShown) {
        LocalDate today = LocalDate.now(clock);
        ZoneId zone = clock.getZone();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonth = monthStart.plusMonths(1);
        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = nextMonth.atStartOfDay(zone).toInstant();
        int daysLeft = (int) ChronoUnit.DAYS.between(today, nextMonth);

        List<BoardMemberRow> shown = boardRepository.findShownMembers(roomId, UserStatus.ACTIVE);
        if (shown.isEmpty()) {
            return new GrowthBoardResponse(today, monthStart, daysLeft, MESSAGE, iAmShown, 0, List.of());
        }

        List<Long> ids = shown.stream().map(BoardMemberRow::memberId).toList();
        Map<Long, MonthPoints> month = new HashMap<>();
        boardRepository.findMonthPoints(roomId, ids, from, to).forEach(p -> month.put(p.memberId(), p));
        Map<Long, Long> totals = new HashMap<>();
        boardRepository.findTotals(ids).forEach(t -> totals.put(t.memberId(), t.points()));

        List<Scored> scored = new ArrayList<>(shown.size());
        for (BoardMemberRow row : shown) {
            MonthPoints points = month.get(row.memberId());
            // The same rules as the member's history, so the two can never disagree. A room has a handful
            // of members, so one lookup each is fine.
            int streak = historyService.roomHistory(row.memberId(), roomId, 0, 1).summary().currentStreak();
            scored.add(new Scored(
                    row, points == null ? 0 : points.points(), points == null ? 0 : points.shared(),
                    totals.getOrDefault(row.memberId(), 0L), streak));
        }

        // Most EXP first. Equal EXP is ordered by name, so it never depends on luck.
        scored.sort(Comparator.<Scored>comparingLong(Scored::points).reversed()
                .thenComparing(s -> s.member().fullName().toLowerCase()));

        List<GrowthEntryResponse> entries = new ArrayList<>(scored.size());
        Integer position = null;
        long previousPoints = -1;
        int index = 0;
        for (Scored s : scored) {
            index++;
            if (s.points() == 0) {
                position = null; // nobody is ranked at zero
            } else if (s.points() != previousPoints) {
                position = index; // equal EXP keeps the same position, and the next one skips (1, 1, 3)
                previousPoints = s.points();
            }
            entries.add(new GrowthEntryResponse(
                    position, s.member().fullName(),
                    viewerId != null && viewerId.equals(s.member().memberId()),
                    LevelResponse.from(ExperienceLevel.forPoints(s.total())),
                    (int) s.points(), (int) s.shared(), s.streak()));
        }

        return new GrowthBoardResponse(today, monthStart, daysLeft, MESSAGE, iAmShown, entries.size(), entries);
    }

    // No row means "shown".
    private boolean isShown(Long memberId) {
        return settingRepository.findById(memberId).map(MemberBoardSetting::isShowOnBoard).orElse(true);
    }

    private static ResourceNotFoundException roomNotFound() {
        return new ResourceNotFoundException("Room not found.");
    }
}