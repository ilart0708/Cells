package com.cells.backend.board;

import com.cells.backend.experience.LevelResponse;

/**
 * One line of the board. position is null for members with no EXP this month: nobody is ranked at zero.
 * Equal EXP shares a position (1, 1, 3). level is the member's own journey (all-time EXP).
 */
public record GrowthEntryResponse(
        Integer position, String fullName, boolean you, LevelResponse level,
        int pointsThisMonth, int sharedThisMonth, int currentStreak) {
}