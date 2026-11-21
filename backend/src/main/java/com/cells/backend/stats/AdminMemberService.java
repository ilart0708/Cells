package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.experience.ExperienceLevel;
import com.cells.backend.experience.LevelResponse;
import com.cells.backend.stats.AdminStatsRepository.MemberRoomRow;
import com.cells.backend.stats.AdminStatsRepository.MemberRow;
import com.cells.backend.stats.AdminStatsRepository.MemberTotals;
import com.cells.backend.stats.AdminStatsRepository.Paged;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Member monitoring for the administrator: who is where, today's status, devotions shared and total EXP.
 * No answer text, and no ordering by EXP or participation: the list is alphabetical.
 */
@Service
public class AdminMemberService {

    private final AdminStatsRepository admin;
    private final Clock clock;

    public AdminMemberService(AdminStatsRepository admin, Clock clock) {
        this.admin = admin;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminMembersResponse list(String query, int page, int size) {
        LocalDate today = LocalDate.now(clock);
        int pageSize = StatsPaging.pageSize(size);
        int pageIndex = StatsPaging.pageIndex(page);
        Paged<MemberRow> found = admin.searchMembers(
                StatsPaging.pattern(query), pageSize, StatsPaging.offset(pageIndex, pageSize));

        List<AdminMemberRow> rows = rows(found.items(), today);
        return new AdminMembersResponse(today, StatsPaging.page(rows, found.total(), pageIndex, pageSize));
    }

    @Transactional(readOnly = true)
    public AdminMemberDetailResponse detail(Long memberId) {
        MemberRow member = admin.findMember(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        LocalDate today = LocalDate.now(clock);
        return new AdminMemberDetailResponse(today, rows(List.of(member), today).get(0));
    }

    /** When and what a member shared, newest first. The text of the answers is never part of this. */
    @Transactional(readOnly = true)
    public PageResponse<AdminSubmissionRow> submissions(Long memberId, int page, int size) {
        admin.findMember(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        int pageSize = StatsPaging.pageSize(size);
        int pageIndex = StatsPaging.pageIndex(page);
        Paged<AdminSubmissionRow> found = admin.submissionsOf(
                memberId, pageSize, StatsPaging.offset(pageIndex, pageSize));
        return StatsPaging.page(found.items(), found.total(), pageIndex, pageSize);
    }

    private List<AdminMemberRow> rows(List<MemberRow> members, LocalDate today) {
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> ids = members.stream().map(MemberRow::memberId).toList();
        Map<Long, MemberTotals> totals = admin.memberTotals(ids).stream()
                .collect(Collectors.toMap(MemberTotals::memberId, total -> total));
        Map<Long, List<MemberRoomRow>> rooms = admin.memberRoomsToday(ids, today).stream()
                .collect(Collectors.groupingBy(MemberRoomRow::memberId));

        return members.stream().map(member -> {
            MemberTotals total = totals.get(member.memberId());
            int points = total == null ? 0 : total.points();
            int shared = total == null ? 0 : total.shared();
            List<MemberRoomToday> roomLines = rooms.getOrDefault(member.memberId(), List.of()).stream()
                    .map(room -> new MemberRoomToday(
                            room.roomId(), room.roomName(), room.churchName(), room.leaderName(),
                            room.number(), statusOf(room)))
                    .toList();
            return new AdminMemberRow(
                    member.memberId(), member.fullName(), member.email(), member.status(), member.churchName(),
                    member.memberSince(), roomLines.size(), shared, points,
                    LevelResponse.from(ExperienceLevel.forPoints(points)), roomLines);
        }).toList();
    }

    private static TodayStatus statusOf(MemberRoomRow room) {
        if (room.devotionId() == null) {
            return TodayStatus.NO_DEVOTION;
        }
        if (room.shared()) {
            return TodayStatus.SHARED;
        }
        return room.exempt() ? TodayStatus.EXEMPT : TodayStatus.STILL_TO_SHARE;
    }
}