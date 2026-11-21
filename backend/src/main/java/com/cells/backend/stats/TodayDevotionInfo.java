package com.cells.backend.stats;

import com.cells.backend.devotion.LearningLevel;

/** The passage a room is on today. Only the reference, title and level: never the instructions or any answer. */
public record TodayDevotionInfo(String reference, String title, LearningLevel learningLevel) {
}