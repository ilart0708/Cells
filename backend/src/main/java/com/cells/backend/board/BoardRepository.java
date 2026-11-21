package com.cells.backend.board;

import com.cells.backend.user.UserStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Read-only queries behind the room board. */
public interface BoardRepository extends Repository<MemberBoardSetting, Long> {

    // Current members with an active account who have not chosen to hide. One query, no N+1.
    @Query("""
            select new com.cells.backend.board.BoardMemberRow(u.id, u.fullName)
            from RoomMember rm
            join User u on u.id = rm.memberId
            where rm.roomId = :roomId and rm.removedAt is null and u.status = :status
              and not exists (
                  select 1 from MemberBoardSetting s where s.memberId = u.id and s.showOnBoard = false)
            order by lower(u.fullName), u.id
            """)
    List<BoardMemberRow> findShownMembers(@Param("roomId") Long roomId, @Param("status") UserStatus status);

    // EXP earned in this room within [from, to), and how many devotions it came from.
    @Query("""
            select new com.cells.backend.board.MonthPoints(e.memberId, sum(e.points), count(e))
            from ExperienceRecord e
            join DevotionSubmission s on s.id = e.submissionId
            where s.roomId = :roomId and e.memberId in :ids and e.awardedAt >= :from and e.awardedAt < :to
            group by e.memberId
            """)
    List<MonthPoints> findMonthPoints(@Param("roomId") Long roomId, @Param("ids") Collection<Long> ids,
                                      @Param("from") Instant from, @Param("to") Instant to);

    // All-time EXP across every room: a level describes a person's own journey.
    @Query("select new com.cells.backend.board.TotalPoints(e.memberId, sum(e.points)) "
            + "from ExperienceRecord e where e.memberId in :ids group by e.memberId")
    List<TotalPoints> findTotals(@Param("ids") Collection<Long> ids);
}