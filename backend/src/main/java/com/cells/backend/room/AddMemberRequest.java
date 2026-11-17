package com.cells.backend.room;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotNull Long memberId) {
}