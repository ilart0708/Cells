package com.cells.backend.room;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.member.Member;
import com.cells.backend.member.MemberRepository;
import com.cells.backend.common.PageResponse;
import com.cells.backend.member.AvailableMemberResponse;
import org.springframework.data.domain.PageRequest;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Every method takes the leader's id and only ever touches that leader's rooms. */
@Service
public class RoomMemberService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    private static final int MAX_PAGE_SIZE = 100;

    public RoomMemberService(RoomRepository roomRepository,
                             RoomMemberRepository roomMemberRepository,
                             MemberRepository memberRepository,
                             UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomMemberResponse> roster(Long leaderId, Long roomId) {
        roomRepository.findByIdAndLeaderId(roomId, leaderId).orElseThrow(RoomMemberService::roomNotFound);
        return roomMemberRepository.roster(roomId);
    }

    /** Active members of the room's church who are not in this room yet. */
    @Transactional(readOnly = true)
    public PageResponse<AvailableMemberResponse> availableMembers(
            Long leaderId, Long roomId, String query, int page, int size) {
        Room room = roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(RoomMemberService::roomNotFound);
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(memberRepository.searchAvailable(
                room.getChurch().getId(), roomId, UserStatus.ACTIVE, pattern, pageable));
    }

    /**
     * Adds a member to the end of the room's participation order. Steps 3 and 4 call this
     * (invitation acceptance and "add an existing member").
     */
    @Transactional
    public RoomMemberResponse enroll(Long leaderId, Long roomId, Long memberId) {
        // Locking the room row makes concurrent changes to one room take turns, so two people
        // can never be handed the same number.
        Room room = roomRepository.findOwnForUpdate(roomId, leaderId)
                .orElseThrow(RoomMemberService::roomNotFound);
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("Archived rooms cannot get new members.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        if (!member.getChurch().getId().equals(room.getChurch().getId())) {
            // Another church's members look like they do not exist.
            throw new ResourceNotFoundException("Member not found.");
        }

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("This member's account is not active.");
        }

        if (roomMemberRepository.existsByRoomIdAndMemberIdAndRemovedAtIsNull(roomId, memberId)) {
            throw new DuplicateResourceException("This member is already in the room.");
        }

        int number = roomMemberRepository.maxActiveNumber(roomId) + 1;
        RoomMember saved = roomMemberRepository.save(new RoomMember(roomId, memberId, number));

        return new RoomMemberResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getStatus(),
                saved.getParticipationNumber(), saved.getJoinedAt());
    }

    // Another leader's room looks exactly like a room that does not exist.
    private static ResourceNotFoundException roomNotFound() {
        return new ResourceNotFoundException("Room not found.");
    }
}