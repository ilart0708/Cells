package com.cells.backend.room;

import jakarta.validation.constraints.NotNull;

public record RoomStatusRequest(@NotNull RoomStatus status) {
}