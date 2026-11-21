package com.cells.backend.summary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuidanceRequest(
        @NotBlank(message = "Please write something first.")
        @Size(max = 4000, message = "This can have at most 4000 characters.") String content) {
}