package com.cells.backend.room;

import com.cells.backend.common.PageResponse;
import com.cells.backend.member.AvailableMemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/members")
@PreAuthorize("hasRole('LEADER')")
public class LeaderRoomMemberController {

    private final RoomMemberService roomMemberService;

    public LeaderRoomMemberController(RoomMemberService roomMemberService) {
        this.roomMemberService = roomMemberService;
    }

    /** The whole room, in participation order. Not paginated: order management needs the full sequence. */
    @GetMapping
    public List<RoomMemberResponse> roster(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return roomMemberService.roster(leaderId(jwt), roomId);
    }

    /** Members of the room's church who are not in this room: the "people I can add" list. */
    @GetMapping("/available")
    public PageResponse<AvailableMemberResponse> available(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomMemberService.availableMembers(leaderId(jwt), roomId, q, page, size);
    }

    /** Adds an existing member of the church to the end of the participation order. */
    @PostMapping
    public ResponseEntity<RoomMemberResponse> add(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @Valid @RequestBody AddMemberRequest request) {
        RoomMemberResponse added = roomMemberService.enroll(leaderId(jwt), roomId, request.memberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    private static Long leaderId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}