package com.cells.backend.history;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the facts about one member in one room into a history, and from it the streaks and totals.
 * It is a pure function: no database, no clock.
 */
public final class HistoryCalculator {

    private HistoryCalculator() {
    }

    /** A stay in the room. from is the first day that can count; toExclusive is null while still in. */
    public record Window(LocalDate from, LocalDate toExclusive) {
        boolean contains(LocalDate day) {
            return !day.isBefore(from) && (toExclusive == null || day.isBefore(toExclusive));
        }
    }

    /** A member ahead in the order, and the day they joined the room. */
    public record Predecessor(Long memberId, LocalDate joinedOn) {
    }

    public record Entry(DevotionDay day, HistoryStatus status, Integer points, Instant submittedAt) {
    }

    public record Result(List<Entry> entries, int currentStreak, int bestStreak) {
    }

    /**
     * @param days                        active devotions dated today or earlier, oldest first
     * @param predecessorsNearestFirst    members ahead of this one in the current order, nearest first
     * @param predecessorSharesByDevotion devotion id -> the predecessors who shared it
     * @param predecessorExemptDates      predecessor id -> the days they were exempt
     */
    public static Result calculate(
            LocalDate today,
            List<DevotionDay> days,
            List<Window> windows,
            Map<Long, MyShare> myShares,
            Set<LocalDate> myExemptDates,
            boolean orderKnown,
            List<Predecessor> predecessorsNearestFirst,
            Map<Long, Set<Long>> predecessorSharesByDevotion,
            Map<Long, Set<LocalDate>> predecessorExemptDates) {

        List<Entry> entries = new ArrayList<>();
        int run = 0;
        int best = 0;

        for (DevotionDay day : days) {
            MyShare share = myShares.get(day.id());
            // A devotion the member answered always counts, whenever they joined.
            boolean inScope = share != null || windows.stream().anyMatch(w -> w.contains(day.date()));
            if (!inScope) {
                continue;
            }

            HistoryStatus status;
            if (share != null) {
                status = HistoryStatus.SHARED;
            } else if (myExemptDates.contains(day.date())) {
                status = HistoryStatus.EXEMPT;
            } else if (!day.date().isBefore(today)) {
                status = HistoryStatus.PENDING; // today: it can still be shared
            } else {
                status = classifyPast(day, orderKnown, predecessorsNearestFirst,
                        predecessorSharesByDevotion, predecessorExemptDates);
            }

            entries.add(new Entry(
                    day, status,
                    share == null ? null : share.points(),
                    share == null ? null : share.submittedAt()));

            switch (status) {
                case SHARED -> {
                    run++;
                    best = Math.max(best, run);
                }
                case MISSED -> run = 0; // the only thing that breaks a streak
                case BLOCKED, EXEMPT, PENDING -> {
                    // paused: not the member's doing, or not over yet
                }
            }
        }

        return new Result(entries, run, best);
    }

    // A past day the member did not share and was not exempt: was it their turn?
    private static HistoryStatus classifyPast(
            DevotionDay day, boolean orderKnown, List<Predecessor> predecessorsNearestFirst,
            Map<Long, Set<Long>> sharesByDevotion, Map<Long, Set<LocalDate>> exemptDates) {

        if (!orderKnown) {
            return HistoryStatus.MISSED; // they have left the room: there is no order left to consult
        }

        for (Predecessor predecessor : predecessorsNearestFirst) {
            if (predecessor.joinedOn().isAfter(day.date())) {
                continue; // not in the room yet that day
            }
            if (exemptDates.getOrDefault(predecessor.memberId(), Set.of()).contains(day.date())) {
                continue; // sat the day out, so the line looked past them
            }
            boolean shared = sharesByDevotion.getOrDefault(day.id(), Set.of()).contains(predecessor.memberId());
            return shared ? HistoryStatus.MISSED : HistoryStatus.BLOCKED;
        }
        return HistoryStatus.MISSED; // nobody ahead: it was their turn from the start
    }

    /** Counts the entries dated from..to (both included). Use LocalDate.MIN and MAX for all time. */
    public static PeriodTotalsResponse totals(List<Entry> entries, LocalDate from, LocalDate to) {
        int shared = 0;
        int missed = 0;
        int blocked = 0;
        int exempt = 0;
        int pending = 0;
        int points = 0;

        for (Entry entry : entries) {
            LocalDate date = entry.day().date();
            if (date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            switch (entry.status()) {
                case SHARED -> shared++;
                case MISSED -> missed++;
                case BLOCKED -> blocked++;
                case EXEMPT -> exempt++;
                case PENDING -> pending++;
            }
            if (entry.points() != null) {
                points += entry.points();
            }
        }
        return PeriodTotalsResponse.of(shared, missed, blocked, exempt, pending, points);
    }
}