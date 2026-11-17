package com.cells.backend.leader;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PastorLeaderRepository extends JpaRepository<PastorLeader, Long> {

    boolean existsByLeaderIdAndPastorId(Long leaderId, Long pastorId);
}