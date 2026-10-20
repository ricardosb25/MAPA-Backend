package com.mapa.service;

import com.mapa.domain.DynamometerTest;
import com.mapa.domain.EcuMapDefault;
import com.mapa.domain.EcuMapTuning;
import com.mapa.domain.Motor;
import com.mapa.domain.User;
import com.mapa.domain.enums.EcuMapType;
import com.mapa.domain.enums.EngineStatus;
import com.mapa.domain.enums.Role;
import com.mapa.dto.DynoResultDTO;
import com.mapa.dto.DynoTestResponseDTO;
import com.mapa.exception.ActiveMotorNotSelectedException;
import com.mapa.repository.DynamometerTestRepository;
import com.mapa.repository.EcuMapDefaultRepository;
import com.mapa.repository.EcuMapTuningRepository;
import com.mapa.repository.MotorRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamometerPhysicsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MotorRepository motorRepository;

    @Mock
    private EcuMapDefaultRepository ecuMapDefaultRepository;

    @Mock
    private EcuMapTuningRepository ecuMapTuningRepository;

    @Mock
    private DynamometerTestRepository dynamometerTestRepository;

    @Mock
    private ChallengeService challengeService;

    @InjectMocks
    private DynamometerPhysicsService dynamometerPhysicsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRunSimulationWithActiveMotorAndFactoryMaps() {
        authenticateAs(user(5L, Role.STUDENT, 1L));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.STUDENT, 1L)));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(any(), any()))
                .thenAnswer(invocation -> Optional.of(factoryMap(invocation.getArgument(1))));
        when(dynamometerTestRepository.save(any(DynamometerTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DynoResultDTO result = dynamometerPhysicsService.runSimulation();

        assertEquals(EngineStatus.INTEGRO, result.statusFinal());
        assertEquals(23, result.rpmEixoX().size());
        assertEquals(result.curvaPotencia().size(), result.curvaTorque().size());

        ArgumentCaptor<DynamometerTest> savedTest = ArgumentCaptor.forClass(DynamometerTest.class);
        verify(dynamometerTestRepository).save(savedTest.capture());
        assertEquals(EngineStatus.INTEGRO, savedTest.getValue().getEngineStatus());
        assertEquals(5L, savedTest.getValue().getUserId());
        assertEquals(1L, savedTest.getValue().getMotorId());
        verify(challengeService).registerSimulationAttempts(any(User.class), any(Motor.class), any(DynamometerTest.class));
    }

    @Test
    void shouldFailWhenUserHasNoActiveMotor() {
        authenticateAs(user(5L, Role.STUDENT, null));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, Role.STUDENT, null)));

        assertThrows(ActiveMotorNotSelectedException.class, () -> dynamometerPhysicsService.runSimulation());

        verify(dynamometerTestRepository, never()).save(any());
    }

    @Test
    void shouldUseUserTuningAndRecordBrokenStatusWhenMixtureIsLean() {
        User student = user(5L, Role.STUDENT, 1L);
        authenticateAs(student);
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(any(), any()))
                .thenAnswer(invocation -> Optional.of(factoryMap(invocation.getArgument(1))));
        when(ecuMapTuningRepository.findByUserIdAndMotorIdAndMapType(5L, 1L, EcuMapType.FUEL))
                .thenReturn(Optional.of(tuningWithConstantValue(10)));
        when(ecuMapTuningRepository.findByUserIdAndMotorIdAndMapType(5L, 1L, EcuMapType.IGNITION))
                .thenReturn(Optional.empty());
        when(dynamometerTestRepository.save(any(DynamometerTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DynoResultDTO result = dynamometerPhysicsService.runSimulation();

        assertEquals(EngineStatus.QUEBRADO_POBRE, result.statusFinal());

        ArgumentCaptor<DynamometerTest> savedTest = ArgumentCaptor.forClass(DynamometerTest.class);
        verify(dynamometerTestRepository).save(savedTest.capture());
        assertEquals(EngineStatus.QUEBRADO_POBRE, savedTest.getValue().getEngineStatus());
    }

    @Test
    void shouldReturnHistoryForCurrentUserId() {
        User student = user(5L, Role.STUDENT, 1L);
        authenticateAs(student);
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));
        when(dynamometerTestRepository.findByUserIdOrderByTestedAtDesc(5L))
                .thenReturn(List.of(DynamometerTest.builder()
                        .id(9L)
                        .userId(5L)
                        .motorId(1L)
                        .maxPower(131.5)
                        .maxTorque(15.7)
                        .averageAfr(14.1)
                        .engineStatus(EngineStatus.INTEGRO)
                        .testedAt(OffsetDateTime.now())
                        .build()));

        List<DynoTestResponseDTO> history = dynamometerPhysicsService.findHistory();

        assertEquals(1, history.size());
        assertEquals(9L, history.get(0).id());
        assertEquals("INTEGRO", history.get(0).statusFinal());
        assertEquals(131.5, history.get(0).potenciaMaxima());
    }

    private EcuMapTuning tuningWithConstantValue(int value) {
        List<List<Integer>> grid = new ArrayList<>();
        EcuMapSpec fuelSpec = EcuMapSpec.forMotor(aspiratedMotor(), EcuMapType.FUEL);
        for (int loadIndex = 0; loadIndex < fuelSpec.loadBreakpoints().size(); loadIndex++) {
            grid.add(new ArrayList<>(Collections.nCopies(fuelSpec.rpmBreakpoints().size(), value)));
        }

        return EcuMapTuning.builder()
                .id(3L)
                .userId(5L)
                .motorId(1L)
                .mapType(EcuMapType.FUEL)
                .tunedValues(grid)
                .build();
    }

    private EcuMapDefault factoryMap(EcuMapType mapType) {
        Motor motor = aspiratedMotor();
        EcuMapSpec spec = EcuMapSpec.forMotor(motor, mapType);
        BigDecimal displacementClass = EcuMapSpec.resolveDisplacementClass(motor.getDisplacementLiters());

        return EcuMapDefault.builder()
                .id(7L)
                .displacementClass(displacementClass)
                .mapType(mapType)
                .rpmBreakpoints(spec.rpmBreakpoints())
                .loadBreakpoints(spec.loadBreakpoints())
                .factoryValues(EcuMapFactory.generate(displacementClass, spec))
                .build();
    }

    private Motor aspiratedMotor() {
        return Motor.builder()
                .id(1L)
                .displacementLiters(new BigDecimal("1.8"))
                .manufacturer("VOLKSWAGEN")
                .name("AP 1.8 8V")
                .compressionRatio(new BigDecimal("9.2"))
                .rpmCutoff(6500)
                .aspirationType("ASPIRADO")
                .build();
    }

    private User user(Long id, Role role, Long motorAtivoId) {
        return User.builder()
                .id(id)
                .fullName("Ana Lima")
                .email("ana@email.com")
                .passwordHash("hash")
                .role(role)
                .active(true)
                .motorAtivoId(motorAtivoId)
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }
}
