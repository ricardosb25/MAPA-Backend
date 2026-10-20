package com.mapa.service;

import com.mapa.domain.Challenge;
import com.mapa.domain.ChallengeProgress;
import com.mapa.domain.DynamometerTest;
import com.mapa.domain.Motor;
import com.mapa.domain.SchoolClass;
import com.mapa.domain.User;
import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.domain.enums.EngineStatus;
import com.mapa.domain.enums.Role;
import com.mapa.repository.ChallengeProgressRepository;
import com.mapa.repository.ChallengeRepository;
import com.mapa.repository.ClassEnrollmentRepository;
import com.mapa.repository.MotorRepository;
import com.mapa.repository.SchoolClassRepository;
import com.mapa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeProgressRepository challengeProgressRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock
    private MotorRepository motorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    void shouldIgnoreSimulationWhenStudentHasNoSelectedChallenge() {
        User student = student();
        Motor motor = motor();

        when(challengeProgressRepository.findByStudentIdAndSelectedIsTrue(5L)).thenReturn(Optional.empty());

        challengeService.registerSimulationAttempts(student, motor, dynamometerTest(131.5, EngineStatus.INTEGRO));

        verify(challengeProgressRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldCompleteSelectedChallengeWhenPowerAndIntegrityAreMet() {
        User student = student();
        Motor motor = motor();
        Challenge challenge = challenge(motor);
        ChallengeProgress progress = ChallengeProgress.builder()
                .id(9L)
                .challenge(challenge)
                .student(student)
                .selected(true)
                .attempts(1)
                .completed(false)
                .build();

        when(challengeProgressRepository.findByStudentIdAndSelectedIsTrue(5L)).thenReturn(Optional.of(progress));
        when(challengeProgressRepository.save(progress)).thenReturn(progress);

        challengeService.registerSimulationAttempts(student, motor, dynamometerTest(110.0, EngineStatus.INTEGRO));

        ArgumentCaptor<ChallengeProgress> savedProgress = ArgumentCaptor.forClass(ChallengeProgress.class);
        verify(challengeProgressRepository).save(savedProgress.capture());
        assertEquals(2, savedProgress.getValue().getAttempts());
        assertTrue(savedProgress.getValue().isCompleted());
        assertEquals(110.0, savedProgress.getValue().getLastPeakPower());
        assertEquals(EngineStatus.INTEGRO, savedProgress.getValue().getLastEngineStatus());
    }

    @Test
    void shouldDeleteProgressAndChallengesOfAClass() {
        challengeService.deleteAllByClass(4L);

        verify(challengeProgressRepository).deleteByChallengeSchoolClassId(4L);
        verify(challengeRepository).deleteBySchoolClassId(4L);
    }

    private Challenge challenge(Motor motor) {
        return Challenge.builder()
                .id(3L)
                .title("Falta de potência em alta rotação")
                .description("Corrija combustível e avanço sem detonação.")
                .difficulty(ChallengeDifficulty.MEDIUM)
                .motor(motor)
                .schoolClass(SchoolClass.builder().id(4L).build())
                .teacher(User.builder().id(2L).role(Role.TEACHER).build())
                .goalPowerCv(103)
                .requiredEngineStatus(EngineStatus.INTEGRO)
                .build();
    }

    private DynamometerTest dynamometerTest(double peakPower, EngineStatus status) {
        return DynamometerTest.builder()
                .userId(5L)
                .motorId(1L)
                .maxPower(peakPower)
                .maxTorque(15.0)
                .averageAfr(14.1)
                .engineStatus(status)
                .build();
    }

    private Motor motor() {
        return Motor.builder().id(1L).name("AP 1.8 8V").manufacturer("Volkswagen").build();
    }

    private User student() {
        return User.builder().id(5L).role(Role.STUDENT).fullName("Ana Lima").email("ana@email.com").build();
    }
}
