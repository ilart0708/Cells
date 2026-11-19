package com.cells.backend.participation;

/** One line of the order as a fellow member sees it: no email, no ids. status is null when no devotion is posted today. */
public record BoardEntryResponse(Integer participationNumber, String fullName, boolean you, ParticipationStatus status) {
}