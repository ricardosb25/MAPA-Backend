package com.mapa.repository;

import com.mapa.domain.EcuMapTuning;
import com.mapa.domain.enums.EcuMapType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcuMapTuningRepository extends JpaRepository<EcuMapTuning, Long> {

    Optional<EcuMapTuning> findByUserIdAndMotorIdAndMapType(Long userId, Long motorId, EcuMapType mapType);

    List<EcuMapTuning> findByMotorId(Long motorId);

    void deleteByUserId(Long userId);

    void deleteByMotorId(Long motorId);
}
