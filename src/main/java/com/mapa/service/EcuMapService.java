package com.mapa.service;

import com.mapa.domain.EcuMapDefault;
import com.mapa.domain.EcuMapTuning;
import com.mapa.domain.Motor;
import com.mapa.domain.enums.EcuMapType;
import com.mapa.dto.EcuMapCellUpdateRequestDTO;
import com.mapa.dto.EcuMapResponseDTO;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.InvalidEcuMapValueException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.EcuMapDefaultRepository;
import com.mapa.repository.EcuMapTuningRepository;
import com.mapa.repository.MotorRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcuMapService {

    private final MotorRepository motorRepository;
    private final EcuMapDefaultRepository ecuMapDefaultRepository;
    private final EcuMapTuningRepository ecuMapTuningRepository;

    @Transactional
    // Entrega os mapas personalizados até o corte do motor.
    public List<EcuMapResponseDTO> findMapsByMotor(Long engineId) {
        Motor motor = findMotor(engineId);
        Long userId = currentPrincipal().getId();

        return Arrays.stream(EcuMapType.values())
                .map(mapType -> buildResponse(motor, mapType, userId))
                .toList();
    }

    @Transactional
    // Converte o índice da coluna em RPM antes de gravar o ajuste individual.
    public EcuMapResponseDTO updateCell(Long engineId, EcuMapType mapType, EcuMapCellUpdateRequestDTO cellUpdate) {
        Motor motor = findMotor(engineId);
        EcuMapSpec factorySpec = EcuMapSpec.forMotor(motor, mapType);
        EcuMapSpec spec = factorySpec.withRpmCutoff(motor.getRpmCutoff());
        validateCell(spec, cellUpdate);

        Long userId = currentPrincipal().getId();
        EcuMapDefault ecuMapDefault = ensureDefaultMap(motor, factorySpec);
        EcuMapTuning tuning = ecuMapTuningRepository
                .findByUserIdAndMotorIdAndMapType(userId, motor.getId(), mapType)
                .orElseGet(() -> EcuMapTuning.builder()
                        .userId(userId).motorId(motor.getId()).mapType(mapType).build());
        EcuMapRpmGrid factoryGrid = EcuMapRpmGrid.factory(ecuMapDefault);
        EcuMapRpmGrid sourceGrid = tuning.getTunedValues() == null ? factoryGrid : factoryGrid.withTuning(tuning);
        EcuMapRpmGrid updatedGrid = sourceGrid.editCell(
                cellUpdate.loadIndex(), spec.rpmBreakpoints().get(cellUpdate.rpmIndex()), cellUpdate.value());
        tuning.setRpmBreakpoints(updatedGrid.rpmBreakpoints());
        tuning.setTunedValues(updatedGrid.values());
        tuning.setUpdatedAt(OffsetDateTime.now());

        return buildResponse(motor, spec, ecuMapDefault, ecuMapTuningRepository.save(tuning));
    }

    @Transactional
    // Remove o ajuste individual para voltar ao mapa padrão.
    public EcuMapResponseDTO restoreFactoryMap(Long engineId, EcuMapType mapType) {
        Motor motor = findMotor(engineId);
        Long userId = currentPrincipal().getId();

        ecuMapTuningRepository.findByUserIdAndMotorIdAndMapType(userId, motor.getId(), mapType)
                .ifPresent(ecuMapTuningRepository::delete);

        return buildResponse(motor, mapType, userId);
    }

    /**
     * Apaga os ajustes feitos pelos usuários de um motor excluído.
     * Os mapas genéricos de fábrica são compartilhados entre motores da
     * mesma classe de litragem e permanecem no banco.
     */
    @Transactional
    public void deleteTuningsByMotor(Long engineId) {
        ecuMapTuningRepository.deleteByMotorId(engineId);
    }

    private EcuMapResponseDTO buildResponse(Motor motor, EcuMapType mapType, Long userId) {
        EcuMapSpec spec = EcuMapSpec.forMotor(motor, mapType);
        EcuMapDefault ecuMapDefault = ensureDefaultMap(motor, spec);
        EcuMapTuning tuning = ecuMapTuningRepository
                .findByUserIdAndMotorIdAndMapType(userId, motor.getId(), mapType)
                .orElse(null);

        return buildResponse(motor, spec.withRpmCutoff(motor.getRpmCutoff()), ecuMapDefault, tuning);
    }

    private EcuMapResponseDTO buildResponse(
            Motor motor, EcuMapSpec spec, EcuMapDefault ecuMapDefault, EcuMapTuning tuning) {
        EcuMapRpmGrid factoryGrid = EcuMapRpmGrid.factory(ecuMapDefault);
        EcuMapRpmGrid effectiveGrid = factoryGrid.withTuning(tuning);
        List<List<Integer>> factoryValues = factoryGrid.valuesAt(spec.rpmBreakpoints());
        boolean factoryDefault = effectiveGrid == factoryGrid;

        return new EcuMapResponseDTO(
                motor.getId(),
                resolveDisplacementClass(motor),
                spec.mapType().name(),
                spec.label(),
                spec.unit(),
                spec.rpmBreakpoints(),
                spec.loadBreakpoints(),
                factoryDefault ? factoryValues : effectiveGrid.valuesAt(spec.rpmBreakpoints()),
                factoryValues,
                spec.minValue(),
                spec.maxValue(),
                spec.step(),
                factoryDefault,
                factoryDefault ? null : tuning.getUpdatedAt());
    }

    private EcuMapDefault ensureDefaultMap(Motor motor, EcuMapSpec spec) {
        BigDecimal displacementClass = resolveDisplacementClass(motor);
        return ecuMapDefaultRepository
                .findByDisplacementClassAndMapType(displacementClass, spec.mapType())
                .orElseGet(() -> ecuMapDefaultRepository.save(newDefaultMap(displacementClass, spec)));
    }

    private EcuMapDefault newDefaultMap(BigDecimal displacementClass, EcuMapSpec spec) {
        return EcuMapDefault.builder()
                .displacementClass(displacementClass)
                .mapType(spec.mapType())
                .rpmBreakpoints(spec.rpmBreakpoints())
                .loadBreakpoints(spec.loadBreakpoints())
                .factoryValues(EcuMapFactory.generate(displacementClass, spec))
                .build();
    }

    private BigDecimal resolveDisplacementClass(Motor motor) {
        return EcuMapSpec.resolveDisplacementClass(motor.getDisplacementLiters());
    }

    private void validateCell(EcuMapSpec spec, EcuMapCellUpdateRequestDTO cellUpdate) {
        if (!spec.isValidLoadIndex(cellUpdate.loadIndex())) {
            throw new InvalidEcuMapValueException("Índice de carga inválido para o " + spec.label().toLowerCase());
        }

        if (!spec.isValidRpmIndex(cellUpdate.rpmIndex())) {
            throw new InvalidEcuMapValueException("Índice de RPM inválido para o " + spec.label().toLowerCase());
        }

        if (!spec.isValidValue(cellUpdate.value())) {
            throw new InvalidEcuMapValueException(String.format(
                    "Valor %d fora do intervalo permitido para o %s: informe um valor entre %d e %d (incremento de %d).",
                    cellUpdate.value(),
                    spec.label().toLowerCase(),
                    spec.minValue(),
                    spec.maxValue(),
                    spec.step()));
        }
    }

    private Motor findMotor(Long engineId) {
        return motorRepository.findById(engineId)
                .orElseThrow(() -> new ResourceNotFoundException("Motor não encontrado com id: " + engineId));
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        return principal;
    }
}
