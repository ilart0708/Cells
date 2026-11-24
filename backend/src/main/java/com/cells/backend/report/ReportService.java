package com.cells.backend.report;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.participation.DevotionSubmission;
import com.cells.backend.participation.DevotionSubmissionRepository;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Members report answers they find offensive; the room's leader keeps or removes them. Removing replaces the
 * text only: the answer still counts for participation and EXP, so nobody's record changes.
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Clock clock;

    public ReportService(ReportRepository reportRepository,
                         DevotionSubmissionRepository submissionRepository,
                         RoomRepository roomRepository,
                         RoomMemberRepository roomMemberRepository,
                         Clock clock) {
        this.reportRepository = reportRepository;
        this.submissionRepository = submissionRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.clock = clock;
    }

    /** Reporting the same answer twice is harmless. */
    @Transactional
    public void report(Long memberId, Long roomId, Long submissionId, ReportRequest request) {
        // A room the member is not in looks like it does not exist.
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException("Room not found.");
        }
        DevotionSubmission submission = submissionRepository.findById(submissionId)
                .filter(s -> s.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));
        if (submission.getMemberId().equals(memberId)) {
            throw new BusinessRuleException("You cannot report your own answer.");
        }
        if (submission.getModeratedAt() != null
                || reportRepository.existsBySubmissionIdAndReporterId(submissionId, memberId)) {
            return;
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().strip();
        reportRepository.save(new ContentReport(submissionId, roomId, memberId, request.reason(), note));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> openReports(Long leaderId, Long roomId) {
        requireOwnRoom(leaderId, roomId);
        return reportRepository.findOpenForRoom(roomId);
    }

    /** "Keep the answer". Closing a report that is already closed is harmless. */
    @Transactional
    public void dismiss(Long leaderId, Long roomId, Long reportId) {
        requireOwnRoom(leaderId, roomId);
        ContentReport report = reportRepository.findByIdAndRoomId(reportId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        if (report.getStatus() == ReportStatus.OPEN) {
            report.resolve(ReportStatus.DISMISSED, clock.instant(), leaderId);
        }
    }

    /** "Remove the answer": the text is replaced and every open report about it is closed. */
    @Transactional
    public void removeAnswer(Long leaderId, Long roomId, Long reportId) {
        requireOwnRoom(leaderId, roomId);
        ContentReport report = reportRepository.findByIdAndRoomId(reportId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        DevotionSubmission submission = submissionRepository.findById(report.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));
        if (submission.getModeratedAt() == null) {
            submission.moderate(clock.instant());
        }
        reportRepository.resolveOpenAsRemoved(submission.getId(), clock.instant(), leaderId);
    }

    private void requireOwnRoom(Long leaderId, Long roomId) {
        // Another leader's room looks exactly like a room that does not exist.
        roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
    }
}
