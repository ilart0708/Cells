package com.cells.backend.participation;

import com.cells.backend.devotion.MemberDevotionResponse;

import java.util.List;

/**
 * unlocked is false while the member has not shared their own understanding yet. Then submissions is empty
 * (no text is sent), lockedMessage says why, and submittedCount tells how many members have already shared.
 */
public record MemberSubmissionsResponse(
        MemberDevotionResponse devotion, boolean unlocked, String lockedMessage,
        int submittedCount, List<MemberSubmissionResponse> submissions) {
}