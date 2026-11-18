package com.cells.backend.room;

/** One line of the participation order as a fellow member sees it: no email, no ids. */
public record ParticipationEntryResponse(Integer participationNumber, String fullName, boolean you) {
}