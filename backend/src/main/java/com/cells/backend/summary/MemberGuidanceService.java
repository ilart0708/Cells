package com.cells.backend.summary;

import com.cells.backend.participation.MemberSubmissionsResponse;
import com.cells.backend.participation.SubmissionReadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/** The member's own view. The member id always comes from the token. */
@Service
public class MemberGuidanceService {

    private static final String LOCKED =
            "Share your own understanding first, then you can read what your leader wrote.";

    private final SubmissionReadService submissionReadService;
    private final DevotionSummaryRepository summaryRepository;

    public MemberGuidanceService(SubmissionReadService submissionReadService,
                                 DevotionSummaryRepository summaryRepository) {
        this.submissionReadService = submissionReadService;
        this.summaryRepository = summaryRepository;
    }

    @Transactional(readOnly = true)
    public MemberGuidanceResponse read(Long memberId, Long roomId, Long devotionId) {
        // The same access check as reading the answers: it answers 404 for a room the member is not in,
        // an archived room, and a devotion members cannot see (future, cancelled, another room's), and it
        // decides whether the member has unlocked the room's content (shared, exempt, or the day is over).
        MemberSubmissionsResponse access = submissionReadService.forMember(memberId, roomId, devotionId);
        if (!access.unlocked()) {
            return new MemberGuidanceResponse(false, LOCKED, null, null);
        }

        Map<SummaryType, DevotionSummary> notes = new EnumMap<>(SummaryType.class);
        summaryRepository.findByDevotionId(devotionId).forEach(note -> notes.put(note.getType(), note));
        return new MemberGuidanceResponse(
                true, null,
                SummaryNoteResponse.from(notes.get(SummaryType.SUMMARY)),
                SummaryNoteResponse.from(notes.get(SummaryType.REFLECTION)));
    }
}