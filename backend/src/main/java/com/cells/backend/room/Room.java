package com.cells.backend.room;

import com.cells.backend.church.Church;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Room() {
        // required by JPA
    }

    public Room(Church church, Long leaderId, String name) {
        this.church = church;
        this.leaderId = leaderId;
        this.name = name.trim();
    }

    public Long getId() { return id; }
    public Church getChurch() { return church; }
    public Long getLeaderId() { return leaderId; }
    public String getName() { return name; }
    public RoomStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getArchivedAt() { return archivedAt; }

    public void rename(String newName) {
        this.name = newName.trim();
    }

    /** Does nothing if the room is already archived. */
    public void archive(Instant now) {
        if (status == RoomStatus.ACTIVE) {
            this.status = RoomStatus.ARCHIVED;
            this.archivedAt = now;
        }
    }

    public void restore() {
        this.status = RoomStatus.ACTIVE;
        this.archivedAt = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}