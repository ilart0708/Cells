package com.cells.backend.stats;

/**
 * Today's participation. expected = shared + stillToShare (members minus exempt). roomsWithDevotion counts
 * the rooms that have a devotion today: rooms without one are left out of every figure.
 */
public record TodayStats(
        int roomsWithDevotion, int shared, int exempt, int stillToShare, int expected, Integer ratePercent) {

    public static TodayStats of(int roomsWithDevotion, int shared, int exempt, int stillToShare) {
        int expected = shared + stillToShare;
        return new TodayStats(roomsWithDevotion, shared, exempt, stillToShare, expected, StatsMath.rate(shared, expected));
    }

    public static TodayStats none() {
        return of(0, 0, 0, 0);
    }

    public TodayStats plus(TodayStats other) {
        return of(roomsWithDevotion + other.roomsWithDevotion, shared + other.shared,
                exempt + other.exempt, stillToShare + other.stillToShare);
    }
}