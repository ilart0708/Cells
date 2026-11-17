package com.cells.backend.pastor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PastorRepository extends JpaRepository<Pastor, Long> {

    // One query returns everything the list needs (pastor + user + church), so there is
    // no N+1 problem. The sort order is part of the query, so pass an unsorted Pageable.
    @Query(value = """
            select new com.cells.backend.pastor.PastorResponse(
                u.id, u.fullName, u.email, u.status, c.id, c.name, u.createdAt)
            from Pastor p join p.church c join User u on u.id = p.userId
            where lower(u.fullName) like :pattern
               or lower(u.email) like :pattern
               or lower(c.name) like :pattern
            order by lower(u.fullName), u.id
            """,
            countQuery = """
            select count(p) from Pastor p join p.church c join User u on u.id = p.userId
            where lower(u.fullName) like :pattern
               or lower(u.email) like :pattern
               or lower(c.name) like :pattern
            """)
    Page<PastorResponse> search(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
            select new com.cells.backend.pastor.PastorResponse(
                u.id, u.fullName, u.email, u.status, c.id, c.name, u.createdAt)
            from Pastor p join p.church c join User u on u.id = p.userId
            where p.userId = :id
            """)
    Optional<PastorResponse> findResponseById(@Param("id") Long id);
}