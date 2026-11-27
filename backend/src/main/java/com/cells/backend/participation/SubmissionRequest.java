package com.cells.backend.participation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** takeaway and highlights are optional. Highlights are character ranges of the (trimmed) content. */
public record SubmissionRequest(
        @NotBlank(message = "Please write your understanding of the passage.")
        @Size(max = 2000, message = "Your answer can have at most 2000 characters.") String content,
        @Size(max = 200, message = "Your key takeaway can have at most 200 characters.") String takeaway,
        @Size(max = 5, message = "You can highlight at most 5 passages.") List<@Valid HighlightRange> highlights) {
}
