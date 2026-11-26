package com.cells.backend.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    boolean existsByRoomIdAndMemberIdAndRemovedAtIsNull(Long roomId, Long memberId);

    Optional<RoomMember> findByRoomIdAndMemberIdAndRemovedAtIsNull(Long roomId, Long memberId);

    List<RoomMember> findByRoomIdAndRemovedAtIsNullOrderByParticipationNumber(Long roomId);

    List<RoomMember> findByMemberIdAndRemovedAtIsNull(Long memberId);

    // People in the active rooms of one church (used for the limit of an unverified church).
    @Query("select count(distinct rm.memberId) from RoomMember rm join Room r on r.id = rm.roomId "
            + "where r.church.id = :churchId and r.status = com.cells.backend.room.RoomStatus.ACTIVE "
            + "and rm.removedAt is null")
    long countActiveMembersOfChurch(@Param("churchId") Long churchId);

    @Query("select coalesce(max(rm.participationNumber), 0) from RoomMember rm "
            + "where rm.roomId = :roomId and rm.removedAt is null")
    int maxActiveNumber(@Param("roomId") Long roomId);

    // One query returns the whole roster (membership + user), so there is no N+1 problem.
    @Query("""
            select new com.cells.backend.room.RoomMemberResponse(
                u.id, u.fullName, u.email, u.status, rm.participationNumber, rm.joinedAt)
            from RoomMember rm
            join User u on u.id = rm.memberId
            where rm.roomId = :roomId and rm.removedAt is null
            order by rm.participationNumber
            """)
    List<RoomMemberResponse> roster(@Param("roomId") Long roomId);

    // ---------- renumbering (see "Why renumbering needs two steps") ----------

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RoomMember rm set rm.participationNumber = rm.participationNumber + :offset "
            + "where rm.roomId = :roomId and rm.removedAt is null")
    int shiftActiveNumbers(@Param("roomId") Long roomId, @Param("offset") int offset);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RoomMember rm set rm.participationNumber = :number where rm.id = :id")
    int setNumber(@Param("id") Long id, @Param("number") int number);

    // ---------- the member's own view ----------

    String MEMBER_ROOM = """
            select new com.cells.backend.room.MemberRoomResponse(
                r.id, r.name, c.id, c.name, lu.fullName, rm.participationNumber,
                (select count(m) from RoomMember m where m.roomId = r.id and m.removedAt is null),
                rm.joinedAt, c.pastorName, c.verified)
            from RoomMember rm
            join Room r on r.id = rm.roomId
            join r.church c
            join User lu on lu.id = r.leaderId
            where rm.memberId = :memberId and rm.removedAt is null and r.status = :status
            """;

    @Query(MEMBER_ROOM + " order by lower(c.name), lower(r.name), r.id")
    List<MemberRoomResponse> findRoomsOf(@Param("memberId") Long memberId,
                                         @Param("status") RoomStatus status);

    @Query(MEMBER_ROOM + " and r.id = :roomId")
    Optional<MemberRoomResponse> findRoomOf(@Param("memberId") Long memberId,
                                            @Param("roomId") Long roomId,
                                            @Param("status") RoomStatus status);
}