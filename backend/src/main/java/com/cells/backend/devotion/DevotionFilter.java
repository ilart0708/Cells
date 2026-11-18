package com.cells.backend.devotion;

import java.util.List;

/** Which devotions a history request wants. Active is the default. */
public enum DevotionFilter {
    ACTIVE, CANCELLED, ALL;

    public List<DevotionStatus> statuses() {
        return switch (this) {
            case ACTIVE -> List.of(DevotionStatus.ACTIVE);
            case CANCELLED -> List.of(DevotionStatus.CANCELLED);
            case ALL -> List.of(DevotionStatus.ACTIVE, DevotionStatus.CANCELLED);
        };
    }
}