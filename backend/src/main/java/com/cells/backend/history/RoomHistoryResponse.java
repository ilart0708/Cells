package com.cells.backend.history;

import com.cells.backend.common.PageResponse;

public record RoomHistoryResponse(RoomHistorySummaryResponse summary, PageResponse<HistoryEntryResponse> entries) {
}