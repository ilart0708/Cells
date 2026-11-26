package com.cells.backend.church;

import java.time.Instant;

/**
 * pastorName and pastorEmail are only filled for churches a leader registered by themselves,
 * where they are typed text, not a pastor account. verified is false until an administrator verifies it.
 */
public record ChurchResponse(
        Long id, String name, ChurchStatus status, Instant createdAt,
        boolean selfRegistered, boolean verified, String pastorName, String pastorEmail) {

    public static ChurchResponse from(Church church) {
        return new ChurchResponse(church.getId(), church.getName(), church.getStatus(), church.getCreatedAt(),
                church.isSelfRegistered(), church.isVerified(), church.getPastorName(), church.getPastorEmail());
    }
}
