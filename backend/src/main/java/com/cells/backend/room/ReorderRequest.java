package com.cells.backend.room;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The members' ids in the new participation order. Every current member must appear exactly once. */
public record ReorderRequest(@NotNull @Size(max = 500) List<@NotNull Long> memberIds) {
}