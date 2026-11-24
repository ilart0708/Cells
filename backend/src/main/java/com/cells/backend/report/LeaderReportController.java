package com.cells.backend.report;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The leader reviews reports about answers in their own rooms. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/reports")
@PreAuthorize("hasRole('LEADER')")
public class LeaderReportController {

    private final ReportService reportService;

    public LeaderReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportResponse> open(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return reportService.openReports(Long.valueOf(jwt.getSubject()), roomId);
    }

    @PostMapping("/{reportId}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long reportId) {
        reportService.dismiss(Long.valueOf(jwt.getSubject()), roomId, reportId);
    }

    @PostMapping("/{reportId}/remove-answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAnswer(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long reportId) {
        reportService.removeAnswer(Long.valueOf(jwt.getSubject()), roomId, reportId);
    }
}
