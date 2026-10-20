package com.mapa.service;

import com.mapa.domain.Challenge;
import com.mapa.domain.ChallengeProgress;
import com.mapa.domain.ClassEnrollment;
import com.mapa.domain.DynamometerTest;
import com.mapa.domain.Motor;
import com.mapa.domain.SchoolClass;
import com.mapa.domain.User;
import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.domain.enums.Role;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.challenge.ChallengePerformanceDTO;
import com.mapa.dto.challenge.ChallengeRequestDTO;
import com.mapa.dto.challenge.ChallengeResponseDTO;
import com.mapa.dto.challenge.ChallengeStudentProgressDTO;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.ChallengeProgressRepository;
import com.mapa.repository.ChallengeRepository;
import com.mapa.repository.ClassEnrollmentRepository;
import com.mapa.repository.MotorRepository;
import com.mapa.repository.SchoolClassRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeProgressRepository challengeProgressRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final MotorRepository motorRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponseDTO<ChallengeResponseDTO> findAllByTeacher(
            String search, Long classId, ChallengeDifficulty difficulty, Long motorId, Pageable pageable) {

        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.TEACHER, "listar desafios");

        Page<Challenge> challengesPage = challengeRepository.findAll(
                buildTeacherFilters(principal.getId(), search, classId, difficulty, motorId), pageable);
        return toTeacherResponsePage(challengesPage);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ChallengeResponseDTO> findAvailableForStudent(
            String search, Long classId, ChallengeDifficulty difficulty, Long motorId, Pageable pageable) {

        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.STUDENT, "consultar desafios");

        List<Long> enrolledClassIds = classEnrollmentRepository.findByStudentId(principal.getId(), Pageable.unpaged())
                .stream()
                .map(enrollment -> enrollment.getSchoolClass().getId())
                .toList();
        if (enrolledClassIds.isEmpty()) {
            return PageResponseDTO.fromPage(Page.empty(pageable));
        }

        Page<Challenge> challengesPage = challengeRepository.findAll(
                buildStudentFilters(enrolledClassIds, search, classId, difficulty, motorId), pageable);
        return toStudentResponsePage(challengesPage, principal.getId());
    }

    @Transactional(readOnly = true)
    public ChallengeResponseDTO findById(Long challengeId) {
        Challenge challenge = findEntityById(challengeId);
        UserPrincipal principal = currentPrincipal();

        if (Role.TEACHER.name().equals(principal.getRole())) {
            requireOwnChallenge(challenge);
            long enrolledStudents = classEnrollmentRepository.countBySchoolClassId(challenge.getSchoolClass().getId());
            long completedStudents = countCompleted(challenge.getId());
            return ChallengeResponseDTO.fromEntity(challenge, null, completedStudents, enrolledStudents);
        }

        requireEnrolledStudent(challenge, principal.getId());
        ChallengeProgress progress = challengeProgressRepository
                .findByChallengeIdAndStudentId(challengeId, principal.getId())
                .orElse(null);
        return ChallengeResponseDTO.fromEntity(challenge, progress, 0, 0);
    }

    @Transactional(readOnly = true)
    public ChallengeResponseDTO findActiveForStudent(Long motorId) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.STUDENT, "consultar o desafio ativo");

        ChallengeProgress progress = challengeProgressRepository
                .findByStudentIdAndSelectedIsTrue(principal.getId())
                .orElse(null);
        if (progress == null) {
            return null;
        }

        Challenge challenge = progress.getChallenge();
        if (!classEnrollmentRepository.existsBySchoolClassIdAndStudentId(
                challenge.getSchoolClass().getId(), principal.getId())) {
            return null;
        }

        if (motorId != null && !Objects.equals(challenge.getMotor().getId(), motorId)) {
            return null;
        }

        return ChallengeResponseDTO.fromEntity(challenge, progress, 0, 0);
    }

    @Transactional
    public ChallengeResponseDTO create(ChallengeRequestDTO request) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.TEACHER, "criar desafios");

        User teacher = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        SchoolClass schoolClass = findOwnedClass(request.classId(), principal.getId());
        Motor motor = findMotor(request.motorId());

        Challenge challenge = Challenge.builder()
                .teacher(teacher)
                .schoolClass(schoolClass)
                .motor(motor)
                .build();
        request.applyTo(challenge);

        Challenge createdChallenge = challengeRepository.save(challenge);
        long enrolledStudents = classEnrollmentRepository.countBySchoolClassId(schoolClass.getId());
        return ChallengeResponseDTO.fromEntity(createdChallenge, null, 0, enrolledStudents);
    }

    @Transactional
    public ChallengeResponseDTO update(Long challengeId, ChallengeRequestDTO request) {
        Challenge challenge = findEntityById(challengeId);
        requireOwnChallenge(challenge);

        challenge.setSchoolClass(findOwnedClass(request.classId(), currentPrincipal().getId()));
        challenge.setMotor(findMotor(request.motorId()));
        request.applyTo(challenge);

        Challenge updatedChallenge = challengeRepository.save(challenge);
        long enrolledStudents = classEnrollmentRepository.countBySchoolClassId(updatedChallenge.getSchoolClass().getId());
        return ChallengeResponseDTO.fromEntity(updatedChallenge, null, countCompleted(challengeId), enrolledStudents);
    }

    @Transactional
    public void delete(Long challengeId) {
        Challenge challenge = findEntityById(challengeId);
        requireOwnChallenge(challenge);
        challengeProgressRepository.deleteByChallengeId(challengeId);
        challengeRepository.delete(challenge);
    }

    @Transactional
    public void deleteAllByClass(Long classId) {
        challengeProgressRepository.deleteByChallengeSchoolClassId(classId);
        challengeRepository.deleteBySchoolClassId(classId);
    }

    @Transactional
    public ChallengeResponseDTO selectForStudent(Long challengeId) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.STUDENT, "selecionar desafios");

        Challenge challenge = findEntityById(challengeId);
        requireEnrolledStudent(challenge, principal.getId());

        User student = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        challengeProgressRepository.findByStudentId(principal.getId()).forEach(progress -> {
            if (progress.isSelected() && !Objects.equals(progress.getChallenge().getId(), challengeId)) {
                progress.setSelected(false);
            }
        });

        ChallengeProgress progress = challengeProgressRepository
                .findByChallengeIdAndStudentId(challengeId, principal.getId())
                .orElseGet(() -> ChallengeProgress.builder()
                        .challenge(challenge)
                        .student(student)
                        .build());
        progress.setSelected(true);
        progress.setSelectedAt(OffsetDateTime.now());
        ChallengeProgress savedProgress = challengeProgressRepository.save(progress);
        return ChallengeResponseDTO.fromEntity(challenge, savedProgress, 0, 0);
    }

    @Transactional(readOnly = true)
    public ChallengePerformanceDTO findPerformance(Long challengeId) {
        Challenge challenge = findEntityById(challengeId);
        requireOwnChallenge(challenge);

        List<ClassEnrollment> enrollments = classEnrollmentRepository
                .findBySchoolClassIdOrderByJoinedAtAsc(challenge.getSchoolClass().getId());
        Map<Long, ChallengeProgress> progressByStudent = challengeProgressRepository.findByChallengeId(challengeId)
                .stream()
                .collect(Collectors.toMap(progress -> progress.getStudent().getId(), Function.identity()));

        List<ChallengeStudentProgressDTO> students = enrollments.stream()
                .map(enrollment -> ChallengeStudentProgressDTO.fromEnrollment(
                        enrollment,
                        progressByStudent.get(enrollment.getStudent().getId()),
                        challenge.getGoalPowerCv(),
                        challenge.getRequiredEngineStatus()))
                .toList();

        return ChallengePerformanceDTO.fromChallenge(challenge, students);
    }

    @Transactional
    public void registerSimulationAttempts(User user, Motor motor, DynamometerTest dynamometerTest) {
        if (user == null || motor == null || dynamometerTest == null || user.getRole() != Role.STUDENT) {
            return;
        }

        Optional<ChallengeProgress> selectedProgress = challengeProgressRepository
                .findByStudentIdAndSelectedIsTrue(user.getId());
        if (selectedProgress.isEmpty()) {
            return;
        }

        ChallengeProgress progress = selectedProgress.get();
        Challenge challenge = progress.getChallenge();
        if (!Objects.equals(challenge.getMotor().getId(), motor.getId())) {
            return;
        }

        progress.setAttempts(progress.getAttempts() + 1);
        progress.setLastPeakPower(dynamometerTest.getMaxPower());
        progress.setLastEngineStatus(dynamometerTest.getEngineStatus());

        if (ChallengeCriteria.isCompleted(
                challenge.getGoalPowerCv(),
                challenge.getRequiredEngineStatus(),
                dynamometerTest.getMaxPower(),
                dynamometerTest.getEngineStatus())
                && !progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(OffsetDateTime.now());
        }

        challengeProgressRepository.save(progress);
    }

    private PageResponseDTO<ChallengeResponseDTO> toTeacherResponsePage(Page<Challenge> challengesPage) {
        List<Challenge> challenges = challengesPage.getContent();
        Map<Long, Long> enrolledByClass = enrolledCountsFor(challenges);
        Map<Long, Long> completedByChallenge = completedCountsFor(challenges);

        return PageResponseDTO.fromPage(challengesPage.map(challenge -> ChallengeResponseDTO.fromEntity(
                challenge,
                null,
                completedByChallenge.getOrDefault(challenge.getId(), 0L),
                enrolledByClass.getOrDefault(challenge.getSchoolClass().getId(), 0L))));
    }

    private PageResponseDTO<ChallengeResponseDTO> toStudentResponsePage(Page<Challenge> challengesPage, Long studentId) {
        Map<Long, ChallengeProgress> progressByChallenge = challengeProgressRepository.findByStudentId(studentId)
                .stream()
                .collect(Collectors.toMap(progress -> progress.getChallenge().getId(), Function.identity()));

        return PageResponseDTO.fromPage(challengesPage.map(challenge -> ChallengeResponseDTO.fromEntity(
                challenge,
                progressByChallenge.get(challenge.getId()),
                0,
                0)));
    }

    private Map<Long, Long> enrolledCountsFor(List<Challenge> challenges) {
        if (challenges.isEmpty()) {
            return Map.of();
        }

        List<Long> classIds = challenges.stream()
                .map(challenge -> challenge.getSchoolClass().getId())
                .distinct()
                .toList();
        Map<Long, Long> enrolledByClass = new HashMap<>();
        classEnrollmentRepository.countStudentsBySchoolClassIds(classIds)
                .forEach(row -> enrolledByClass.put((Long) row[0], (Long) row[1]));
        return enrolledByClass;
    }

    private Map<Long, Long> completedCountsFor(List<Challenge> challenges) {
        Map<Long, Long> completedByChallenge = new HashMap<>();
        for (Challenge challenge : challenges) {
            completedByChallenge.put(challenge.getId(), countCompleted(challenge.getId()));
        }
        return completedByChallenge;
    }

    private long countCompleted(Long challengeId) {
        return challengeProgressRepository.findByChallengeId(challengeId).stream()
                .filter(ChallengeProgress::isCompleted)
                .count();
    }

    private Specification<Challenge> buildTeacherFilters(
            Long teacherId, String search, Long classId, ChallengeDifficulty difficulty, Long motorId) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("teacher").get("id"), teacherId));
            applyCommonFilters(predicates, root, criteriaBuilder, search, classId, difficulty, motorId);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Challenge> buildStudentFilters(
            List<Long> enrolledClassIds, String search, Long classId, ChallengeDifficulty difficulty, Long motorId) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("schoolClass").get("id").in(enrolledClassIds));
            applyCommonFilters(predicates, root, criteriaBuilder, search, classId, difficulty, motorId);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyCommonFilters(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Challenge> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            String search,
            Long classId,
            ChallengeDifficulty difficulty,
            Long motorId) {

        if (classId != null) {
            predicates.add(criteriaBuilder.equal(root.get("schoolClass").get("id"), classId));
        }
        if (difficulty != null) {
            predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
        }
        if (motorId != null) {
            predicates.add(criteriaBuilder.equal(root.get("motor").get("id"), motorId));
        }

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!normalizedSearch.isEmpty()) {
            String pattern = "%" + normalizedSearch + "%";
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("schoolClass").get("code")), pattern)));
        }
    }

    private SchoolClass findOwnedClass(Long classId, Long teacherId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada com id: " + classId));
        if (!Objects.equals(schoolClass.getTeacher().getId(), teacherId)) {
            throw new AccessDeniedException("Acesso negado: você só pode atribuir desafios às próprias turmas");
        }
        return schoolClass;
    }

    private Motor findMotor(Long motorId) {
        return motorRepository.findById(motorId)
                .orElseThrow(() -> new ResourceNotFoundException("Motor não encontrado com id: " + motorId));
    }

    private Challenge findEntityById(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Desafio não encontrado com id: " + challengeId));
    }

    private void requireOwnChallenge(Challenge challenge) {
        UserPrincipal principal = currentPrincipal();
        requireRole(principal, Role.TEACHER, "gerenciar desafios");
        if (!Objects.equals(challenge.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("Acesso negado: você só pode gerenciar os próprios desafios");
        }
    }

    private void requireEnrolledStudent(Challenge challenge, Long studentId) {
        requireRole(currentPrincipal(), Role.STUDENT, "acessar desafios");
        if (!classEnrollmentRepository.existsBySchoolClassIdAndStudentId(challenge.getSchoolClass().getId(), studentId)) {
            throw new AccessDeniedException("Acesso negado: você não participa da turma deste desafio");
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
