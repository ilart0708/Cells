package com.cells.backend.history;

import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.room.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Read-only queries behind a member's history. The member id always comes from the token. */
public interface HistoryRepository extends Repository<Room, Long> {

    String ROOM_INFO = """
            select new com.cells.backend.history.HistoryRoomInfo(r.id, r.name, c.name, lu.fullName, r.status)
            from Room r
            join r.church c
            join User lu on lu.id = r.leaderId
            where r.id in (select rm.roomId from RoomMember rm where rm.memberId = :memberId)
            """;

    // Every room the member has ever been in: current, left and archived ones.
    @Query(ROOM_INFO + " order by lower(c.name), lower(r.name), r.id")
    List<HistoryRoomInfo> findRoomsEverIn(@Param("memberId") Long memberId);

    @Query(ROOM_INFO + " and r.id = :roomId")
    Optional<HistoryRoomInfo> findRoomEverIn(@Param("memberId") Long memberId, @Param("roomId") Long roomId);

    @Query("select new com.cells.backend.history.MembershipWindow(rm.joinedAt, rm.removedAt) "
            + "from RoomMember rm where rm.roomId = :roomId and rm.memberId = :memberId order by rm.joinedAt, rm.id")
    List<MembershipWindow> findWindows(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    // Only devotions members could see: active, today or earlier. Oldest first.
    @Query("select new com.cells.backend.history.DevotionDay("
            + "d.id, d.devotionDate, d.bibleBook, d.chapter, d.verseStart, d.verseEnd, d.title) "
            + "from Devotion d where d.roomId = :roomId and d.status = :status and d.devotionDate <= :today "
            + "order by d.devotionDate, d.id")
    List<DevotionDay> findDevotionDays(@Param("roomId") Long roomId,
                                       @Param("status") DevotionStatus status,
                                       @Param("today") LocalDate today);

    @Query("select new com.cells.backend.history.MyShare(s.devotionId, s.submittedAt, coalesce(e.points, 0)) "
            + "from DevotionSubmission s left join ExperienceRecord e on e.submissionId = s.id "
            + "where s.roomId = :roomId and s.memberId = :memberId")
    List<MyShare> findMyShares(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    @Query("select e.exemptionDate from DailyExemption e "
            + "where e.roomId = :roomId and e.memberId = :memberId and e.revokedAt is null")
    List<LocalDate> findMyExemptionDates(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    // What the members ahead in the order did, to tell "it was my turn" from "I was waiting".
    @Query("select new com.cells.backend.history.PredecessorShare(s.devotionId, s.memberId) "
            + "from DevotionSubmission s where s.roomId = :roomId and s.memberId in :memberIds")
    List<PredecessorShare> findPredecessorShares(@Param("roomId") Long roomId,
                                                 @Param("memberIds") Collection<Long> memberIds);

    @Query("select new com.cells.backend.history.PredecessorExemption(e.memberId, e.exemptionDate) "
            + "from DailyExemption e where e.roomId = :roomId and e.memberId in :memberIds and e.revokedAt is null")
    List<PredecessorExemption> findPredecessorExemptions(@Param("roomId") Long roomId,
                                                         @Param("memberIds") Collection<Long> memberIds);
}