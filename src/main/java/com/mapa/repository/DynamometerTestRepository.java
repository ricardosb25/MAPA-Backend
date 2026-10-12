package com.mapa.repository;

import com.mapa.domain.DynamometerTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamometerTestRepository extends JpaRepository<DynamometerTest, Long> {

    List<DynamometerTest> findByUserIdOrderByTestedAtDesc(Long userId);
}
