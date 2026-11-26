package com.cells.backend.reaction;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.Devotion;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.participation.DailyExemptionRepository;
import com.cells.backend.participation.DevotionSubmission;
import com.cells.backend.participation.DevotionSubmissionRepository;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reactions to shared answers. A member may react once they could read the answers (they shared their own, are
 * exempt that day, or the day is over); the room's leader may always react. Nobody reacts to their own answer or
 * to an answer the leader removed. Counts are shown, who reacted is not.
 */
@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final DevotionRepository devotionRepository;
    private final DailyExemptionRepository exemptionRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Clock clock;

    public ReactionService(ReactionRepository reactionRepository,
                           DevotionSubmissionRepository submissionRepository,
                           DevotionRepository devotionRepository,
                           DailyExemptionRepository exemptionRepository,
                           RoomRepository roomRepository,
                           RoomMemberRepository roomMemberRepository,
                           Clock clock) {
        this.reactionRepository = reactionRepository;
        this.submissionRepository = submissionRepository;
        this.devotionRepository = devotionRepository;
        this.exemptionRepository = exemptionRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.clock = clock;
    }

    @Transactional
    public ReactionSummary memberReacts(Long memberId, Long roomId, Long submissionId, ReactionType type) {
        DevotionSubmission submission = checkedForMember(memberId, roomId, submissionId);
        reactionRepository.upsert(submissionId, roomId, memberId, type.name());
        return summaryOf(memberId, submission.getId());
    }

    @Transactional
    public ReactionSummary memberRemoves(Long memberId, Long roomId, Long submissionId) {
        DevotionSubmission submission = checkedForMember(memberId, roomId, submissionId);
        reactionRepository.deleteMine(submissionId, memberId);
        return summaryOf(memberId, submission.getId());
    }

    @Transactional
    public ReactionSummary leaderReacts(Long leaderId, Long roomId, Long submissionId, ReactionType type) {
        DevotionSubmission submission = checkedForLeader(leaderId, roomId, submissionId);
        reactionRepository.upsert(submissionId, roomId, leaderId, type.name());
        return summaryOf(leaderId, submission.getId());
    }

    @Transactional
    public ReactionSummary leaderRemoves(Long leaderId, Long roomId, Long submissionId) {
        DevotionSubmission submission = checkedForLeader(leaderId, roomId, submissionId);
        reactionRepository.deleteMine(submissionId, leaderId);
        return summaryOf(leaderId, submission.getId());
    }

    /** Counts and "my reaction" for many answers at once (no query per answer). */
    @Transactional(readOnly = true)
    public Map<Long, ReactionSummary> summaries(Long userId, Collection<Long> submissionIds) {
        Map<Long, ReactionSummary> result = new HashMap<>();
        if (submissionIds.isEmpty()) {
            return result;
        }
        Map<Long, Map<ReactionType, Long>> counts = new HashMap<>();
        for (Object[] row : reactionRepository.countsFor(submissionIds)) {
            counts.computeIfAbsent((Long) row[0], k -> new EnumMap<>(ReactionType.class))
                    .put((ReactionType) row[1], (Long) row[2]);
        }
        Map<Long, ReactionType> mine = new HashMap<>();
        for (AnswerReaction reaction : reactionRepository.mineFor(submissionIds, userId)) {
            mine.put(reaction.getSubmissionId(), reaction.getType());
        }
        for (Long id : submissionIds) {
            result.put(id, ReactionSummary.of(counts.getOrDefault(id, Map.of()), mine.get(id)));
        }
        return result;
    }

    private ReactionSummary summaryOf(Long userId, Long submissionId) {
        return summaries(userId, List.of(submissionId)).get(submissionId);
    }

    private DevotionSubmission checkedForMember(Long memberId, Long roomId, Long submissionId) {
        // A room the member is not in (or that is archived) looks like it does not exist.
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException("Room not found.");
        }
        DevotionSubmission submission = submissionRepository.findById(submissionId)
                .filter(s -> s.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));

        // Same rule as reading the answers: the member shared their own, is exempt that day, or the day is over.
        LocalDate today = LocalDate.now(clock);
        Devotion devotion = devotionRepository.findByIdAndRoomId(submission.getDevotionId(), roomId)
                .filter(d -> d.getStatus() == DevotionStatus.ACTIVE && !d.getDevotionDate().isAfter(today))
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));
        boolean answeredMyself = submissionRepository.existsByDevotionIdAndMemberId(devotion.getId(), memberId);
        boolean exempt = exemptionRepository.findActiveMemberIds(roomId, devotion.getDevotionDate())
                .contains(memberId);
        boolean dayIsOver = devotion.getDevotionDate().isBefore(today);
        if (!(answeredMyself || exempt || dayIsOver)) {
            throw new BusinessRuleException("Share your own understanding first, then you can react.");
        }
        return checkedAnswer(submission, memberId);
    }

    private DevotionSubmission checkedForLeader(Long leaderId, Long roomId, Long submissionId) {
        var room = roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("This room is archived.");
        }
        DevotionSubmission submission = submissionRepository.findById(submissionId)
                .filter(s -> s.getRoomId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found."));
        return checkedAnswer(submission, leaderId);
    }

    private static DevotionSubmission checkedAnswer(DevotionSubmission submission, Long userId) {
        if (submission.getMemberId().equals(userId)) {
            throw new BusinessRuleException("You cannot react to your own answer.");
        }
        if (submission.getModeratedAt() != null) {
            throw new BusinessRuleException("This answer was removed.");
        }
        return submission;
    }
}
