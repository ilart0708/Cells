package com.cells.backend.stats;

/** A member's day in one room. STILL_TO_SHARE is "not yet": the day isn't over, so it is not a miss. */
public enum TodayStatus {
    SHARED, EXEMPT, STILL_TO_SHARE, NO_DEVOTION
}