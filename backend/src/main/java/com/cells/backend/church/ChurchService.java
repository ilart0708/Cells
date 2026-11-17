package com.cells.backend.church;

import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChurchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChurchRepository churchRepository;

    public ChurchService(ChurchRepository churchRepository) {
        this.churchRepository = churchRepository;
    }

    @Transactional
    public ChurchResponse create(ChurchRequest request) {
        String name = request.name().trim();
        if (churchRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A church with this name already exists.");
        }
        return ChurchResponse.from(churchRepository.save(new Church(name)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChurchResponse> list(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("name"));

        Page<Church> result = (query == null || query.isBlank())
                ? churchRepository.findAll(pageable)
                : churchRepository.findByNameContainingIgnoreCase(query.trim(), pageable);

        return PageResponse.from(result.map(ChurchResponse::from));
    }

    @Transactional(readOnly = true)
    public ChurchResponse get(Long id) {
        return ChurchResponse.from(find(id));
    }

    @Transactional
    public ChurchResponse rename(Long id, ChurchRequest request) {
        Church church = find(id);
        String name = request.name().trim();
        if (churchRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A church with this name already exists.");
        }
        church.rename(name);
        return ChurchResponse.from(church);
    }

    private Church find(Long id) {
        return churchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Church not found."));
    }
}