package com.cells.backend.participation;

import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.Devotion;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionResponse;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.devotion.MemberDevotionResponse;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Reading the written answers. Only the room's leader and its members ever get here. */
@Service
public class SubmissionReadService {

    private static final String LOCKED_MESSAGE =
            "Share your own understanding first, then you can read what the others wrote.";

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final DevotionRepository devotionRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final DailyExemptionRepository exemptionRepository;
    private final Clock clock;

    public SubmissionReadService(RoomRepository roomRepository,
                                 RoomMemberRepository roomMemberRepository,
                                 DevotionRepository devotionRepository,
                                 DevotionSubmissionRepository submissionRepository,
                                 DailyExemptionRepository exemptionRepository,
                                 Clock clock) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.devotionRepository = devotionRepository;
        this.submissionRepository = submissionRepository;
        this.exemptionRepository = exemptionRepository;
        this.clock = clock;
    }

    /** The leader reads every answer of any devotion of their own room (cancelled and past ones too). */
    @Transactional(readOnly = true)
    public LeaderSubmissionsResponse forLeader(Long leaderId, Long roomId, Long devotionId) {
        roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        Devotion devotion = devotionRepository.findByIdAndRoomId(devotionId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Devotion not found."));

        List<LeaderSubmissionResponse> answers = submissionRepository.findForDevotion(devotionId);
        return new LeaderSubmissionsResponse(
                DevotionResponse.from(devotion, LocalDate.now(clock), answers.size()), answers);
    }

    /**
     * A member reads the room's answers once they have shared their own, or are exempt that day, or the
     * day is over. Until then no text is sent, only how many members have shared.
     */
    @Transactional(readOnly = true)
    public MemberSubmissionsResponse forMember(Long memberId, Long roomId, Long devotionId) {
        // A room the member is not in (or no longer in, or that is archived) looks like it does not exist.
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException("Room not found.");
        }

        // Only devotions members may see: posted, today or earlier. Anything else looks like it does not exist.
        LocalDate today = LocalDate.now(clock);
        Devotion devotion = devotionRepository.findByIdAndRoomId(devotionId, roomId)
                .filter(d -> d.getStatus() == DevotionStatus.ACTIVE && !d.getDevotionDate().isAfter(today))
                .orElseThrow(() -> new ResourceNotFoundException("Devotion not found."));
        MemberDevotionResponse devotionResponse = MemberDevotionResponse.from(devotion);

        boolean answeredMyself = submissionRepository.existsByDevotionIdAndMemberId(devotionId, memberId);
        boolean exempt = exemptionRepository.findActiveMemberIds(roomId, devotion.getDevotionDate())
                .contains(memberId);
        boolean dayIsOver = devotion.getDevotionDate().isBefore(today);

        if (!(answeredMyself || exempt || dayIsOver)) {
            int count = (int) submissionRepository.countByDevotionId(devotionId);
            return new MemberSubmissionsResponse(devotionResponse, false, LOCKED_MESSAGE, count, List.of());
        }

        List<MemberSubmissionResponse> answers = submissionRepository.findForDevotion(devotionId).stream()
                .map(a -> new MemberSubmissionResponse(
                        a.submissionId(), a.participationNumber(), a.fullName(), a.content(), a.submittedAt(),
                        a.memberId().equals(memberId), a.removedAt() != null))
                .toList();
        return new MemberSubmissionsResponse(devotionResponse, true, null, answers.size(), answers);
    }
}