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

    // The entity graphs fetch church and room in the same query, so lists do not trigger one query per row.
    @Override
    @EntityGraph(attributePaths = {"church", "room"})
    Page<Invitation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"church", "room"})
    Page<Invitation> findByInvitedBy(Long invitedBy, Pageable pageable);

    @EntityGraph(attributePaths = {"church", "room"})
    Page<Invitation> findByRoom_IdAndInvitedBy(Long roomId, Long invitedBy, Pageable pageable);

    Optional<Invitation> findByIdAndInvitedBy(Long id, Long invitedBy);

    Optional<Invitation> findByIdAndInvitedByAndRoom_Id(Long id, Long invitedBy, Long roomId);

    // Pastor and leader invitations (no room): a new link replaces this inviter's older one.
    @Modifying
    @Query("update Invitation i set i.revokedAt = :now "
            + "where i.email = :email and i.invitedBy = :invitedBy and i.room is null "
            + "and i.acceptedAt is null and i.revokedAt is null")
    int revokePendingForEmail(@Param("email") String email,
                              @Param("invitedBy") Long invitedBy,
                              @Param("now") Instant now);

    // Member invitations: a new link replaces the older one for the SAME room only.
    @Modifying
    @Query("update Invitation i set i.revokedAt = :now "
            + "where i.email = :email and i.invitedBy = :invitedBy and i.room.id = :roomId "
            + "and i.acceptedAt is null and i.revokedAt is null")
    int revokePendingForEmailInRoom(@Param("email") String email,
                                    @Param("invitedBy") Long invitedBy,
                                    @Param("roomId") Long roomId,
                                    @Param("now") Instant now);
}