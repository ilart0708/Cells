package com.cells.backend.participation;

import java.time.Instant;
import java.time.LocalDate;

public record ExemptionResponse(Long memberId, LocalDate date, Instant grantedAt) {

    public static ExemptionResponse from(DailyExemption e) {
        return new ExemptionResponse(e.getMemberId(), e.getExemptionDate(), e.getGrantedAt());
    }
}