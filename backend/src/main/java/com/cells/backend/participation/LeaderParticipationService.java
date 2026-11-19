package com.cells.backend.participation;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.Devotion;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionResponse;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.participation.ParticipationRules.Result;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomMemberResponse;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The leader's view of today, and the "Exempt for Today" control. The leader id always comes from the token. */
@Service
public class LeaderParticipationService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final DevotionRepository devotionRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final DailyExemptionRepository exemptionRepository;
    private final ParticipationBoardService boardService;
    private final UserRepository userRepository;
    private final Clock clock;

    public LeaderParticipationService(RoomRepository roomRepository,
                                      RoomMemberRepository roomMemberRepository,
                                      DevotionRepository devotionRepository,
                                      DevotionSubmissionRepository submissionRepository,
                                      DailyExemptionRepository exemptionRepository,
                                      ParticipationBoardService boardService,
                                      UserRepository userRepository,
                                      Clock clock) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.devotionRepository = devotionRepository;
        this.submissionRepository = submissionRepository;
        this.exemptionRepository = exemptionRepository;
        this.boardService = boardService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LeaderBoardResponse board(Long leaderId, Long roomId) {
        // Another leader's room looks exactly like a room that does not exist.
        Room room = roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(LeaderParticipationService::roomNotFound);
        LocalDate today = LocalDate.now(clock);
        boolean open = room.getStatus() == RoomStatus.ACTIVE;

        List<RoomMemberResponse> roster = roomMemberRepository.roster(roomId);
        Devotion devotion = devotionRepository
                .findByRoomIdAndDevotionDateAndStatus(roomId, today, DevotionStatus.ACTIVE)
                .orElse(null);
        Set<Long> exempt = new HashSet<>(exemptionRepository.findActiveMemberIds(roomId, today));

        Map<Long, Result> results = new HashMap<>();
        if (devotion != null) {
            for (Result result : boardService.evaluate(roster, roomId, devotion.getId(), today)) {
                results.put(result.participant().memberId(), result);
            }
        }

        List<LeaderBoardEntryResponse> order = new ArrayList<>(roster.size());
        int submitted = 0;
        int exempted = 0;
        for (RoomMemberResponse member : roster) {
            Result result = results.get(member.memberId());
            ParticipationStatus status = result == null ? null : result.status();
            boolean isExempt = exempt.contains(member.memberId()) && status != ParticipationStatus.SUBMITTED;
            WaitingForResponse waitingFor = (result != null && result.waitingFor() != null)
                    ? new WaitingForResponse(result.waitingFor().number(), result.waitingFor().fullName())
                    : null;

            boolean canExempt = open && !isExempt && status != ParticipationStatus.SUBMITTED;
            boolean blocking = status == ParticipationStatus.ELIGIBLE && member.accountStatus() != UserStatus.ACTIVE;

            if (status == ParticipationStatus.SUBMITTED) submitted++;
            if (isExempt) exempted++;

            order.add(new LeaderBoardEntryResponse(
                    member.memberId(), member.participationNumber(), member.fullName(), member.email(),
                    member.accountStatus(), status, isExempt, canExempt, blocking, waitingFor));
        }

        DevotionResponse devotionResponse = devotion == null ? null
                : DevotionResponse.from(devotion, today, (int) submissionRepository.countByDevotionId(devotion.getId()));
        return new LeaderBoardResponse(today, devotionResponse, order.size(), submitted, exempted, order);
    }

    /** Exempts a member for today. Granting twice is harmless and returns the existing exemption. */
    @Transactional
    public ExemptionResponse exempt(Long leaderId, Long roomId, Long memberId) {
        // The same room lock a member's submission takes: whichever comes first wins, cleanly.
        lockOpenRoom(leaderId, roomId);
        requireInRoom(roomId, memberId);
        LocalDate today = LocalDate.now(clock);

        DailyExemption existing = exemptionRepository
                .findByRoomIdAndMemberIdAndExemptionDateAndRevokedAtIsNull(roomId, memberId, today)
                .orElse(null);
        if (existing != null) {
            return ExemptionResponse.from(existing);
        }

        boolean alreadySubmitted = devotionRepository
                .findByRoomIdAndDevotionDateAndStatus(roomId, today, DevotionStatus.ACTIVE)
                .map(devotion -> submissionRepository.existsByDevotionIdAndMemberId(devotion.getId(), memberId))
                .orElse(false);
        if (alreadySubmitted) {
            String name = userRepository.findById(memberId).map(User::getFullName).orElse("This member");
            throw new BusinessRuleException(
                    name + " has already submitted today\u2019s devotion, so they can\u2019t be exempted.");
        }

        return ExemptionResponse.from(
                exemptionRepository.save(new DailyExemption(roomId, memberId, today, leaderId)));
    }

    /** Removes today's exemption. Removing one that does not exist is harmless. The row stays, as history. */
    @Transactional
    public void revoke(Long leaderId, Long roomId, Long memberId) {
        lockOpenRoom(leaderId, roomId);
        requireInRoom(roomId, memberId);
        LocalDate today = LocalDate.now(clock);

        exemptionRepository
                .findByRoomIdAndMemberIdAndExemptionDateAndRevokedAtIsNull(roomId, memberId, today)
                .ifPresent(exemption -> exemption.revoke(leaderId, Instant.now()));
    }

    // ---------- lookups ----------

    private Room lockOpenRoom(Long leaderId, Long roomId) {
        Room room = roomRepository.findOwnForUpdate(roomId, leaderId)
                .orElseThrow(LeaderParticipationService::roomNotFound);
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("Archived rooms cannot be changed.");
        }
        return room;
    }

    private void requireInRoom(Long roomId, Long memberId) {
        if (!roomMemberRepository.existsByRoomIdAndMemberIdAndRemovedAtIsNull(roomId, memberId)) {
            throw new ResourceNotFoundException("Member not found in this room.");
        }
    }

    private static ResourceNotFoundException roomNotFound() {
        return new ResourceNotFoundException("Room not found.");
    }
}