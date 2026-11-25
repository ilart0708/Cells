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

    @Column(name = "self_registered", nullable = false)
    private boolean selfRegistered = false;

    // Self-registered churches start unverified. Administrator-created churches are always verified.
    @Column(nullable = false)
    private boolean verified = true;

    @Column(name = "pastor_name")
    private String pastorName;

    @Column(name = "pastor_email")
    private String pastorEmail;

    protected Church() {
        // required by JPA
    }

    public Church(String name) {
        this.name = name.trim();
    }

    /** A church created by a leader who signed up without an invitation. Not verified until an administrator says so. */
    public static Church selfRegistered(String name, String pastorName, String pastorEmail) {
        Church church = new Church(name);
        church.selfRegistered = true;
        church.verified = false;
        church.pastorName = pastorName.trim();
        church.pastorEmail = (pastorEmail == null || pastorEmail.isBlank()) ? null : pastorEmail.trim().toLowerCase();
        return church;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ChurchStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isSelfRegistered() { return selfRegistered; }
    public boolean isVerified() { return verified; }
    public String getPastorName() { return pastorName; }
    public String getPastorEmail() { return pastorEmail; }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

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