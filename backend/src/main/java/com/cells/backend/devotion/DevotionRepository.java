package com.cells.backend.devotion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface DevotionRepository extends JpaRepository<Devotion, Long> {

    Optional<Devotion> findByIdAndRoomId(Long id, Long roomId);

    Optional<Devotion> findByRoomIdAndDevotionDateAndStatus(Long roomId, LocalDate date, DevotionStatus status);

    boolean existsByRoomIdAndDevotionDateAndStatus(Long roomId, LocalDate date, DevotionStatus status);

    boolean existsByRoomIdAndDevotionDateAndStatusAndIdNot(
            Long roomId, LocalDate date, DevotionStatus status, Long id);

    // The leader's history (devotions have no relations, so there is no N+1 problem here).
    Page<Devotion> findByRoomIdAndStatusIn(Long roomId, Collection<DevotionStatus> statuses, Pageable pageable);

    // The member's history: today and earlier, never the leader's preparation.
    Page<Devotion> findByRoomIdAndStatusAndDevotionDateLessThanEqual(
            Long roomId, DevotionStatus status, LocalDate date, Pageable pageable);
}