package com.mapa.repository;

import com.mapa.domain.ClassEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    boolean existsBySchoolClassIdAndStudentId(Long classId, Long studentId);

    Optional<ClassEnrollment> findBySchoolClassIdAndStudentId(Long classId, Long studentId);

    List<ClassEnrollment> findBySchoolClassIdOrderByJoinedAtAsc(Long classId);

    Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable);

    long countBySchoolClassId(Long classId);

    void deleteBySchoolClassId(Long classId);

    @Query("SELECT ta.schoolClass.id, COUNT(ta) FROM ClassEnrollment ta "
            + "WHERE ta.schoolClass.id IN :schoolClassIds GROUP BY ta.schoolClass.id")
    List<Object[]> countStudentsBySchoolClassIds(@Param("schoolClassIds") Collection<Long> classIds);
}