package com.mapa.service;

import com.mapa.domain.DynamometerTest;
import com.mapa.domain.EcuMapDefault;
import com.mapa.domain.EcuMapTuning;
import com.mapa.domain.Motor;
import com.mapa.domain.User;
import com.mapa.domain.enums.EcuMapType;
import com.mapa.dto.DynoResultDTO;
import com.mapa.dto.DynoTestResponseDTO;
import com.mapa.exception.ActiveMotorNotSelectedException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.DynamometerTestRepository;
import com.mapa.repository.EcuMapDefaultRepository;
import com.mapa.repository.EcuMapTuningRepository;
import com.mapa.repository.MotorRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DynamometerPhysicsService {

    private final UserRepository userRepository;
    private final MotorRepository motorRepository;
    private final EcuMapDefaultRepository ecuMapDefaultRepository;
    private final EcuMapTuningRepository ecuMapTuningRepository;
    private final DynamometerTestRepository dynamometerTestRepository;
    private final ChallengeService challengeService;

    @Transactional
    // Reúne o motor ativo e os dois mapas, executa a puxada e registra o resultado e o progresso.
    public DynoResultDTO runSimulation() {
        User user = currentUser();
        Motor motor = findActiveMotor(user);
        Long userId = user.getId();

        EcuMapSpec fuelSpec = EcuMapSpec.forMotor(motor, EcuMapType.FUEL).withRpmCutoff(motor.getRpmCutoff());
        List<Integer> rpmBreakpoints = fuelSpec.rpmBreakpoints();
        List<List<Integer>> fuelMap = effectiveMap(userId, motor, EcuMapType.FUEL, rpmBreakpoints);
        List<List<Integer>> ignitionMap = effectiveMap(userId, motor, EcuMapType.IGNITION, rpmBreakpoints);

        DynamometerPhysics.SimulationInput input = new DynamometerPhysics.SimulationInput(
                motor.getDisplacementLiters().doubleValue(),
                motor.getCompressionRatio().doubleValue(),
                motor.getRpmCutoff(),
                EcuMapSpec.isForcedInduction(motor),
                rpmBreakpoints,
                fuelSpec.loadBreakpoints(),
                fuelMap,
                ignitionMap);

        DynamometerPhysics.SimulationResult result = DynamometerPhysics.run(input);

        DynamometerTest dynamometerTest = dynamometerTestRepository.save(DynamometerTest.builder()
                .userId(userId)
                .motorId(motor.getId())
                .maxPower(result.peakPower())
                .maxTorque(result.peakTorque())
                .averageAfr(result.averageAfr())
                .engineStatus(result.finalStatus())
                .build());

        challengeService.registerSimulationAttempts(user, motor, dynamometerTest);

        return new DynoResultDTO(
                result.rpmAxis(),
                result.powerCurve(),
                result.torqueCurve(),
                result.afrCurve(),
                result.finalStatus(),
                result.peakPower(),
                result.peakTorque());
    }

    @Transactional(readOnly = true)
    public List<DynoTestResponseDTO> findHistory() {
        return dynamometerTestRepository.findByUserIdOrderByTestedAtDesc(currentUser().getId())
                .stream()
                .map(DynoTestResponseDTO::fromEntity)
                .toList();
    }

    private List<List<Integer>> effectiveMap(Long userId, Motor motor, EcuMapType mapType, List<Integer> rpmBreakpoints) {
        EcuMapSpec spec = EcuMapSpec.forMotor(motor, mapType);
        BigDecimal displacementClass = EcuMapSpec.resolveDisplacementClass(motor.getDisplacementLiters());

        EcuMapDefault defaultMap = ecuMapDefaultRepository
                .findByDisplacementClassAndMapType(displacementClass, mapType)
                .orElseGet(() -> ecuMapDefaultRepository.save(newDefaultMap(displacementClass, spec)));

        EcuMapTuning tuning = ecuMapTuningRepository
                .findByUserIdAndMotorIdAndMapType(userId, motor.getId(), mapType)
                .orElse(null);

        return EcuMapRpmGrid.factory(defaultMap).withTuning(tuning).valuesAt(rpmBreakpoints);
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

    private Motor findActiveMotor(User user) {
        if (user.getActiveMotorId() == null) {
            throw new ActiveMotorNotSelectedException();
        }

        return motorRepository.findById(user.getActiveMotorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Motor ativo não encontrado com id: " + user.getActiveMotorId()));
    }

    private User currentUser() {
        UserPrincipal principal = currentPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + principal.getId()));
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        return principal;
    }
}
