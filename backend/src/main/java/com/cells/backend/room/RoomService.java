package com.cells.backend.room;

import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchStatus;
import com.cells.backend.church.UnverifiedLimits;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.leader.CellGroupLeader;
import com.cells.backend.leader.CellGroupLeaderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Every method takes the leader's id from the token and only ever touches that leader's rooms. */
@Service
public class RoomService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RoomRepository roomRepository;
    private final CellGroupLeaderRepository leaderRepository;

    public RoomService(RoomRepository roomRepository, CellGroupLeaderRepository leaderRepository) {
        this.roomRepository = roomRepository;
        this.leaderRepository = leaderRepository;
    }

    @Transactional
    public RoomResponse create(Long leaderId, RoomRequest request) {
        CellGroupLeader leader = leaderRepository.findById(leaderId)
                .orElseThrow(() -> new BusinessRuleException("Your account is not linked to a church."));
        Church church = leader.getChurch();
        if (church.getStatus() != ChurchStatus.ACTIVE) {
            throw new BusinessRuleException("Your church is not active.");
        }

        if (!church.isVerified()
                && roomRepository.countByLeaderIdAndStatus(leaderId, RoomStatus.ACTIVE) >= UnverifiedLimits.MAX_ROOMS) {
            throw new BusinessRuleException(UnverifiedLimits.roomsMessage());
        }

        String name = request.name().trim();
        if (roomRepository.existsByLeaderIdAndStatusAndNameIgnoreCase(leaderId, RoomStatus.ACTIVE, name)) {
            throw new DuplicateResourceException("You already have a room with this name.");
        }

        Room room = roomRepository.save(new Room(church, leaderId, name));
        return get(leaderId, room.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(Long leaderId, String query, RoomFilter filter, int page, int size) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(roomRepository.searchOwn(leaderId, filter.statuses(), pattern, pageable));
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long leaderId, Long roomId) {
        return roomRepository.findOwnResponse(roomId, leaderId).orElseThrow(RoomService::notFound);
    }

    @Transactional
    public RoomResponse rename(Long leaderId, Long roomId, RoomRequest request) {
        Room room = findOwn(leaderId, roomId);
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("Restore the room before renaming it.");
        }

        String name = request.name().trim();
        if (roomRepository.existsByLeaderIdAndStatusAndNameIgnoreCaseAndIdNot(
                leaderId, RoomStatus.ACTIVE, name, roomId)) {
            throw new DuplicateResourceException("You already have a room with this name.");
        }

        room.rename(name);
        return get(leaderId, roomId);
    }

    @Transactional
    public RoomResponse setStatus(Long leaderId, Long roomId, RoomStatus status) {
        Room room = findOwn(leaderId, roomId);

        if (status == RoomStatus.ARCHIVED) {
            room.archive(Instant.now());
        } else if (room.getStatus() != RoomStatus.ACTIVE) {
            if (roomRepository.existsByLeaderIdAndStatusAndNameIgnoreCaseAndIdNot(
                    leaderId, RoomStatus.ACTIVE, room.getName(), roomId)) {
                throw new DuplicateResourceException(
                        "Another active room already has this name. Rename that room first, then restore this one.");
            }
            if (!room.getChurch().isVerified()
                    && roomRepository.countByLeaderIdAndStatus(leaderId, RoomStatus.ACTIVE) >= UnverifiedLimits.MAX_ROOMS) {
                throw new BusinessRuleException(UnverifiedLimits.roomsMessage());
            }
            room.restore();
        }
        return get(leaderId, roomId);
    }

    // Another leader's room looks exactly like a room that does not exist.
    private Room findOwn(Long leaderId, Long roomId) {
        return roomRepository.findByIdAndLeaderId(roomId, leaderId).orElseThrow(RoomService::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Room not found.");
    }
}