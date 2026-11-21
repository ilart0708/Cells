package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;

/** A page of statistics lines, together with the days they cover. */
public record StatsPageResponse<T>(StatsPeriods periods, PageResponse<T> items) {
}