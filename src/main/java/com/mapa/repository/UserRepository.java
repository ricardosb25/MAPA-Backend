package com.mapa.repository;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM User account WHERE account.id = :userId")
    Optional<User> findAccountForUpdate(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    long countByRoleAndActiveTrueAndIdNot(Role role, Long id);

    @Modifying
    @Query("UPDATE User user SET user.activeMotorId = null WHERE user.activeMotorId = :engineId")
    void clearActiveEngineReference(@Param("engineId") Long engineId);
}

