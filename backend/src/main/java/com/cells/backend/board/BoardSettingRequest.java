package com.cells.backend.board;

import jakarta.validation.constraints.NotNull;

public record BoardSettingRequest(@NotNull(message = "Choose whether to be shown.") Boolean showOnBoard) {
}