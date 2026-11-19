package com.cells.backend.participation;

import com.cells.backend.devotion.MemberDevotionResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * The member's participation screen in one call. devotion, myStatus, waitingFor and mySubmission are
 * null when they do not apply (nothing posted today, not waiting, not submitted yet).
 */
public record MemberBoardResponse(
        LocalDate today,
        MemberDevotionResponse devotion,
        Integer myParticipationNumber,
        ParticipationStatus myStatus,
        WaitingForResponse waitingFor,
        SubmissionResponse mySubmission,
        List<BoardEntryResponse> order) {
}