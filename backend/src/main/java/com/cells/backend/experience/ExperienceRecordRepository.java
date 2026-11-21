package com.cells.backend.experience;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExperienceRecordRepository extends JpaRepository<ExperienceRecord, Long> {

    boolean existsBySubmissionId(Long submissionId);

    // EXP per room in one query (no N+1). Rooms the member has left or that were archived still count:
    // the EXP belongs to the member.
    @Query("""
            select new com.cells.backend.experience.RoomExperienceResponse(
                r.id, r.name, c.name, sum(e.points), count(e))
            from ExperienceRecord e
            join DevotionSubmission s on s.id = e.submissionId
            join Room r on r.id = s.roomId
            join r.church c
            where e.memberId = :memberId
            group by r.id, r.name, c.name
            order by lower(c.name), lower(r.name), r.id
            """)
    List<RoomExperienceResponse> byRoom(@Param("memberId") Long memberId);
}