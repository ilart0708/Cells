package com.cells.backend.notification;

import com.cells.backend.devotion.Devotion;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.participation.ParticipationBoardService;
import com.cells.backend.participation.ParticipationStatus;
import com.cells.backend.participation.ParticipationRules.Result;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomMemberResponse;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import com.cells.backend.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides WHO is told about WHAT, and queues it. Every method joins the caller's transaction, so the
 * notification exists if and only if the action that caused it was saved. Sending happens after the commit
 * (see NotificationDispatcher). Asking twice for the same event is harmless: the table's unique key stores it once.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DevotionRepository devotionRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ParticipationBoardService boardService;
    private final NotificationDispatcher dispatcher;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository,
                               DevotionRepository devotionRepository,
                               RoomRepository roomRepository,
                               RoomMemberRepository roomMemberRepository,
                               ParticipationBoardService boardService,
                               NotificationDispatcher dispatcher,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.devotionRepository = devotionRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.boardService = boardService;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    /**
     * Today's devotion is open. Members who can submit right now are told it is their turn; everyone else
     * who has to wait is told the devotion is open. Does nothing if the devotion is not today's active one,
     * so it is safe to call from "created", "moved to today", "restored" and from the midnight job.
     */
    @Transactional
    public void devotionOpened(Long devotionId) {
        LocalDate today = LocalDate.now(clock);
        Devotion devotion = devotionRepository.findById(devotionId).orElse(null);
        if (devotion == null || devotion.getStatus() != DevotionStatus.ACTIVE
                || !devotion.getDevotionDate().equals(today)) {
            return;
        }
        // The same room lock a submission takes, so the roster and the turn order cannot change under us.
        Room room = roomRepository.findByIdForUpdate(devotion.getRoomId()).orElse(null);
        if (room == null || room.getStatus() != RoomStatus.ACTIVE) {
            return;
        }

        List<RoomMemberResponse> roster = roomMemberRepository.roster(room.getId());
        Set<Long> activeMembers = activeMemberIds(roster);
        Instant now = clock.instant();
        int queued = 0;
        for (Result result : boardService.evaluate(roster, room.getId(), devotionId, today)) {
            Long memberId = result.participant().memberId();
            if (!activeMembers.contains(memberId)) {
                continue;
            }
            switch (result.status()) {
                case ELIGIBLE -> queued += enqueue(memberId, NotificationType.YOUR_TURN, devotion, now);
                case WAITING -> queued += enqueue(memberId, NotificationType.DEVOTION_OPEN, devotion, now);
                default -> {
                    // already submitted or exempt: nothing to tell them
                }
            }
        }
        dispatchAfterCommit(queued);
    }

    /** Someone submitted, or an exemption changed: tell every member who can submit now. */
    @Transactional
    public void turnsOpened(Long roomId, Long devotionId) {
        LocalDate today = LocalDate.now(clock);
        Devotion devotion = devotionRepository.findById(devotionId).orElse(null);
        if (devotion == null || devotion.getStatus() != DevotionStatus.ACTIVE
                || !devotion.getDevotionDate().equals(today) || !devotion.getRoomId().equals(roomId)) {
            return;
        }

        List<RoomMemberResponse> roster = roomMemberRepository.roster(roomId);
        Set<Long> activeMembers = activeMemberIds(roster);
        Instant now = clock.instant();
        int queued = 0;
        for (Result result : boardService.evaluate(roster, roomId, devotionId, today)) {
            Long memberId = result.participant().memberId();
            if (activeMembers.contains(memberId) && result.status() == ParticipationStatus.ELIGIBLE) {
                queued += enqueue(memberId, NotificationType.YOUR_TURN, devotion, now);
            }
        }
        dispatchAfterCommit(queued);
    }

    /** The same, for a leader action (exempt / un-exempt) that has no devotion id at hand. */
    @Transactional
    public void turnsOpenedToday(Long roomId) {
        devotionRepository.findByRoomIdAndDevotionDateAndStatus(roomId, LocalDate.now(clock), DevotionStatus.ACTIVE)
                .ifPresent(devotion -> turnsOpened(roomId, devotion.getId()));
    }

    /** The leader saved a summary or reflection for the first time: tell the room's members. */
    @Transactional
    public void guidancePosted(Long roomId, Long devotionId, NotificationType type) {
        Devotion devotion = devotionRepository.findByIdAndRoomId(devotionId, roomId).orElse(null);
        if (devotion == null) {
            return;
        }
        List<RoomMemberResponse> roster = roomMemberRepository.roster(roomId);
        Instant now = clock.instant();
        int queued = 0;
        for (RoomMemberResponse member : roster) {
            if (member.accountStatus() == UserStatus.ACTIVE) {
                queued += enqueue(member.memberId(), type, devotion, now);
            }
        }
        dispatchAfterCommit(queued);
    }

    /**
     * Someone reacted to a member's shared answer: tell the author, by phone push only. One notification per
     * answer (the table's unique key), so a second reaction, a changed reaction or a removed-and-added reaction
     * never sends another one. It never says who reacted.
     */
    @Transactional
    public void answerReacted(Long roomId, Long devotionId, Long authorId) {
        Devotion devotion = devotionRepository.findByIdAndRoomId(devotionId, roomId).orElse(null);
        if (devotion == null) {
            return;
        }
        for (RoomMemberResponse member : roomMemberRepository.roster(roomId)) {
            if (member.memberId().equals(authorId) && member.accountStatus() == UserStatus.ACTIVE) {
                dispatchAfterCommit(enqueue(authorId, NotificationType.ANSWER_REACTED, devotion, clock.instant()));
                return;
            }
        }
    }

    // ---------- helpers ----------

    private int enqueue(Long memberId, NotificationType type, Devotion devotion, Instant now) {
        // cycle_date is the devotion's own date: editing a note on another day never notifies twice.
        return notificationRepository.enqueue(
                memberId, type.name(), devotion.getRoomId(), devotion.getId(), devotion.getDevotionDate(), now);
    }

    private static Set<Long> activeMemberIds(List<RoomMemberResponse> roster) {
        Set<Long> ids = new HashSet<>();
        for (RoomMemberResponse member : roster) {
            if (member.accountStatus() == UserStatus.ACTIVE) {
                ids.add(member.memberId());
            }
        }
        return ids;
    }

    /** Sends only if the surrounding transaction commits; a rolled-back action notifies nobody. */
    private void dispatchAfterCommit(int queued) {
        if (queued == 0) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatchAsync();
                }
            });
        } else {
            dispatcher.dispatchAsync();
        }
    }
}
