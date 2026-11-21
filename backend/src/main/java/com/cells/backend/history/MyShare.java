package com.cells.backend.history;

import java.time.Instant;

public record MyShare(Long devotionId, Instant submittedAt, Integer points) {
}