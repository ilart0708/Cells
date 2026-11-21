package com.cells.backend.stats;

/**
 * Participation over a window of completed days. At room level devotionDays and possibleDays count days; at
 * leader and dashboard level they count room-days. expected and shared count member-days, so a member counts
 * once for each devotion day they were expected to take part in.
 */
public record PeriodStats(int devotionDays, int possibleDays, int expected, int shared, Integer ratePercent) {

    public static PeriodStats of(int devotionDays, int possibleDays, int expected, int shared) {
        return new PeriodStats(devotionDays, possibleDays, expected, shared, StatsMath.rate(shared, expected));
    }

    public static PeriodStats none(int possibleDays) {
        return of(0, possibleDays, 0, 0);
    }

    public PeriodStats plus(PeriodStats other) {
        return of(devotionDays + other.devotionDays, possibleDays + other.possibleDays,
                expected + other.expected, shared + other.shared);
    }
}