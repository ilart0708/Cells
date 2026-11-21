package com.cells.backend.summary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevotionSummaryRepository extends JpaRepository<DevotionSummary, Long> {

    List<DevotionSummary> findByDevotionId(Long devotionId);

    Optional<DevotionSummary> findByDevotionIdAndType(Long devotionId, SummaryType type);
}