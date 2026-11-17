package com.cells.backend.church;

import com.cells.backend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/churches")
@PreAuthorize("hasRole('ADMIN')")
public class ChurchController {

    private final ChurchService churchService;

    public ChurchController(ChurchService churchService) {
        this.churchService = churchService;
    }

    @PostMapping
    public ResponseEntity<ChurchResponse> create(@Valid @RequestBody ChurchRequest request) {
        ChurchResponse created = churchService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/churches/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<ChurchResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return churchService.list(q, page, size);
    }

    @GetMapping("/{id}")
    public ChurchResponse get(@PathVariable Long id) {
        return churchService.get(id);
    }

    @PutMapping("/{id}")
    public ChurchResponse rename(@PathVariable Long id, @Valid @RequestBody ChurchRequest request) {
        return churchService.rename(id, request);
    }
}