package com.cells.backend.participation;

import com.cells.backend.devotion.DevotionResponse;

import java.time.LocalDate;
import java.util.List;

/** devotion is null when nothing is posted today. The counts cover the room's current members. */
public record LeaderBoardResponse(
        LocalDate today, DevotionResponse devotion,
        int memberCount, int submittedCount, int exemptedCount,
        List<LeaderBoardEntryResponse> order) {
}