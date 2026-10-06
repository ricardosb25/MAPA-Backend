package com.mapa.repository;

import com.mapa.domain.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long>, JpaSpecificationExecutor<SchoolClass> {

    Optional<SchoolClass> findByCode(String code);

    boolean existsByCode(String code);
}