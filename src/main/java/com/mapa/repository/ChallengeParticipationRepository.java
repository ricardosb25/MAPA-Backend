package com.mapa.repository;

import com.mapa.domain.ChallengeParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {

    Optional<ChallengeParticipation> findByChallengeIdAndStudentId(Long challengeId, Long studentId);

    List<ChallengeParticipation> findByChallengeId(Long challengeId);

    List<ChallengeParticipation> findByChallengeIdIn(Collection<Long> challengeIds);

    List<ChallengeParticipation> findByStudentId(Long studentId);

    List<ChallengeParticipation> findByStudentIdAndActiveTrue(Long studentId);

    void deleteByChallengeId(Long challengeId);
}