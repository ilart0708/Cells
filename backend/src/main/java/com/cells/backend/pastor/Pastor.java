package com.cells.backend.pastor;

import com.cells.backend.church.Church;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pastors")
public class Pastor {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private Church church;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Pastor() {
        // required by JPA
    }

    public Pastor(Long userId, Church church) {
        this.userId = userId;
        this.church = church;
    }

    public Long getUserId() { return userId; }
    public Church getChurch() { return church; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pastor other)) return false;
        return userId != null && userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}