package com.cells.backend.room;

import java.util.List;

/** Which rooms a list request wants. Active is the default. */
public enum RoomFilter {
    ACTIVE, ARCHIVED, ALL;

    public List<RoomStatus> statuses() {
        return switch (this) {
            case ACTIVE -> List.of(RoomStatus.ACTIVE);
            case ARCHIVED -> List.of(RoomStatus.ARCHIVED);
            case ALL -> List.of(RoomStatus.ACTIVE, RoomStatus.ARCHIVED);
        };
    }
}