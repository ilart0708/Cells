package com.cells.backend.participation;

import com.cells.backend.participation.ParticipationRules.Participant;
import com.cells.backend.participation.ParticipationRules.Result;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomMemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Gathers the facts for one room and day, then asks ParticipationRules. Both boards use it. */
@Service
public class ParticipationBoardService {

    private final RoomMemberRepository roomMemberRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final DailyExemptionRepository exemptionRepository;

    public ParticipationBoardService(RoomMemberRepository roomMemberRepository,
                                     DevotionSubmissionRepository submissionRepository,
                                     DailyExemptionRepository exemptionRepository) {
        this.roomMemberRepository = roomMemberRepository;
        this.submissionRepository = submissionRepository;
        this.exemptionRepository = exemptionRepository;
    }

    @Transactional(readOnly = true)
    public List<Result> evaluate(Long roomId, Long devotionId, LocalDate date) {
        return evaluate(roomMemberRepository.roster(roomId), roomId, devotionId, date);
    }

    /** The same, for a caller that already loaded the roster (the leader's board also needs emails and account status). */
    @Transactional(readOnly = true)
    public List<Result> evaluate(List<RoomMemberResponse> roster, Long roomId, Long devotionId, LocalDate date) {
        List<Participant> inOrder = roster.stream()
                .map(m -> new Participant(m.memberId(), m.participationNumber(), m.fullName()))
                .toList();
        Set<Long> exempt = new HashSet<>(exemptionRepository.findActiveMemberIds(roomId, date));
        Set<Long> submitted = new HashSet<>(submissionRepository.findMemberIdsByDevotion(devotionId));
        return ParticipationRules.evaluate(inOrder, exempt, submitted);
    }
}