package com.cells.backend.participation;

import jakarta.validation.constraints.Min;

/** One marked passage inside an answer: characters start (included) to end (excluded). */
public record HighlightRange(@Min(0) int start, @Min(1) int end) {
}
