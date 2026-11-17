package com.cells.backend.invitation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    // Row lock: a second simultaneous accept waits here, then sees the invitation as used.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invitation i where i.tokenHash = :hash")
    Optional<Invitation> findByTokenHashForUpdate(@Param("hash") String hash);

    // Fetches the church in the same query, so listing does not trigger one query per row.
    @Override
    @EntityGraph(attributePaths = "church")
    Page<Invitation> findAll(Pageable pageable);

    @Modifying
    @Query("update Invitation i set i.revokedAt = :now "
            + "where i.email = :email and i.acceptedAt is null and i.revokedAt is null")
    int revokePendingForEmail(@Param("email") String email, @Param("now") Instant now);
}