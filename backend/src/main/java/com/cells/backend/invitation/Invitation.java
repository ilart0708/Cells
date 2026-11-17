package com.cells.backend.invitation;

import com.cells.backend.church.Church;
import com.cells.backend.room.Room;
import com.cells.backend.user.Role;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    // Only member invitations have a room.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Invitation() {
        // required by JPA
    }

    public Invitation(String tokenHash, String email, String fullName, Role role,
                      Church church, Long invitedBy, Instant expiresAt) {
        this(tokenHash, email, fullName, role, church, null, invitedBy, expiresAt);
    }

    public Invitation(String tokenHash, String email, String fullName, Role role,
                      Church church, Room room, Long invitedBy, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.church = church;
        this.room = room;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public Church getChurch() { return church; }
    public Room getRoom() { return room; }
    public Long getInvitedBy() { return invitedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public InvitationStatus getStatus(Instant now) {
        if (acceptedAt != null) return InvitationStatus.ACCEPTED;
        if (revokedAt != null) return InvitationStatus.REVOKED;
        if (!expiresAt.isAfter(now)) return InvitationStatus.EXPIRED;
        return InvitationStatus.PENDING;
    }

    public void markAccepted(Instant now) { this.acceptedAt = now; }
    public void markRevoked(Instant now) { this.revokedAt = now; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invitation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}