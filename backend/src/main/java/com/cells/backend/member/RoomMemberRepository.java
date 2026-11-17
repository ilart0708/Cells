package com.cells.backend.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    boolean existsByRoomIdAndMemberIdAndRemovedAtIsNull(Long roomId, Long memberId);

    Optional<RoomMember> findByRoomIdAndMemberIdAndRemovedAtIsNull(Long roomId, Long memberId);

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
}