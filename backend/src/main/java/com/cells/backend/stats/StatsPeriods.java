package com.cells.backend.stats;

import java.time.LocalDate;

/**
 * The days a statistics response covers (platform days). Today is shown on its own. The two windows end
 * yesterday, so a day still in progress never counts against anyone.
 */
public record StatsPeriods(LocalDate today, LocalDate through, LocalDate last7From, LocalDate last30From) {

    public static StatsPeriods of(LocalDate today) {
        return new StatsPeriods(today, today.minusDays(1), today.minusDays(7), today.minusDays(30));
    }
}