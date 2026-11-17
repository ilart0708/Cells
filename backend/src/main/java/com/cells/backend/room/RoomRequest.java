package com.cells.backend.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only the room's own name: the church comes from the leader, and the display name is built on screen. */
public record RoomRequest(@NotBlank @Size(max = 100) String name) {
}