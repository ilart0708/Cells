package com.cells.backend.room;

import com.cells.backend.church.UnverifiedLimits;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.member.AvailableMemberResponse;
import com.cells.backend.member.Member;
import com.cells.backend.member.MemberRepository;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Every method takes the leader's id and only ever touches that leader's rooms. */
@Service
public class RoomMemberService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

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

    /** Adds a member to the end of the room's participation order. */
    @Transactional
    public RoomMemberResponse enroll(Long leaderId, Long roomId, Long memberId) {
        Room room = lockOpenRoom(leaderId, roomId);

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

        if (!room.getChurch().isVerified()
                && roomMemberRepository.countActiveMembersOfChurch(room.getChurch().getId()) >= UnverifiedLimits.MAX_MEMBERS) {
            throw new BusinessRuleException(UnverifiedLimits.membersMessage());
        }

        int number = roomMemberRepository.maxActiveNumber(roomId) + 1;
        RoomMember saved = roomMemberRepository.save(new RoomMember(roomId, memberId, number));

        return new RoomMemberResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getStatus(),
                saved.getParticipationNumber(), saved.getJoinedAt());
    }

    /** Puts the members in the given order. The list must contain every current member exactly once. */
    @Transactional
    public List<RoomMemberResponse> reorder(Long leaderId, Long roomId, List<Long> memberIds) {
        lockOpenRoom(leaderId, roomId);

        List<RoomMember> active =
                roomMemberRepository.findByRoomIdAndRemovedAtIsNullOrderByParticipationNumber(roomId);
        Map<Long, Long> membershipByMember = new HashMap<>();
        active.forEach(rm -> membershipByMember.put(rm.getMemberId(), rm.getId()));

        // Same size and same set means: no duplicates, nobody missing, nobody extra.
        Set<Long> requested = new HashSet<>(memberIds);
        if (requested.size() != memberIds.size() || !requested.equals(membershipByMember.keySet())) {
            throw new BusinessRuleException(
                    "The list must contain every current member of the room exactly once. "
                            + "Reload the list and try again.");
        }

        List<Long> currentOrder = active.stream().map(RoomMember::getMemberId).toList();
        if (!currentOrder.equals(memberIds)) {
            assignNumbers(roomId, memberIds.stream().map(membershipByMember::get).toList());
        }
        return roomMemberRepository.roster(roomId);
    }

    /** Closes the membership (history is kept) and closes the gap in the numbering. */
    @Transactional
    public void remove(Long leaderId, Long roomId, Long memberId) {
        lockOpenRoom(leaderId, roomId);

        RoomMember membership = roomMemberRepository
                .findByRoomIdAndMemberIdAndRemovedAtIsNull(roomId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this room."));
        membership.markRemoved(Instant.now());
        roomMemberRepository.flush();

        List<Long> remaining = roomMemberRepository
                .findByRoomIdAndRemovedAtIsNullOrderByParticipationNumber(roomId)
                .stream().map(RoomMember::getId).toList();
        assignNumbers(roomId, remaining);
    }

    /**
     * Account deletion: the member leaves every room they are in (history is kept, the gap in the numbering is
     * closed) so nobody is left waiting for them. Rooms are locked in id order, so two deletions cannot deadlock.
     */
    @Transactional
    public void leaveAllRooms(Long memberId) {
        List<Long> roomIds = roomMemberRepository.findByMemberIdAndRemovedAtIsNull(memberId).stream()
                .map(RoomMember::getRoomId).sorted().toList();
        for (Long roomId : roomIds) {
            Room room = roomRepository.findByIdForUpdate(roomId).orElse(null);
            if (room == null) {
                continue;
            }
            roomMemberRepository.findByRoomIdAndMemberIdAndRemovedAtIsNull(roomId, memberId).ifPresent(membership -> {
                membership.markRemoved(Instant.now());
                roomMemberRepository.flush();
            });
            if (room.getStatus() == RoomStatus.ACTIVE) {
                assignNumbers(roomId, roomMemberRepository
                        .findByRoomIdAndRemovedAtIsNullOrderByParticipationNumber(roomId)
                        .stream().map(RoomMember::getId).toList());
            }
        }
    }

    // Locks the room row, so changes to one room's roster take turns. Another leader's room looks
    // exactly like a room that does not exist.
    private Room lockOpenRoom(Long leaderId, Long roomId) {
        Room room = roomRepository.findOwnForUpdate(roomId, leaderId)
                .orElseThrow(RoomMemberService::roomNotFound);
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("Archived rooms cannot be changed.");
        }
        return room;
    }

    /** Gives the memberships the numbers 1..n in the given order, in two steps (see the step notes). */
    private void assignNumbers(Long roomId, List<Long> membershipIdsInOrder) {
        int offset = roomMemberRepository.maxActiveNumber(roomId);
        if (offset == 0) {
            return;
        }
        roomMemberRepository.shiftActiveNumbers(roomId, offset);

        int number = 1;
        for (Long membershipId : membershipIdsInOrder) {
            roomMemberRepository.setNumber(membershipId, number++);
        }
    }

    private static ResourceNotFoundException roomNotFound() {
        return new ResourceNotFoundException("Room not found.");
    }
}