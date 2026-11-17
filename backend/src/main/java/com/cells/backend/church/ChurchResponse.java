package com.cells.backend.church;

import java.time.Instant;

public record ChurchResponse(Long id, String name, ChurchStatus status, Instant createdAt) {

    public static ChurchResponse from(Church church) {
        return new ChurchResponse(church.getId(), church.getName(), church.getStatus(), church.getCreatedAt());
    }
}