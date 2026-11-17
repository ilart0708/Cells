package com.cells.backend.pastor;

import com.cells.backend.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record PastorStatusRequest(@NotNull UserStatus status) {
}