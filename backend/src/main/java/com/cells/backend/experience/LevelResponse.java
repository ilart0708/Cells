package com.cells.backend.experience;

public record LevelResponse(String code, String name, int threshold) {

    public static LevelResponse from(ExperienceLevel level) {
        return new LevelResponse(level.name(), level.displayName(), level.threshold());
    }
}