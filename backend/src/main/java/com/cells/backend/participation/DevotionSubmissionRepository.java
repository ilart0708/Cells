package com.cells.backend.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DevotionSubmissionRepository extends JpaRepository<DevotionSubmission, Long> {

    boolean existsByDevotionIdAndMemberId(Long devotionId, Long memberId);

    Optional<DevotionSubmission> findByDevotionIdAndMemberId(Long devotionId, Long memberId);

    long countByDevotionId(Long devotionId);

    @Query("select s.memberId from DevotionSubmission s where s.devotionId = :devotionId")
    List<Long> findMemberIdsByDevotion(@Param("devotionId") Long devotionId);

    // The answer counts of a whole page of devotions in one query (no N+1).
    @Query("select new com.cells.backend.participation.SubmissionCount(s.devotionId, count(s)) "
            + "from DevotionSubmission s where s.devotionId in :ids group by s.devotionId")
    List<SubmissionCount> countPerDevotion(@Param("ids") Collection<Long> ids);

    // Every answer of one devotion, oldest first. participationNumber is null for someone who has
    // since left the room: their answer stays in the history, but they no longer have a place in the order.
    @Query("""
            select new com.cells.backend.participation.LeaderSubmissionResponse(
                s.id, s.memberId, u.fullName, rm.participationNumber, s.content, s.submittedAt)
            from DevotionSubmission s
            join User u on u.id = s.memberId
            left join RoomMember rm
                on rm.roomId = s.roomId and rm.memberId = s.memberId and rm.removedAt is null
            where s.devotionId = :devotionId
            order by s.submittedAt, s.id
            """)
    List<LeaderSubmissionResponse> findForDevotion(@Param("devotionId") Long devotionId);
}