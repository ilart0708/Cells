package com.cells.backend.devotion;

import com.cells.backend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/devotions")
@PreAuthorize("hasRole('LEADER')")
public class LeaderDevotionController {

    private final DevotionService devotionService;

    public LeaderDevotionController(DevotionService devotionService) {
        this.devotionService = devotionService;
    }

    @PostMapping
    public ResponseEntity<DevotionResponse> create(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @Valid @RequestBody DevotionRequest request) {
        DevotionResponse created = devotionService.create(leaderId(jwt), roomId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/leader/rooms/" + roomId + "/devotions/" + created.id()))
                .body(created);
    }

    /** The room's devotions, newest day first. status = ACTIVE (default) | CANCELLED | ALL. */
    @GetMapping
    public PageResponse<DevotionResponse> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @RequestParam(defaultValue = "ACTIVE") DevotionFilter status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return devotionService.list(leaderId(jwt), roomId, status, page, size);
    }

    @GetMapping("/{id}")
    public DevotionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id) {
        return devotionService.get(leaderId(jwt), roomId, id);
    }

    @PutMapping("/{id}")
    public DevotionResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id,
            @Valid @RequestBody DevotionRequest request) {
        return devotionService.update(leaderId(jwt), roomId, id, request);
    }

    /** Cancel (CANCELLED) or restore (ACTIVE). Devotions are never deleted. */
    @PatchMapping("/{id}/status")
    public DevotionResponse setStatus(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id,
            @Valid @RequestBody DevotionStatusRequest request) {
        return devotionService.setStatus(leaderId(jwt), roomId, id, request.status());
    }

    private static Long leaderId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}