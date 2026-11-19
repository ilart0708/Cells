package com.cells.backend.participation;

import com.cells.backend.devotion.DevotionResponse;

import java.util.List;

public record LeaderSubmissionsResponse(DevotionResponse devotion, List<LeaderSubmissionResponse> submissions) {
}