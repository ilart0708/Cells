package com.cells.backend.notification;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One "tell this member about this event" row. It is only ever created by NotificationRepository.enqueue
 * (insert ... on conflict do nothing), so this class is read-only.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private NotificationType kind;

    @Column(name = "room_id", nullable = false, updatable = false)
    private Long roomId;

    @Column(name = "devotion_id", nullable = false, updatable = false)
    private Long devotionId;

    @Column(name = "cycle_date", nullable = false, updatable = false)
    private LocalDate cycleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "push_sent_at")
    private Instant pushSentAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Notification() {
        // required by JPA
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationType getKind() { return kind; }
    public Long getRoomId() { return roomId; }
    public Long getDevotionId() { return devotionId; }
    public LocalDate getCycleDate() { return cycleDate; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getEmailSentAt() { return emailSentAt; }
    public Instant getPushSentAt() { return pushSentAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
