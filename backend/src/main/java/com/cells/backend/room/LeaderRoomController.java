package com.cells.backend.room;

import com.cells.backend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Everything here is scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms")
@PreAuthorize("hasRole('LEADER')")
public class LeaderRoomController {

    private final RoomService roomService;

    public LeaderRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody RoomRequest request) {
        RoomResponse created = roomService.create(leaderId(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/leader/rooms/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<RoomResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ACTIVE") RoomFilter status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomService.list(leaderId(jwt), q, status, page, size);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return roomService.get(leaderId(jwt), id);
    }

    @PutMapping("/{id}")
    public RoomResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody RoomRequest request) {
        return roomService.rename(leaderId(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    public RoomResponse setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody RoomStatusRequest request) {
        return roomService.setStatus(leaderId(jwt), id, request.status());
    }

    private static Long leaderId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}