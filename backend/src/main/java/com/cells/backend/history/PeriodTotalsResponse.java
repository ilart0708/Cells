package com.cells.backend.history;

/**
 * Counts for a period. expected = shared + missed: exempt, blocked and today's pending devotions are
 * left out, so nobody is penalized for something that was not their doing. ratePercent is null when
 * nothing was expected yet.
 */
public record PeriodTotalsResponse(
        int shared, int missed, int blocked, int exempt, int pending,
        int expected, Integer ratePercent, int points) {

    public static PeriodTotalsResponse of(int shared, int missed, int blocked, int exempt, int pending, int points) {
        int expected = shared + missed;
        Integer rate = expected == 0 ? null : (int) Math.round(shared * 100.0 / expected);
        return new PeriodTotalsResponse(shared, missed, blocked, exempt, pending, expected, rate, points);
    }

    public static PeriodTotalsResponse empty() {
        return of(0, 0, 0, 0, 0, 0);
    }

    public PeriodTotalsResponse plus(PeriodTotalsResponse other) {
        return of(shared + other.shared, missed + other.missed, blocked + other.blocked,
                exempt + other.exempt, pending + other.pending, points + other.points);
    }
}