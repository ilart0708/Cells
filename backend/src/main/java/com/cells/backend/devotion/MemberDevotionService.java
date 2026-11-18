package com.cells.backend.devotion;

import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** The member's own view. The member id always comes from the token. */
@Service
public class MemberDevotionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DevotionRepository devotionRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Clock clock;

    public MemberDevotionService(DevotionRepository devotionRepository,
                                 RoomMemberRepository roomMemberRepository,
                                 Clock clock) {
        this.devotionRepository = devotionRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TodayDevotionResponse today(Long memberId, Long roomId) {
        requireMembership(memberId, roomId);
        LocalDate today = LocalDate.now(clock);
        MemberDevotionResponse devotion = devotionRepository
                .findByRoomIdAndDevotionDateAndStatus(roomId, today, DevotionStatus.ACTIVE)
                .map(MemberDevotionResponse::from)
                .orElse(null);
        return new TodayDevotionResponse(today, devotion);
    }

    /** Today's and earlier devotions, newest first. Future ones are the leader's preparation and stay hidden. */
    @Transactional(readOnly = true)
    public PageResponse<MemberDevotionResponse> list(Long memberId, Long roomId, int page, int size) {
        requireMembership(memberId, roomId);
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "devotionDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(devotionRepository
                .findByRoomIdAndStatusAndDevotionDateLessThanEqual(
                        roomId, DevotionStatus.ACTIVE, LocalDate.now(clock), pageable)
                .map(MemberDevotionResponse::from));
    }

    @Transactional(readOnly = true)
    public MemberDevotionResponse get(Long memberId, Long roomId, Long devotionId) {
        requireMembership(memberId, roomId);
        LocalDate today = LocalDate.now(clock);
        return devotionRepository.findByIdAndRoomId(devotionId, roomId)
                .filter(d -> d.getStatus() == DevotionStatus.ACTIVE && !d.getDevotionDate().isAfter(today))
                .map(MemberDevotionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Devotion not found."));
    }

    // A room the member is not in (or no longer in, or that is archived) looks like it does not exist.
    private void requireMembership(Long memberId, Long roomId) {
        if (roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException("Room not found.");
        }
    }
}