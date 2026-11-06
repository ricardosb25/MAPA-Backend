package com.mapa.repository;

import com.mapa.domain.ChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeProgressRepository extends JpaRepository<ChallengeProgress, Long> {

    Optional<ChallengeProgress> findByStudentIdAndSelectedIsTrue(Long studentId);

    Optional<ChallengeProgress> findByChallengeIdAndStudentId(Long challengeId, Long studentId);

    List<ChallengeProgress> findByChallengeId(Long challengeId);

    List<ChallengeProgress> findByStudentId(Long studentId);

    void deleteByChallengeId(Long challengeId);

    void deleteByChallengeSchoolClassId(Long classId);
}
