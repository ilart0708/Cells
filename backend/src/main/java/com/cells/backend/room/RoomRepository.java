package com.cells.backend.room;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    String SELECT_DTO = """
            select new com.cells.backend.room.RoomResponse(
                r.id, r.name, r.status, c.id, c.name, u.id, u.fullName,
                (select count(m) from RoomMember m where m.roomId = r.id and m.removedAt is null),
                r.createdAt, r.archivedAt, c.pastorName, c.verified)
            """;

    String FROM_JOINS = """
            from Room r
            join r.church c
            join User u on u.id = r.leaderId
            """;

    String OWN_MATCHING = " where r.leaderId = :leaderId and r.status in :statuses and lower(r.name) like :pattern ";

    // One query returns everything a list needs, so there is no N+1 problem.
    // The sort is part of the query, so pass an unsorted Pageable.
    @Query(value = SELECT_DTO + FROM_JOINS + OWN_MATCHING + "order by lower(r.name), r.id",
            countQuery = "select count(r) " + FROM_JOINS + OWN_MATCHING)
    Page<RoomResponse> searchOwn(@Param("leaderId") Long leaderId,
                                 @Param("statuses") Collection<RoomStatus> statuses,
                                 @Param("pattern") String pattern,
                                 Pageable pageable);

    @Query(SELECT_DTO + FROM_JOINS + " where r.id = :id and r.leaderId = :leaderId")
    Optional<RoomResponse> findOwnResponse(@Param("id") Long id, @Param("leaderId") Long leaderId);

    Optional<Room> findByIdAndLeaderId(Long id, Long leaderId);

    // Row lock: anything that changes a room's roster takes this first, so changes to one room take turns.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id and r.leaderId = :leaderId")
    Optional<Room> findOwnForUpdate(@Param("id") Long id, @Param("leaderId") Long leaderId);

    long countByLeaderIdAndStatus(Long leaderId, RoomStatus status);

    Optional<Room> findByLeaderIdAndStatusAndNameIgnoreCase(Long leaderId, RoomStatus status, String name);

    boolean existsByLeaderIdAndStatusAndNameIgnoreCase(Long leaderId, RoomStatus status, String name);

    boolean existsByLeaderIdAndStatusAndNameIgnoreCaseAndIdNot(
            Long leaderId, RoomStatus status, String name, Long id);

    // Row lock for member actions (submitting). It is the SAME lock the leader's roster changes take,
    // so a submission and a reorder or removal in one room always take turns.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}