package com.cells.backend.stats;

import java.time.LocalDate;

public record AdminMemberDetailResponse(LocalDate today, AdminMemberRow member) {
}