package com.cells.backend.church;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "churches")
public class Church {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChurchStatus status = ChurchStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Church() {
        // required by JPA
    }

    public Church(String name) {
        this.name = name.trim();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ChurchStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void rename(String newName) {
        this.name = newName.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Church other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}