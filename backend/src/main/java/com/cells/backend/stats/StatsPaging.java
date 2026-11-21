package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;

import java.util.List;

/** The same paging rules for every statistics list: page size capped at 100, searches ignore case. */
final class StatsPaging {

    static final int MAX_PAGE_SIZE = 100;

    private StatsPaging() {
    }

    static String pattern(String query) {
        return "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
    }

    static int pageSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    static int pageIndex(int page) {
        return Math.max(page, 0);
    }

    static long offset(int pageIndex, int pageSize) {
        return (long) pageIndex * pageSize;
    }

    static <T> PageResponse<T> page(List<T> items, long total, int pageIndex, int pageSize) {
        return new PageResponse<>(items, pageIndex, pageSize, total, (int) ((total + pageSize - 1) / pageSize));
    }
}