package com.cells.backend.member;

import com.cells.backend.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    String AVAILABLE = """
            from Member m
            join User u on u.id = m.userId
            where m.church.id = :churchId
              and u.status = :status
              and (lower(u.fullName) like :pattern or lower(u.email) like :pattern)
              and not exists (
                  select 1 from RoomMember rm
                  where rm.roomId = :roomId and rm.memberId = m.userId and rm.removedAt is null)
            """;

    // Active members of one church who are not (any more) in the given room.
    // The sort is part of the query, so pass an unsorted Pageable.
    @Query(value = "select new com.cells.backend.member.AvailableMemberResponse(u.id, u.fullName, u.email) "
            + AVAILABLE + " order by lower(u.fullName), u.id",
            countQuery = "select count(m) " + AVAILABLE)
    Page<AvailableMemberResponse> searchAvailable(@Param("churchId") Long churchId,
                                                  @Param("roomId") Long roomId,
                                                  @Param("status") UserStatus status,
                                                  @Param("pattern") String pattern,
                                                  Pageable pageable);
}