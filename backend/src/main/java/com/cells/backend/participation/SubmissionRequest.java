package com.cells.backend.participation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmissionRequest(
        @NotBlank(message = "Please write your understanding of the passage.")
        @Size(max = 2000, message = "Your answer can have at most 2000 characters.") String content) {
}