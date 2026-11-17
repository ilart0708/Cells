package com.cells.backend.pastor;

import com.cells.backend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/pastors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPastorController {

    private final PastorService pastorService;

    public AdminPastorController(PastorService pastorService) {
        this.pastorService = pastorService;
    }

    @GetMapping
    public PageResponse<PastorResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return pastorService.list(q, page, size);
    }

    @GetMapping("/{id}")
    public PastorResponse get(@PathVariable Long id) {
        return pastorService.get(id);
    }

    @PatchMapping("/{id}/status")
    public PastorResponse setStatus(@PathVariable Long id, @Valid @RequestBody PastorStatusRequest request) {
        return pastorService.setStatus(id, request.status());
    }
}