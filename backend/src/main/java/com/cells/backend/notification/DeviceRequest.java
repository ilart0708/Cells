package com.cells.backend.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank @Size(max = 512) String token,
        @NotNull DevicePlatform platform) {
}
