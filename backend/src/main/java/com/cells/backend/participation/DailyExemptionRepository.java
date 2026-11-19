package com.cells.backend.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyExemptionRepository extends JpaRepository<DailyExemption, Long> {

    @Query("select e.memberId from DailyExemption e "
            + "where e.roomId = :roomId and e.exemptionDate = :date and e.revokedAt is null")
    List<Long> findActiveMemberIds(@Param("roomId") Long roomId, @Param("date") LocalDate date);

    Optional<DailyExemption> findByRoomIdAndMemberIdAndExemptionDateAndRevokedAtIsNull(
            Long roomId, Long memberId, LocalDate exemptionDate);
}