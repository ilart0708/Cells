package com.cells.backend.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable page shape for every list endpoint (the raw Spring Page is not part of our API). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}