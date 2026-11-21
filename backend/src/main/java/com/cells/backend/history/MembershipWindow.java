package com.cells.backend.history;

import java.time.Instant;

/** One stay of a member in a room. removedAt is null while they are still in it. */
public record MembershipWindow(Instant joinedAt, Instant removedAt) {
}