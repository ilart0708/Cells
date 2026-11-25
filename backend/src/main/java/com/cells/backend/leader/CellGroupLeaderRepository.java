package com.cells.backend.leader;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CellGroupLeaderRepository extends JpaRepository<CellGroupLeader, Long> {

    String SELECT_DTO = """
            select new com.cells.backend.leader.LeaderResponse(
                u.id, u.fullName, u.email, u.status, c.id, c.name, pu.id, coalesce(pu.fullName, c.pastorName), u.createdAt, c.verified)
            """;

    String FROM_JOINS = """
            from CellGroupLeader l
            join l.church c
            join User u on u.id = l.userId
            left join PastorLeader pl on pl.leaderId = l.userId
            left join User pu on pu.id = pl.pastorId
            """;

    String MATCHES = " where (lower(u.fullName) like :pattern or lower(u.email) like :pattern) ";
    String ORDER = " order by lower(u.fullName), u.id";

    // One query returns everything a list needs, so there is no N+1 problem.
    // The sort is part of the query, so pass an unsorted Pageable.

    @Query(value = SELECT_DTO + FROM_JOINS + MATCHES + ORDER,
            countQuery = "select count(l) " + FROM_JOINS + MATCHES)
    Page<LeaderResponse> search(@Param("pattern") String pattern, Pageable pageable);

    @Query(value = SELECT_DTO + FROM_JOINS + MATCHES + "and pl.pastorId = :pastorId" + ORDER,
            countQuery = "select count(l) " + FROM_JOINS + MATCHES + "and pl.pastorId = :pastorId")
    Page<LeaderResponse> searchForPastor(@Param("pattern") String pattern,
                                         @Param("pastorId") Long pastorId, Pageable pageable);

    @Query(SELECT_DTO + FROM_JOINS + " where l.userId = :id")
    Optional<LeaderResponse> findResponseById(@Param("id") Long id);

    @Query(SELECT_DTO + FROM_JOINS + " where l.userId = :id and pl.pastorId = :pastorId")
    Optional<LeaderResponse> findResponseByIdForPastor(@Param("id") Long id, @Param("pastorId") Long pastorId);
}