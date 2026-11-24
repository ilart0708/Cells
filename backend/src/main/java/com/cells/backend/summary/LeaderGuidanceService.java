package com.cells.backend.summary;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.notification.NotificationService;
import com.cells.backend.notification.NotificationType;
import com.cells.backend.participation.DevotionSubmissionRepository;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/** Every method takes the leader's id from the token and only ever touches that leader's rooms. */
@Service
public class LeaderGuidanceService {

    private static final String ARCHIVED = "Archived rooms cannot be changed.";
    private static final String NO_ANSWERS =
            "You can write a summary or reflection once at least one member has shared their understanding.";

    private final RoomRepository roomRepository;
    private final DevotionRepository devotionRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final DevotionSummaryRepository summaryRepository;
    private final NotificationService notificationService;

    public LeaderGuidanceService(RoomRepository roomRepository,
                                 DevotionRepository devotionRepository,
                                 DevotionSubmissionRepository submissionRepository,
                                 DevotionSummaryRepository summaryRepository,
                                 NotificationService notificationService) {
        this.roomRepository = roomRepository;
        this.devotionRepository = devotionRepository;
        this.submissionRepository = submissionRepository;
        this.summaryRepository = summaryRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public LeaderGuidanceResponse get(Long leaderId, Long roomId, Long devotionId) {
        // Another leader's room looks exactly like a room that does not exist.
        Room room = roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        requireDevotion(roomId, devotionId);

        int answers = (int) submissionRepository.countByDevotionId(devotionId);
        String blocked = blockedReason(room, answers);

        Map<SummaryType, DevotionSummary> notes = notesOf(devotionId);
        return new LeaderGuidanceResponse(
                devotionId, answers, blocked == null, blocked,
                SummaryNoteResponse.from(notes.get(SummaryType.SUMMARY)),
                SummaryNoteResponse.from(notes.get(SummaryType.REFLECTION)));
    }

    /** Creates the note the first time, and replaces its text after that. */
    @Transactional
    public SummaryNoteResponse save(Long leaderId, Long roomId, Long devotionId, SummaryType type, String rawContent) {
        // The room lock makes two saves (a double click, two tabs) take turns instead of clashing.
        Room room = roomRepository.findOwnForUpdate(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        requireDevotion(roomId, devotionId);

        int answers = (int) submissionRepository.countByDevotionId(devotionId);
        String blocked = blockedReason(room, answers);
        if (blocked != null) {
            throw new BusinessRuleException(blocked);
        }

        String content = rawContent.strip();
        DevotionSummary note = summaryRepository.findByDevotionIdAndType(devotionId, type).orElse(null);
        if (note == null) {
            note = summaryRepository.save(new DevotionSummary(devotionId, roomId, type, content, leaderId));
            // Only the first save tells the room; later edits are quiet.
            notificationService.guidancePosted(roomId, devotionId,
                    type == SummaryType.SUMMARY ? NotificationType.SUMMARY_POSTED : NotificationType.REFLECTION_POSTED);
        } else {
            note.edit(content, Instant.now());
        }
        return SummaryNoteResponse.from(note);
    }

    // ---------- helpers ----------

    private static String blockedReason(Room room, int answers) {
        if (room.getStatus() != RoomStatus.ACTIVE) {
            return ARCHIVED;
        }
        return answers == 0 ? NO_ANSWERS : null;
    }

    private void requireDevotion(Long roomId, Long devotionId) {
        devotionRepository.findByIdAndRoomId(devotionId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Devotion not found."));
    }

    private Map<SummaryType, DevotionSummary> notesOf(Long devotionId) {
        Map<SummaryType, DevotionSummary> notes = new EnumMap<>(SummaryType.class);
        summaryRepository.findByDevotionId(devotionId).forEach(note -> notes.put(note.getType(), note));
        return notes;
    }
}