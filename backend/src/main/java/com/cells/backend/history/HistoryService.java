package com.cells.backend.history;

import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomMemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The member's own history. The member id always comes from the token. */
@Service
public class HistoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final HistoryRepository historyRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Clock clock;

    public HistoryService(HistoryRepository historyRepository, RoomMemberRepository roomMemberRepository, Clock clock) {
        this.historyRepository = historyRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HistorySummaryResponse summary(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);

        List<RoomHistorySummaryResponse> rooms = new ArrayList<>();
        PeriodTotalsResponse week = PeriodTotalsResponse.empty();
        PeriodTotalsResponse month = PeriodTotalsResponse.empty();
        PeriodTotalsResponse all = PeriodTotalsResponse.empty();

        for (HistoryRoomInfo room : historyRepository.findRoomsEverIn(memberId)) {
            RoomHistorySummaryResponse summary = compute(memberId, room, today, weekStart, monthStart).summary();
            rooms.add(summary);
            week = week.plus(summary.thisWeek());
            month = month.plus(summary.thisMonth());
            all = all.plus(summary.allTime());
        }
        return new HistorySummaryResponse(today, weekStart, monthStart, week, month, all, rooms);
    }

    @Transactional(readOnly = true)
    public RoomHistoryResponse roomHistory(Long memberId, Long roomId, int page, int size) {
        // A room the member was never in looks exactly like a room that does not exist.
        HistoryRoomInfo room = historyRepository.findRoomEverIn(memberId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        LocalDate today = LocalDate.now(clock);
        Computed computed = compute(memberId, room, today,
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today.withDayOfMonth(1));

        List<HistoryEntryResponse> newestFirst = new ArrayList<>(computed.entries().stream()
                .map(entry -> new HistoryEntryResponse(
                        entry.day().id(), entry.day().date(), entry.day().reference(), entry.day().title(),
                        entry.status(), entry.points(), entry.submittedAt()))
                .toList());
        Collections.reverse(newestFirst);

        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 0);
        int total = newestFirst.size();
        int from = (int) Math.min((long) pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);
        int totalPages = (total + pageSize - 1) / pageSize;

        return new RoomHistoryResponse(
                computed.summary(),
                new PageResponse<>(newestFirst.subList(from, to), pageIndex, pageSize, total, totalPages));
    }

    private record Computed(List<HistoryCalculator.Entry> entries, RoomHistorySummaryResponse summary) {
    }

    private Computed compute(Long memberId, HistoryRoomInfo room, LocalDate today,
                             LocalDate weekStart, LocalDate monthStart) {
        ZoneId zone = clock.getZone();
        Long roomId = room.roomId();

        List<DevotionDay> days = historyRepository.findDevotionDays(roomId, DevotionStatus.ACTIVE, today);

        // The first day in a room is a welcome day: it cannot be missed. A day they answered still shows.
        List<HistoryCalculator.Window> windows = historyRepository.findWindows(roomId, memberId).stream()
                .map(w -> new HistoryCalculator.Window(
                        w.joinedAt().atZone(zone).toLocalDate().plusDays(1),
                        w.removedAt() == null ? null : w.removedAt().atZone(zone).toLocalDate()))
                .toList();

        Map<Long, MyShare> myShares = new HashMap<>();
        for (MyShare share : historyRepository.findMyShares(roomId, memberId)) {
            myShares.put(share.devotionId(), share);
        }
        Set<LocalDate> myExempt = new HashSet<>(historyRepository.findMyExemptionDates(roomId, memberId));

        // The order is read as it is now. Someone who has left the room has no place in it.
        List<RoomMemberResponse> roster = roomMemberRepository.roster(roomId);
        RoomMemberResponse mine = roster.stream()
                .filter(m -> m.memberId().equals(memberId))
                .findFirst().orElse(null);

        List<HistoryCalculator.Predecessor> predecessors = List.of();
        Map<Long, Set<Long>> predecessorShares = new HashMap<>();
        Map<Long, Set<LocalDate>> predecessorExempt = new HashMap<>();

        if (mine != null) {
            predecessors = roster.stream()
                    .filter(m -> m.participationNumber() < mine.participationNumber())
                    .sorted(Comparator.comparing(RoomMemberResponse::participationNumber).reversed())
                    .map(m -> new HistoryCalculator.Predecessor(m.memberId(), m.joinedAt().atZone(zone).toLocalDate()))
                    .toList();

            if (!predecessors.isEmpty()) {
                List<Long> ids = predecessors.stream().map(HistoryCalculator.Predecessor::memberId).toList();
                for (PredecessorShare share : historyRepository.findPredecessorShares(roomId, ids)) {
                    predecessorShares.computeIfAbsent(share.devotionId(), key -> new HashSet<>()).add(share.memberId());
                }
                for (PredecessorExemption exemption : historyRepository.findPredecessorExemptions(roomId, ids)) {
                    predecessorExempt.computeIfAbsent(exemption.memberId(), key -> new HashSet<>()).add(exemption.date());
                }
            }
        }

        HistoryCalculator.Result result = HistoryCalculator.calculate(
                today, days, windows, myShares, myExempt, mine != null,
                predecessors, predecessorShares, predecessorExempt);

        List<HistoryCalculator.Entry> entries = result.entries();
        RoomHistorySummaryResponse summary = new RoomHistorySummaryResponse(
                roomId, room.roomName(), room.churchName(), room.leaderName(),
                room.roomStatus(), mine != null,
                result.currentStreak(), result.bestStreak(),
                HistoryCalculator.totals(entries, weekStart, today),
                HistoryCalculator.totals(entries, monthStart, today),
                HistoryCalculator.totals(entries, LocalDate.MIN, LocalDate.MAX));

        return new Computed(entries, summary);
    }
}