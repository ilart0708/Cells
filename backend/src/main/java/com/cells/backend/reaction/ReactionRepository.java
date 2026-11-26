package com.cells.backend.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReactionRepository extends JpaRepository<AnswerReaction, Long> {

    // One atomic statement: a first reaction inserts, a changed mind updates. Two taps at once cannot clash.
    @Modifying
    @Query(value = """
            insert into answer_reactions (submission_id, room_id, user_id, type)
            values (:submissionId, :roomId, :userId, :type)
            on conflict (submission_id, user_id) do update set type = excluded.type
            """, nativeQuery = true)
    void upsert(@Param("submissionId") Long submissionId, @Param("roomId") Long roomId,
                @Param("userId") Long userId, @Param("type") String type);

    @Modifying
    @Query("delete from AnswerReaction r where r.submissionId = :submissionId and r.userId = :userId")
    void deleteMine(@Param("submissionId") Long submissionId, @Param("userId") Long userId);

    // Counts per answer and kind. Nobody's id is selected.
    @Query("""
            select r.submissionId, r.type, count(r)
            from AnswerReaction r
            where r.submissionId in :ids
            group by r.submissionId, r.type
            """)
    List<Object[]> countsFor(@Param("ids") Collection<Long> ids);

    @Query("select r from AnswerReaction r where r.submissionId in :ids and r.userId = :userId")
    List<AnswerReaction> mineFor(@Param("ids") Collection<Long> ids, @Param("userId") Long userId);
}
