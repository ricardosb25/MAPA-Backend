package com.mapa.repository;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    long countByRoleAndActiveTrueAndIdNot(Role role, Long id);

    @Modifying
    @Query("UPDATE User user SET user.motorAtivoId = null WHERE user.motorAtivoId = :engineId")
    void clearActiveEngineReference(@Param("engineId") Long engineId);
}

