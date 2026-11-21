package com.cells.backend.experience;

/**
 * The ladder. Levels are derived from total EXP and never stored, so changing a threshold here needs no
 * migration. The names are deliberately different from the passage learning levels (Beginner, Intermediate,
 * Advanced). They describe a person's own journey, not a ranking.
 */
public enum ExperienceLevel {
    SEED("Seed", 0),
    SPROUT("Sprout", 100),
    BRANCH("Branch", 250),
    TREE("Tree", 500),
    FRUITFUL("Fruitful", 1000);

    private final String displayName;
    private final int threshold;

    ExperienceLevel(String displayName, int threshold) {
        this.displayName = displayName;
        this.threshold = threshold;
    }

    public String displayName() { return displayName; }
    public int threshold() { return threshold; }

    public static ExperienceLevel forPoints(long points) {
        ExperienceLevel result = SEED;
        for (ExperienceLevel level : values()) {
            if (points >= level.threshold) {
                result = level;
            }
        }
        return result;
    }

    /** The next level up, or null at the top. */
    public ExperienceLevel next() {
        ExperienceLevel[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }
}