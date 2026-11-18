package com.cells.backend.room;

import com.cells.backend.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The member's own view. The member id always comes from the token. */
@Service
public class MemberRoomService {

    private final RoomMemberRepository roomMemberRepository;

    public MemberRoomService(RoomMemberRepository roomMemberRepository) {
        this.roomMemberRepository = roomMemberRepository;
    }

    /** Active rooms the member currently belongs to. Not paginated: a member has a handful at most. */
    @Transactional(readOnly = true)
    public List<MemberRoomResponse> myRooms(Long memberId) {
        return roomMemberRepository.findRoomsOf(memberId, RoomStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public MemberRoomDetailResponse detail(Long memberId, Long roomId) {
        // A room the member is not in (or no longer in) looks exactly like a room that does not exist.
        MemberRoomResponse room = roomMemberRepository.findRoomOf(memberId, roomId, RoomStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        // The roster also carries emails; fellow members only get names and numbers.
        List<ParticipationEntryResponse> order = roomMemberRepository.roster(roomId).stream()
                .map(m -> new ParticipationEntryResponse(
                        m.participationNumber(), m.fullName(), m.memberId().equals(memberId)))
                .toList();

        return new MemberRoomDetailResponse(room, order);
    }
}