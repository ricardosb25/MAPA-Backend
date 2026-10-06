package com.mapa.service;

import com.mapa.domain.SchoolClass;
import com.mapa.domain.ClassEnrollment;
import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.Shift;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.schoolClass.ClassStudentResponseDTO;
import com.mapa.dto.schoolClass.ClassDetailResponseDTO;
import com.mapa.dto.schoolClass.ClassRequestDTO;
import com.mapa.dto.schoolClass.ClassResponseDTO;
import com.mapa.exception.AlreadyEnrolledException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.ClassEnrollmentRepository;
import com.mapa.repository.SchoolClassRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private static final int MAX_CODE_ATTEMPTS = 25;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SchoolClassRepository schoolClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponseDTO<ClassResponseDTO> findAllByTeacher(String search, Shift shift, Pageable pageable) {
        UserPrincipal principal = currentPrincipal();
        Page<SchoolClass> classesPage = schoolClassRepository.findAll(
                buildTeacherFilters(principal.getId(), search, shift), pageable);
        return toResponsePage(classesPage);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ClassResponseDTO> findAllByStudent(String search, Shift shift, Pageable pageable) {
        UserPrincipal principal = currentPrincipal();
        Page<SchoolClass> classesPage = schoolClassRepository.findAll(
                buildStudentFilters(principal.getId(), search, shift), pageable);
        return toResponsePage(classesPage);
    }

    @Transactional(readOnly = true)
    public ClassDetailResponseDTO findById(Long classId) {
        SchoolClass schoolClass = findEntityById(classId);
        requireOwnClass(schoolClass);
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findBySchoolClassIdOrderByJoinedAtAsc(classId);
        return ClassDetailResponseDTO.fromEntity(schoolClass, enrollments);
    }

    @Transactional
    public ClassResponseDTO create(ClassRequestDTO request) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.TEACHER, "criar turmas");

        User teacher = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        SchoolClass schoolClass = SchoolClass.builder()
                .code(generateUniqueCode(request.name()))
                .teacher(teacher)
                .build();
        request.applyTo(schoolClass);

        SchoolClass createdClass = schoolClassRepository.save(schoolClass);
        return ClassResponseDTO.fromEntity(createdClass, 0L);
    }

    @Transactional
    public ClassResponseDTO update(Long classId, ClassRequestDTO request) {
        SchoolClass schoolClass = findEntityById(classId);
        requireOwnClass(schoolClass);
        request.applyTo(schoolClass);

        SchoolClass updatedClass = schoolClassRepository.save(schoolClass);
        long studentCount = classEnrollmentRepository.countBySchoolClassId(classId);
        return ClassResponseDTO.fromEntity(updatedClass, studentCount);
    }

    @Transactional
    public void delete(Long classId) {
        SchoolClass schoolClass = findEntityById(classId);
        requireOwnClass(schoolClass);

        classEnrollmentRepository.deleteBySchoolClassId(classId);
        schoolClassRepository.delete(schoolClass);
    }

    @Transactional
    public ClassResponseDTO regenerateCode(Long classId) {
        SchoolClass schoolClass = findEntityById(classId);
        requireOwnClass(schoolClass);

        schoolClass.setCode(generateUniqueCode(schoolClass.getName()));
        SchoolClass updatedClass = schoolClassRepository.save(schoolClass);
        long studentCount = classEnrollmentRepository.countBySchoolClassId(classId);
        return ClassResponseDTO.fromEntity(updatedClass, studentCount);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponseDTO> findStudents(Long classId) {
        SchoolClass schoolClass = findEntityById(classId);
        requireOwnClass(schoolClass);
        return classEnrollmentRepository.findBySchoolClassIdOrderByJoinedAtAsc(classId)
                .stream()
                .map(ClassStudentResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void removeStudent(Long classId, Long studentId) {
        SchoolClass schoolClass = findEntityById(classId);
        requireOwnClass(schoolClass);

        ClassEnrollment matricula = classEnrollmentRepository.findBySchoolClassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não matriculado nesta turma"));
        classEnrollmentRepository.delete(matricula);
    }

    @Transactional
    public ClassResponseDTO join(String codigo) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.STUDENT, "entrar em turmas");

        String normalizedCode = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        SchoolClass schoolClass = schoolClassRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada para o código informado"));

        boolean alreadyEnrolled = classEnrollmentRepository.existsBySchoolClassIdAndStudentId(schoolClass.getId(), principal.getId());
        if (alreadyEnrolled) {
            throw new AlreadyEnrolledException("Você já participa desta turma");
        }

        User student = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        classEnrollmentRepository.save(ClassEnrollment.builder().schoolClass(schoolClass).student(student).build());

        long studentCount = classEnrollmentRepository.countBySchoolClassId(schoolClass.getId());
        return ClassResponseDTO.fromEntity(schoolClass, studentCount);
    }

    @Transactional
    public void leave(Long classId) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.STUDENT, "sair de turmas");
        findEntityById(classId);

        ClassEnrollment matricula = classEnrollmentRepository.findBySchoolClassIdAndStudentId(classId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Você não participa desta turma"));
        classEnrollmentRepository.delete(matricula);
    }

    private PageResponseDTO<ClassResponseDTO> toResponsePage(Page<SchoolClass> classesPage) {
        Map<Long, Long> studentCounts = studentCountsFor(classesPage.getContent());
        return PageResponseDTO.fromPage(classesPage.map(schoolClass ->
                ClassResponseDTO.fromEntity(schoolClass, studentCounts.getOrDefault(schoolClass.getId(), 0L))));
    }

    private Map<Long, Long> studentCountsFor(List<SchoolClass> classes) {
        if (classes.isEmpty()) {
            return Map.of();
        }

        List<Long> classIds = classes.stream().map(SchoolClass::getId).toList();
        Map<Long, Long> studentCounts = new HashMap<>();
        classEnrollmentRepository.countStudentsBySchoolClassIds(classIds)
                .forEach(row -> studentCounts.put((Long) row[0], (Long) row[1]));
        return studentCounts;
    }

    private Specification<SchoolClass> buildTeacherFilters(Long teacherId, String search, Shift shift) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("teacher").get("id"), teacherId));
            applyCommonFilters(predicates, root, criteriaBuilder, search, shift);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<SchoolClass> buildStudentFilters(Long studentId, String search, Shift shift) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<Long> enrollmentSubquery = query.subquery(Long.class);
            Root<ClassEnrollment> enrollmentRoot = enrollmentSubquery.from(ClassEnrollment.class);
            enrollmentSubquery.select(enrollmentRoot.get("id"));
            enrollmentSubquery.where(
                    criteriaBuilder.equal(enrollmentRoot.get("schoolClass"), root),
                    criteriaBuilder.equal(enrollmentRoot.get("student").get("id"), studentId));
            predicates.add(criteriaBuilder.exists(enrollmentSubquery));

            applyCommonFilters(predicates, root, criteriaBuilder, search, shift);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyCommonFilters(
            List<Predicate> predicates, Root<SchoolClass> root, CriteriaBuilder criteriaBuilder, String search, Shift shift) {

        if (shift != null) {
            predicates.add(criteriaBuilder.equal(root.get("shift"), shift));
        }

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!normalizedSearch.isEmpty()) {
            String pattern = "%" + normalizedSearch + "%";
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)));
        }
    }

    private String generateUniqueCode(String schoolClassName) {
        String prefix = normalizeCodePrefix(schoolClassName);

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String candidate = prefix + "-" + randomSuffix(2);
            if (!schoolClassRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        return prefix + "-" + randomSuffix(4);
    }

    private String normalizeCodePrefix(String schoolClassName) {
        String withoutAccents = Normalizer.normalize(
                schoolClassName == null ? "" : schoolClassName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);

        StringBuilder prefix = new StringBuilder();
        for (int position = 0; position < withoutAccents.length() && prefix.length() < 3; position++) {
            char character = withoutAccents.charAt(position);
            if (character >= 'A' && character <= 'Z') {
                prefix.append(character);
            }
        }
        while (prefix.length() < 3) {
            prefix.append('X');
        }
        return prefix.toString();
    }

    private String randomSuffix(int length) {
        StringBuilder suffix = new StringBuilder();
        for (int position = 0; position < length; position++) {
            suffix.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return suffix.toString();
    }

    private SchoolClass findEntityById(Long classId) {
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada com id: " + classId));
    }

    private void requireOwnClass(SchoolClass schoolClass) {
        UserPrincipal principal = currentPrincipal();
        if (!Objects.equals(schoolClass.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("Acesso negado: você só pode gerenciar as próprias turmas");
        }
    }

    private void requireRole(UserPrincipal principal, Role requiredRole, String actionDescription) {
        if (principal == null || !requiredRole.name().equals(principal.getRole())) {
            throw new AccessDeniedException("Acesso negado: você não tem permissão para " + actionDescription);
        }
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        return principal;
    }
}