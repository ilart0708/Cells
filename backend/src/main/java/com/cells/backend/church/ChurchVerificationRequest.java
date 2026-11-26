package com.cells.backend.church;

import jakarta.validation.constraints.NotNull;

public record ChurchVerificationRequest(@NotNull Boolean verified) {
}
