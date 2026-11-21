package com.cells.backend.board;

import java.time.LocalDate;
import java.util.List;

/**
 * Growing Together for one room and month. message carries the spirit of the board, so every screen shows
 * the same words. iAmShown is the member's own choice (null in the leader's view). daysLeftInMonth counts today.
 */
public record GrowthBoardResponse(
        LocalDate today, LocalDate monthStart, int daysLeftInMonth, String message,
        Boolean iAmShown, int shownCount, List<GrowthEntryResponse> entries) {
}