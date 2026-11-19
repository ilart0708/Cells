package com.cells.backend.participation;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.devotion.Devotion;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.devotion.MemberDevotionResponse;
import com.cells.backend.participation.ParticipationRules.Result;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomMemberResponse;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** The member's own view of participation. The member id always comes from the token. */
@Service
public class MemberParticipationService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final DevotionRepository devotionRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final ParticipationBoardService boardService;
    private final Clock clock;

    public MemberParticipationService(RoomRepository roomRepository,
                                      RoomMemberRepository roomMemberRepository,
                                      DevotionRepository devotionRepository,
                                      DevotionSubmissionRepository submissionRepository,
                                      ParticipationBoardService boardService,
                                      Clock clock) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.devotionRepository = devotionRepository;
        this.submissionRepository = submissionRepository;
        this.boardService = boardService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MemberBoardResponse board(Long memberId, Long roomId) {
        // A room the member is not in (or no longer in, or that is archived) looks like it does not exist.
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw roomNotFound();
        }

        LocalDate today = LocalDate.now(clock);
        Devotion devotion = devotionRepository
                .findByRoomIdAndDevotionDateAndStatus(roomId, today, DevotionStatus.ACTIVE)
                .orElse(null);

        if (devotion == null) {
            List<RoomMemberResponse> roster = roomMemberRepository.roster(roomId);
            Integer mine = roster.stream()
                    .filter(m -> m.memberId().equals(memberId))
                    .map(RoomMemberResponse::participationNumber)
                    .findFirst().orElse(null);
            List<BoardEntryResponse> order = roster.stream()
                    .map(m -> new BoardEntryResponse(
                            m.participationNumber(), m.fullName(), m.memberId().equals(memberId), null))
                    .toList();
            return new MemberBoardResponse(today, null, mine, null, null, null, order);
        }

        List<Result> results = boardService.evaluate(roomId, devotion.getId(), today);

        Result mine = results.stream()
                .filter(r -> r.participant().memberId().equals(memberId))
                .findFirst().orElse(null);
        WaitingForResponse waitingFor = (mine != null && mine.waitingFor() != null)
                ? new WaitingForResponse(mine.waitingFor().number(), mine.waitingFor().fullName())
                : null;
        SubmissionResponse mySubmission = submissionRepository
                .findByDevotionIdAndMemberId(devotion.getId(), memberId)
                .map(SubmissionResponse::from)
                .orElse(null);
        List<BoardEntryResponse> order = results.stream()
                .map(r -> new BoardEntryResponse(
                        r.participant().number(), r.participant().fullName(),
                        r.participant().memberId().equals(memberId), r.status()))
                .toList();

        return new MemberBoardResponse(
                today, MemberDevotionResponse.from(devotion),
                mine == null ? null : mine.participant().number(),
                mine == null ? null : mine.status(),
                waitingFor, mySubmission, order);
    }

    @Transactional
    public SubmissionResponse submit(Long memberId, Long roomId, Long devotionId, String rawContent) {
        // 1. Cheap check first, so strangers never reach the lock.
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw roomNotFound();
        }

        // 2. The room lock: submitting, reordering, adding and removing in one room take turns. Everything
        //    below is read AFTER the lock is held, so it is what is true at this moment.
        Room room = roomRepository.findByIdForUpdate(roomId).orElseThrow(MemberParticipationService::roomNotFound);
        if (room.getStatus() != RoomStatus.ACTIVE
                || !roomMemberRepository.existsByRoomIdAndMemberIdAndRemovedAtIsNull(roomId, memberId)) {
            throw roomNotFound();
        }

        // 3. The devotion must be one of this room's, posted, and not in the leader's preparation.
        LocalDate today = LocalDate.now(clock);
        Devotion devotion = devotionRepository.findByIdAndRoomId(devotionId, roomId)
                .filter(d -> d.getStatus() == DevotionStatus.ACTIVE && !d.getDevotionDate().isAfter(today))
                .orElseThrow(() -> new ResourceNotFoundException("Devotion not found."));
        if (devotion.getDevotionDate().isBefore(today)) {
            throw new BusinessRuleException(
                    "That day has passed, so this devotion is closed for new answers.");
        }

        // 4. One answer per devotion.
        if (submissionRepository.existsByDevotionIdAndMemberId(devotionId, memberId)) {
            throw new DuplicateResourceException("You have already submitted your understanding for this devotion.");
        }

        // 5. The sequence rule, evaluated on the order as it is right now.
        Result mine = boardService.evaluate(roomId, devotionId, today).stream()
                .filter(r -> r.participant().memberId().equals(memberId))
                .findFirst()
                .orElseThrow(MemberParticipationService::roomNotFound);

        switch (mine.status()) {
            case EXEMPTED -> throw new BusinessRuleException(
                    "You are exempt from today\u2019s devotion, so you can\u2019t submit. "
                            + "If this is a mistake, ask your leader to remove the exemption.");
            case WAITING -> throw new BusinessRuleException(
                    "It isn\u2019t your turn yet. Please wait for " + mine.waitingFor().fullName()
                            + " (No. " + mine.waitingFor().number() + ") to submit first.");
            case SUBMITTED -> throw new DuplicateResourceException(
                    "You have already submitted your understanding for this devotion.");
            case ELIGIBLE -> {
                // it is this member's turn: fall through to saving
            }
        }

        DevotionSubmission saved = submissionRepository.save(
                new DevotionSubmission(devotionId, roomId, memberId, rawContent.strip()));
        return SubmissionResponse.from(saved);
    }

    private static ResourceNotFoundException roomNotFound() {
        return new ResourceNotFoundException("Room not found.");
    }
}