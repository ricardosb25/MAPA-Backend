package com.mapa.repository;

import com.mapa.domain.EcuMapDefault;
import com.mapa.domain.enums.EcuMapType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface EcuMapDefaultRepository extends JpaRepository<EcuMapDefault, Long> {

    Optional<EcuMapDefault> findByDisplacementClassAndMapType(BigDecimal displacementClass, EcuMapType mapType);
}