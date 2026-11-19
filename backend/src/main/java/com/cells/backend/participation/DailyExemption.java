package com.cells.backend.participation;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** The leader's "Exempt for Today". Never deleted: revoking closes it, so the history stays. */
@Entity
@Table(name = "daily_exemptions")
public class DailyExemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "exemption_date", nullable = false)
    private LocalDate exemptionDate;

    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    protected DailyExemption() {
        // required by JPA
    }

    public DailyExemption(Long roomId, Long memberId, LocalDate exemptionDate, Long grantedBy) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.exemptionDate = exemptionDate;
        this.grantedBy = grantedBy;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public Long getMemberId() { return memberId; }
    public LocalDate getExemptionDate() { return exemptionDate; }
    public Long getGrantedBy() { return grantedBy; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public void revoke(Long by, Instant now) {
        this.revokedAt = now;
        this.revokedBy = by;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyExemption other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}