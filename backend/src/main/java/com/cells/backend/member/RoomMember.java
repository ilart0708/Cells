package com.cells.backend.room;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "room_members")
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "participation_number", nullable = false)
    private Integer participationNumber;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "removed_at")
    private Instant removedAt;

    protected RoomMember() {
        // required by JPA
    }

    public RoomMember(Long roomId, Long memberId, Integer participationNumber) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.participationNumber = participationNumber;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public Long getMemberId() { return memberId; }
    public Integer getParticipationNumber() { return participationNumber; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getRemovedAt() { return removedAt; }

    /** Closes the membership. The row stays, so history is kept (used by removal in Step 4). */
    public void markRemoved(Instant now) {
        this.removedAt = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomMember other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}