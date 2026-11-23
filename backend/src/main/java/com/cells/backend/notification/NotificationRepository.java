package com.cells.backend.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Stores the notification once. A second request for the same (user, type, room, devotion, day) does
     * nothing and returns 0, so the midnight job, the startup catch-up and the live hooks can overlap safely.
     * Plain INSERT ... ON CONFLICT does not abort the surrounding transaction the way a failed save() would.
     */
    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into notifications (user_id, type, room_id, devotion_id, cycle_date, status, attempts, created_at, updated_at)
            values (:userId, :type, :roomId, :devotionId, :cycleDate, 'PENDING', 0, :now, :now)
            on conflict on constraint uq_notification_event do nothing
            """, nativeQuery = true)
    int enqueue(@Param("userId") Long userId, @Param("type") String type, @Param("roomId") Long roomId,
                @Param("devotionId") Long devotionId, @Param("cycleDate") LocalDate cycleDate,
                @Param("now") Instant now);

    /** Not finished, still allowed another try, or stuck in SENDING for longer than the stale limit (a crash). */
    @Query("""
            select n.id from Notification n
            where n.attempts < :maxAttempts
              and (n.status in :retryable or (n.status = :sending and n.updatedAt < :stale))
            order by n.id
            """)
    List<Long> findDispatchable(@Param("retryable") Collection<NotificationStatus> retryable,
                                @Param("sending") NotificationStatus sending,
                                @Param("stale") Instant stale,
                                @Param("maxAttempts") int maxAttempts,
                                Pageable pageable);

    /** Takes the row for this sender. Returns 0 when someone else took it first or it is already done. */
    @Transactional
    @Modifying
    @Query("""
            update Notification n
               set n.status = :sending, n.attempts = n.attempts + 1, n.updatedAt = :now
             where n.id = :id and n.attempts < :maxAttempts
               and (n.status in :retryable or (n.status = :sending and n.updatedAt < :stale))
            """)
    int claim(@Param("id") Long id, @Param("sending") NotificationStatus sending,
              @Param("retryable") Collection<NotificationStatus> retryable, @Param("now") Instant now,
              @Param("stale") Instant stale, @Param("maxAttempts") int maxAttempts);

    @Transactional
    @Modifying
    @Query("update Notification n set n.emailSentAt = :now, n.updatedAt = :now where n.id = :id")
    int markEmailSent(@Param("id") Long id, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("update Notification n set n.pushSentAt = :now, n.updatedAt = :now where n.id = :id")
    int markPushSent(@Param("id") Long id, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("update Notification n set n.status = :status, n.lastError = :error, n.updatedAt = :now where n.id = :id")
    int finish(@Param("id") Long id, @Param("status") NotificationStatus status,
               @Param("error") String error, @Param("now") Instant now);

    @Query("""
            select new com.cells.backend.notification.NotificationDetails(
                n.id, n.kind, n.userId, u.email, u.fullName,
                n.roomId, n.devotionId, r.name, c.name, lu.fullName,
                d.bibleBook, d.chapter, d.verseStart, d.verseEnd, d.title,
                n.emailSentAt, n.pushSentAt)
            from Notification n
            join User u on u.id = n.userId
            join Room r on r.id = n.roomId
            join r.church c
            join User lu on lu.id = r.leaderId
            join Devotion d on d.id = n.devotionId
            where n.id = :id
            """)
    Optional<NotificationDetails> findDetails(@Param("id") Long id);
}
