package com.cells.backend.leader;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pastor_leaders")
public class PastorLeader {

    @Id
    @Column(name = "leader_id")
    private Long leaderId;

    @Column(name = "pastor_id", nullable = false)
    private Long pastorId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    protected PastorLeader() {
        // required by JPA
    }

    public PastorLeader(Long leaderId, Long pastorId) {
        this.leaderId = leaderId;
        this.pastorId = pastorId;
    }

    public Long getLeaderId() { return leaderId; }
    public Long getPastorId() { return pastorId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PastorLeader other)) return false;
        return leaderId != null && leaderId.equals(other.leaderId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}