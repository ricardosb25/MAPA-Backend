package com.mapa.service;

import com.mapa.domain.EcuMapDefault;
import com.mapa.domain.EcuMapTuning;
import com.mapa.domain.Motor;
import com.mapa.domain.User;
import com.mapa.domain.enums.EcuMapType;
import com.mapa.domain.enums.Role;
import com.mapa.dto.EcuMapCellUpdateRequestDTO;
import com.mapa.dto.EcuMapResponseDTO;
import com.mapa.exception.InvalidEcuMapValueException;
import com.mapa.repository.EcuMapDefaultRepository;
import com.mapa.repository.EcuMapTuningRepository;
import com.mapa.repository.MotorRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcuMapServiceTest {

    @Mock
    private MotorRepository motorRepository;

    @Mock
    private EcuMapDefaultRepository ecuMapDefaultRepository;

    @Mock
    private EcuMapTuningRepository ecuMapTuningRepository;

    @InjectMocks
    private EcuMapService ecuMapService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistFactoryMapsWhenTheyDoNotExistYet() {
        authenticateAs(user(5L, Role.STUDENT));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(any(), any())).thenReturn(Optional.empty());
        when(ecuMapDefaultRepository.save(any(EcuMapDefault.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<EcuMapResponseDTO> maps = ecuMapService.findMapsByMotor(1L);

        assertEquals(2, maps.size());
        assertTrue(maps.stream().allMatch(EcuMapResponseDTO::factoryDefault));
        assertTrue(maps.stream().allMatch(map -> map.values().equals(map.defaultValues())));
        verify(ecuMapDefaultRepository, org.mockito.Mockito.times(2)).save(any(EcuMapDefault.class));
    }

    @Test
    void shouldKeepEveryFactoryValueInsideTheAllowedEcuRange() {
        authenticateAs(user(5L, Role.STUDENT));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(forcedInductionMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(any(), any())).thenReturn(Optional.empty());
        when(ecuMapDefaultRepository.save(any(EcuMapDefault.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<EcuMapResponseDTO> maps = ecuMapService.findMapsByMotor(1L);

        for (EcuMapResponseDTO map : maps) {
            assertTrue(isInsideRange(map.values(), map.minValue(), map.maxValue()),
                    map.mapType() + " deve respeitar os limites da ECU");
        }
    }

    @Test
    void shouldPersistEditedCellAsTuningWithoutTouchingFactoryValues() {
        authenticateAs(user(5L, Role.STUDENT));
        EcuMapDefault factoryMap = factoryMap(EcuMapType.IGNITION);
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(new BigDecimal("1.8"), EcuMapType.IGNITION))
                .thenReturn(Optional.of(factoryMap));
        when(ecuMapTuningRepository.findByUserIdAndMotorIdAndMapType(5L, 1L, EcuMapType.IGNITION))
                .thenReturn(Optional.empty());
        when(ecuMapTuningRepository.save(any(EcuMapTuning.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EcuMapResponseDTO updatedMap = ecuMapService.updateCell(
                1L, EcuMapType.IGNITION, new EcuMapCellUpdateRequestDTO(0, 0, 21));

        assertFalse(updatedMap.factoryDefault());
        assertEquals(21, updatedMap.values().get(0).get(0));
        assertEquals(factoryMap.getFactoryValues().get(0).get(0), updatedMap.defaultValues().get(0).get(0));
        verify(ecuMapTuningRepository).save(any(EcuMapTuning.class));
    }

    @Test
    void shouldRejectIgnitionValueAboveRealisticCeiling() {
        authenticateAs(user(5L, Role.STUDENT));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));

        InvalidEcuMapValueException exception = assertThrows(
                InvalidEcuMapValueException.class,
                () -> ecuMapService.updateCell(
                        1L, EcuMapType.IGNITION, new EcuMapCellUpdateRequestDTO(0, 0, 46)));

        assertTrue(exception.getMessage().contains("fora do intervalo"));
        verify(ecuMapTuningRepository, never()).save(any(EcuMapTuning.class));
    }

    @Test
    void shouldRejectForcedInductionIgnitionAboveBoostSafeCeiling() {
        authenticateAs(user(5L, Role.TEACHER));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(forcedInductionMotor()));

        InvalidEcuMapValueException exception = assertThrows(
                InvalidEcuMapValueException.class,
                () -> ecuMapService.updateCell(
                        1L, EcuMapType.IGNITION, new EcuMapCellUpdateRequestDTO(0, 0, 36)));

        assertTrue(exception.getMessage().contains("fora do intervalo"));
    }

    @Test
    void shouldRejectFuelValueBelowRealisticFloor() {
        authenticateAs(user(5L, Role.STUDENT));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));

        assertThrows(
                InvalidEcuMapValueException.class,
                () -> ecuMapService.updateCell(
                        1L, EcuMapType.FUEL, new EcuMapCellUpdateRequestDTO(0, 0, 5)));
    }

    @Test
    void shouldRestoreFactoryMapByDroppingUserTuning() {
        authenticateAs(user(5L, Role.STUDENT));
        EcuMapTuning tuning = EcuMapTuning.builder()
                .id(9L)
                .userId(5L)
                .motorId(1L)
                .mapType(EcuMapType.FUEL)
                .tunedValues(List.of(List.of(50)))
                .build();
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(new BigDecimal("1.8"), EcuMapType.FUEL))
                .thenReturn(Optional.of(factoryMap(EcuMapType.FUEL)));
        when(ecuMapTuningRepository.findByUserIdAndMotorIdAndMapType(5L, 1L, EcuMapType.FUEL))
                .thenReturn(Optional.of(tuning))
                .thenReturn(Optional.empty());

        EcuMapResponseDTO restoredMap = ecuMapService.restoreFactoryMap(1L, EcuMapType.FUEL);

        assertTrue(restoredMap.factoryDefault());
        verify(ecuMapTuningRepository).delete(tuning);
    }

    @Test
    void shouldKeyGenericDefaultsByDisplacementClass() {
        authenticateAs(user(5L, Role.STUDENT));
        when(motorRepository.findById(1L)).thenReturn(Optional.of(aspiratedMotor()));
        when(ecuMapDefaultRepository.findByDisplacementClassAndMapType(any(), any()))
                .thenReturn(Optional.empty());
        when(ecuMapDefaultRepository.save(any(EcuMapDefault.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<EcuMapResponseDTO> maps = ecuMapService.findMapsByMotor(1L);

        org.mockito.ArgumentCaptor<EcuMapDefault> savedDefaults =
                org.mockito.ArgumentCaptor.forClass(EcuMapDefault.class);
        verify(ecuMapDefaultRepository, org.mockito.Mockito.times(2)).save(savedDefaults.capture());
        assertTrue(savedDefaults.getAllValues().stream()
                .allMatch(savedDefault -> new BigDecimal("1.8").equals(savedDefault.getDisplacementClass())));
        assertEquals(new BigDecimal("1.8"), maps.get(0).displacementClass());
        assertEquals(new BigDecimal("1.8"), maps.get(1).displacementClass());
    }

    @Test
    void shouldMapAnyDisplacementToTheNearestGenericClass() {
        assertEquals(new BigDecimal("1.8"), EcuMapSpec.resolveDisplacementClass(new BigDecimal("1.781")));
        assertEquals(new BigDecimal("1.4"), EcuMapSpec.resolveDisplacementClass(new BigDecimal("1.390")));
        assertEquals(new BigDecimal("2.0"), EcuMapSpec.resolveDisplacementClass(new BigDecimal("1.984")));
        assertEquals(new BigDecimal("0.6"), EcuMapSpec.resolveDisplacementClass(new BigDecimal("0.16")));
        assertEquals(new BigDecimal("1.8"), EcuMapSpec.resolveDisplacementClass(null));
    }

    private boolean isInsideRange(List<List<Integer>> values, int minValue, int maxValue) {
        return values.stream()
                .flatMap(List::stream)
                .allMatch(value -> value >= minValue && value <= maxValue);
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

    private Motor forcedInductionMotor() {
        return Motor.builder()
                .id(1L)
                .displacementLiters(new BigDecimal("1.8"))
                .manufacturer("VOLKSWAGEN")
                .name("AP 1.8 Turbo")
                .compressionRatio(new BigDecimal("8.5"))
                .rpmCutoff(7000)
                .aspirationType("TURBO")
                .build();
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .fullName("Ana Lima")
                .email("ana@email.com")
                .passwordHash("hash")
                .role(role)
                .active(true)
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }
}
