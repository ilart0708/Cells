package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;

import java.time.LocalDate;

/** today is the platform's day that every roomsToday status refers to. */
public record AdminMembersResponse(LocalDate today, PageResponse<AdminMemberRow> members) {
}