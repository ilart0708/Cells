package com.cells.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    @Query("select d.token from DeviceToken d where d.userId = :userId order by d.id")
    List<String> findTokensByUserId(@Param("userId") Long userId);

    /** Logging out on a phone removes only that account's own registration of the token. */
    @Transactional
    @Modifying
    @Query("delete from DeviceToken d where d.token = :token and d.userId = :userId")
    int deleteOwn(@Param("token") String token, @Param("userId") Long userId);

    /** Tokens the push service says no longer exist (app uninstalled). */
    @Transactional
    @Modifying
    @Query("delete from DeviceToken d where d.token in :tokens")
    int deleteTokens(@Param("tokens") Collection<String> tokens);
}
