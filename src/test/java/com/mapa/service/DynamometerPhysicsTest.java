package com.mapa.service;

import com.mapa.domain.Motor;
import com.mapa.domain.enums.EcuMapType;
import com.mapa.domain.enums.EngineStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamometerPhysicsTest {

    @Test
    void shouldKeepEngineIntactWhenRunningFactoryMaps() {
        Motor motor = aspiratedMotor();
        DynamometerPhysics.SimulationResult result =
                DynamometerPhysics.run(factoryInput(motor, motor.getRpmCutoff()));

        assertEquals(EngineStatus.INTEGRO, result.finalStatus());
        assertFalse(result.rpmAxis().isEmpty());
        assertEquals(23, result.rpmAxis().size());
        assertTrue(result.peakPower() > 80.0 && result.peakPower() < 220.0);
        assertTrue(result.peakTorque() > 10.0);
        result.afrCurve().forEach(afr -> assertTrue(afr > 12.0 && afr < 16.0));
    }

    @Test
    void shouldCalculatePowerFromTorqueAndRpm() {
        Motor motor = aspiratedMotor();
        DynamometerPhysics.SimulationResult result =
                DynamometerPhysics.run(factoryInput(motor, motor.getRpmCutoff()));

        for (int index = 0; index < result.rpmAxis().size(); index++) {
            double expectedPower = result.torqueCurve().get(index) * result.rpmAxis().get(index) / 716.2;
            assertEquals(expectedPower, result.powerCurve().get(index), 0.01);
        }
    }

    @Test
    void shouldBreakEngineWhenMixtureIsExcessivelyLean() {
        Motor motor = aspiratedMotor();
        DynamometerPhysics.SimulationInput factory = factoryInput(motor, motor.getRpmCutoff());
        DynamometerPhysics.SimulationInput leanInput = new DynamometerPhysics.SimulationInput(
                factory.displacementLiters(),
                factory.compressionRatio(),
                factory.rpmCutoff(),
                factory.forcedInduction(),
                factory.rpmBreakpoints(),
                factory.loadBreakpoints(),
                constantGrid(factory.loadBreakpoints().size(), factory.rpmBreakpoints().size(), 10),
                factory.ignitionGrid());

        DynamometerPhysics.SimulationResult result = DynamometerPhysics.run(leanInput);

        assertEquals(EngineStatus.QUEBRADO_POBRE, result.finalStatus());
        assertTrue(result.rpmAxis().size() < 23);
    }

    @Test
    void shouldBreakEngineWhenIgnitionAdvanceExceedsKnockLimit() {
        Motor motor = highCompressionMotor();
        DynamometerPhysics.SimulationInput factory = factoryInput(motor, motor.getRpmCutoff());
        DynamometerPhysics.SimulationInput excessiveAdvanceInput = new DynamometerPhysics.SimulationInput(
                factory.displacementLiters(),
                factory.compressionRatio(),
                factory.rpmCutoff(),
                factory.forcedInduction(),
                factory.rpmBreakpoints(),
                factory.loadBreakpoints(),
                factory.fuelGrid(),
                constantGrid(factory.loadBreakpoints().size(), factory.rpmBreakpoints().size(), 45));

        DynamometerPhysics.SimulationResult result = DynamometerPhysics.run(excessiveAdvanceInput);

        assertEquals(EngineStatus.QUEBRADO_DETONACAO, result.finalStatus());
    }

    @Test
    void shouldFlagOverRevWhenCutoffExceedsPhysicalTolerance() {
        Motor motor = aspiratedMotor();
        DynamometerPhysics.SimulationResult result = DynamometerPhysics.run(factoryInput(motor, 12000));

        assertEquals(EngineStatus.OVERREV, result.finalStatus());
        assertTrue(result.rpmAxis().get(result.rpmAxis().size() - 1) <= 8670);
    }

    @Test
    void shouldProduceMoreTorqueInForcedInductionEngine() {
        Motor aspirated = aspiratedMotor();
        Motor turbo = forcedInductionMotor();

        DynamometerPhysics.SimulationResult aspiratedResult =
                DynamometerPhysics.run(factoryInput(aspirated, aspirated.getRpmCutoff()));
        DynamometerPhysics.SimulationResult turboResult =
                DynamometerPhysics.run(factoryInput(turbo, turbo.getRpmCutoff()));

        assertEquals(EngineStatus.INTEGRO, turboResult.finalStatus());
        assertTrue(turboResult.peakTorque() > aspiratedResult.peakTorque());
    }

    private DynamometerPhysics.SimulationInput factoryInput(Motor motor, int rpmCutoff) {
        EcuMapSpec fuelSpec = EcuMapSpec.forMotor(motor, EcuMapType.FUEL);
        EcuMapSpec ignitionSpec = EcuMapSpec.forMotor(motor, EcuMapType.IGNITION);
        BigDecimal displacementClass = EcuMapSpec.resolveDisplacementClass(motor.getDisplacementLiters());

        return new DynamometerPhysics.SimulationInput(
                motor.getDisplacementLiters().doubleValue(),
                motor.getCompressionRatio().doubleValue(),
                rpmCutoff,
                EcuMapSpec.isForcedInduction(motor),
                fuelSpec.rpmBreakpoints(),
                fuelSpec.loadBreakpoints(),
                EcuMapFactory.generate(displacementClass, fuelSpec),
                EcuMapFactory.generate(displacementClass, ignitionSpec));
    }

    private List<List<Integer>> constantGrid(int loadCount, int rpmCount, int value) {
        List<List<Integer>> grid = new ArrayList<>();
        for (int loadIndex = 0; loadIndex < loadCount; loadIndex++) {
            grid.add(new ArrayList<>(Collections.nCopies(rpmCount, value)));
        }
        return grid;
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

    private Motor highCompressionMotor() {
        return Motor.builder()
                .id(2L)
                .displacementLiters(new BigDecimal("1.8"))
                .manufacturer("VOLKSWAGEN")
                .name("AP 1.8 8V High Compression")
                .compressionRatio(new BigDecimal("10.5"))
                .rpmCutoff(6500)
                .aspirationType("ASPIRADO")
                .build();
    }

    private Motor forcedInductionMotor() {
        return Motor.builder()
                .id(3L)
                .displacementLiters(new BigDecimal("1.8"))
                .manufacturer("VOLKSWAGEN")
                .name("AP 1.8 Turbo")
                .compressionRatio(new BigDecimal("8.5"))
                .rpmCutoff(6800)
                .aspirationType("TURBO")
                .build();
    }
}
