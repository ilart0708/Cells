package com.cells.backend.reaction;

import jakarta.validation.constraints.NotNull;

public record ReactionRequest(@NotNull(message = "Please choose a reaction.") ReactionType type) {
}
