package com.cells.backend.experience;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** The member's own experience. The member id always comes from the token. */
@Service
public class ExperienceService {

    private final ExperienceRecordRepository experienceRepository;

    public ExperienceService(ExperienceRecordRepository experienceRepository) {
        this.experienceRepository = experienceRepository;
    }

    @Transactional(readOnly = true)
    public ExperienceSummaryResponse summary(Long memberId) {
        List<RoomExperienceResponse> rooms = experienceRepository.byRoom(memberId);
        long total = rooms.stream().mapToLong(RoomExperienceResponse::points).sum();
        long submissions = rooms.stream().mapToLong(RoomExperienceResponse::submissions).sum();

        ExperienceLevel current = ExperienceLevel.forPoints(total);
        ExperienceLevel next = current.next();

        int pointsToNext = next == null ? 0 : (int) (next.threshold() - total);
        int progress = next == null
                ? 100
                : (int) ((total - current.threshold()) * 100 / (next.threshold() - current.threshold()));

        return new ExperienceSummaryResponse(
                (int) total, (int) submissions,
                LevelResponse.from(current), next == null ? null : LevelResponse.from(next),
                pointsToNext, progress,
                Arrays.stream(ExperienceLevel.values()).map(LevelResponse::from).toList(),
                rooms);
    }
}