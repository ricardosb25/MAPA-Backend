package com.mapa.repository;

import com.mapa.domain.ChallengeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ChallengeAttemptRepository extends JpaRepository<ChallengeAttempt, Long> {

    List<ChallengeAttempt> findByChallengeIdOrderByCreatedAtDesc(Long challengeId);

    List<ChallengeAttempt> findByChallengeIdAndStudentIdOrderByCreatedAtDesc(Long challengeId, Long studentId);

    List<ChallengeAttempt> findByChallengeIdIn(Collection<Long> challengeIds);

    void deleteByChallengeId(Long challengeId);
}