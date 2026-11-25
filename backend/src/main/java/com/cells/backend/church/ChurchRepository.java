package com.cells.backend.church;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChurchRepository extends JpaRepository<Church, Long> {

    // Only administrator-created churches have unique names (see migration V23).
    boolean existsByNameIgnoreCaseAndSelfRegisteredFalse(String name);

    boolean existsByNameIgnoreCaseAndSelfRegisteredFalseAndIdNot(String name, Long id);

    Page<Church> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Church> findByNameIgnoreCaseAndSelfRegisteredFalse(String name);
}