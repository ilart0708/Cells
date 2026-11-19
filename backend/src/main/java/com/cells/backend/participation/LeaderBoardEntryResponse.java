package com.cells.backend.participation;

import com.cells.backend.user.UserStatus;

/**
 * One line of the leader's board. status is null when no devotion is posted today.
 * canExempt says whether the "Exempt for Today" toggle may be switched on right now.
 * blocking is true when the line is waiting on someone whose account is disabled: they can never
 * submit, so the leader should exempt or remove them.
 */
public record LeaderBoardEntryResponse(
        Long memberId, Integer participationNumber, String fullName, String email, UserStatus accountStatus,
        ParticipationStatus status, boolean exempt, boolean canExempt, boolean blocking,
        WaitingForResponse waitingFor) {
}