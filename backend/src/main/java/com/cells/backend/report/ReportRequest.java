package com.cells.backend.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(@NotNull ReportReason reason, @Size(max = 500) String note) {
}
