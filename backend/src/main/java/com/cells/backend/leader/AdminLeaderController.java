package com.cells.backend.leader;

import com.cells.backend.common.PageResponse;
import com.cells.backend.user.UserStatusRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/leaders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLeaderController {

    private final LeaderService leaderService;

    public AdminLeaderController(LeaderService leaderService) {
        this.leaderService = leaderService;
    }

    @GetMapping
    public PageResponse<LeaderResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return leaderService.listAll(q, page, size);
    }

    @PatchMapping("/{id}/status")
    public LeaderResponse setStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return leaderService.setStatusAsAdmin(id, request.status());
    }
}