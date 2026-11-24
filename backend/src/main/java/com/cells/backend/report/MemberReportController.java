package com.cells.backend.report;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** A member reports a fellow member's answer in one of their own rooms. The reporter id comes from the token. */
@RestController
@RequestMapping("/api/v1/member/rooms/{roomId}/submissions/{submissionId}/report")
@PreAuthorize("hasRole('MEMBER')")
public class MemberReportController {

    private final ReportService reportService;

    public MemberReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                       @PathVariable Long submissionId, @Valid @RequestBody ReportRequest request) {
        reportService.report(Long.valueOf(jwt.getSubject()), roomId, submissionId, request);
    }
}
