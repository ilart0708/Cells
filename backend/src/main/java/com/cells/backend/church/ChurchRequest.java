package com.cells.backend.church;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChurchRequest(@NotBlank @Size(max = 150) String name) {
}