package com.cells.backend.pastor;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PastorService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PastorRepository pastorRepository;
    private final UserRepository userRepository;

    public PastorService(PastorRepository pastorRepository, UserRepository userRepository) {
        this.pastorRepository = pastorRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PastorResponse> list(String query, int page, int size) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(pastorRepository.search(pattern, pageable));
    }

    @Transactional(readOnly = true)
    public PastorResponse get(Long id) {
        return pastorRepository.findResponseById(id).orElseThrow(PastorService::notFound);
    }

    @Transactional
    public PastorResponse setStatus(Long id, UserStatus status) {
        if (status == UserStatus.PENDING) {
            throw new BusinessRuleException("Status must be ACTIVE or DISABLED.");
        }
        if (!pastorRepository.existsById(id)) {
            throw notFound();
        }
        User user = userRepository.findById(id).orElseThrow(PastorService::notFound);
        if (status == UserStatus.ACTIVE) {
            user.activate();
        } else {
            user.disable();
        }
        return pastorRepository.findResponseById(id).orElseThrow(PastorService::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Pastor not found.");
    }
}