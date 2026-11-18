package com.cells.backend.devotion;

import jakarta.validation.constraints.NotNull;

public record DevotionStatusRequest(@NotNull DevotionStatus status) {
}