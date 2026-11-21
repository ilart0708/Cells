package com.cells.backend.experience;

import java.util.List;

/**
 * The member's own progress. nextLevel is null at the top. levelProgressPercent is the share of the way from
 * the current level to the next (100 at the top). levels is the whole ladder, so screens never keep their own copy.
 */
public record ExperienceSummaryResponse(
        int totalPoints, int totalSubmissions,
        LevelResponse level, LevelResponse nextLevel, int pointsToNextLevel, int levelProgressPercent,
        List<LevelResponse> levels,
        List<RoomExperienceResponse> rooms) {
}