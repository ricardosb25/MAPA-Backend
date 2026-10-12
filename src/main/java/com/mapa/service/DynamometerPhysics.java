package com.mapa.service;

import com.mapa.domain.enums.EngineStatus;

import java.util.ArrayList;
import java.util.List;

//Modelo matemático puro do Dinamômetro Virtual 
public final class DynamometerPhysics {

    private static final double STOICHIOMETRIC_AFR = 14.7;
    private static final double AIR_GRAMS_PER_LITER = 1.18;
    private static final double PRESSURE_FACTOR_ASPIRATED = 1.0;
    private static final double PRESSURE_FACTOR_FORCED = 1.35;
    private static final double TORQUE_PER_GRAM_AIR = 9.2;
    private static final double POWER_CONSTANT_KGFM = 716.2;
    private static final int RPM_STEP = 250;
    private static final int MIN_RPM = 1000;

    private static final double AIR_VE_BASE = 76.0;
    private static final double AIR_VE_CURVATURE = 1.75;
    private static final double AIR_VE_PEAK_RATIO = 0.62;
    private static final double AIR_VE_FLOOR_RATIO = 0.4;

    private static final double HIGH_RPM_FRICTION_START_RATIO = 0.7;
    private static final double HIGH_RPM_FRICTION_FACTOR = 0.15;

    private static final double MBT_BASE_DEGREES = 14.0;
    private static final double MBT_COMPRESSION_FACTOR = 1.2;
    private static final double MBT_RPM_FACTOR = 0.6;
    private static final double COMBUSTION_EFFICIENCY_PENALTY = 0.0035;
    private static final double COMBUSTION_EFFICIENCY_MIN = 0.25;

    private static final double KNOCK_BASE_LIMIT_DEGREES = 32.0;
    private static final double KNOCK_COMPRESSION_FACTOR = 2.0;
    private static final double KNOCK_RPM_FACTOR = 0.5;
    private static final double HIGH_LOAD_THRESHOLD_PERCENT = 70.0;
    private static final double LEAN_AFR_LIMIT = 16.0;

    private static final double PHYSICAL_RPM_BASE = 10200.0;
    private static final double PHYSICAL_RPM_DISPLACEMENT_FACTOR = 850.0;

    private static final double AFR_OUTPUT_MIN = 5.0;
    private static final double AFR_OUTPUT_MAX = 30.0;

    private DynamometerPhysics() {
    }

    /**
     * Parâmetros físicos do motor base e das matrizes efetivas (fábrica ou ajustadas pelo usuário).
     */
    public record SimulationInput(
            double displacementLiters,
            double compressionRatio,
            int rpmCutoff,
            boolean forcedInduction,
            List<Integer> rpmBreakpoints,
            List<Integer> loadBreakpoints,
            List<List<Integer>> fuelGrid,
            List<List<Integer>> ignitionGrid) {
    }

    /**
     * Telemetria da puxada em arrays paralelos, pronta para plotagem dos gráficos.
     */
    public record SimulationResult(
            List<Integer> rpmAxis,
            List<Double> powerCurve,
            List<Double> torqueCurve,
            List<Double> afrCurve,
            EngineStatus finalStatus,
            double peakPower,
            double peakTorque,
            double averageAfr) {
    }

    public static SimulationResult run(SimulationInput input) {
        int startRpm = input.rpmBreakpoints().get(0);
        double fullLoad = input.loadBreakpoints().get(0);
        int physicalRpmLimit = physicalRpmLimit(input.displacementLiters());

        List<Integer> rpmAxis = new ArrayList<>();
        List<Double> powerCurve = new ArrayList<>();
        List<Double> torqueCurve = new ArrayList<>();
        List<Double> afrCurve = new ArrayList<>();

        EngineStatus finalStatus = EngineStatus.INTEGRO;
        int lastRpm = Math.max(input.rpmCutoff(), startRpm);

        for (int rpm = startRpm; rpm <= lastRpm; rpm += RPM_STEP) {
            if (rpm > physicalRpmLimit) {
                finalStatus = EngineStatus.OVERREV;
                break;
            }

            double airVolumetricEfficiency = airVolumetricEfficiency(rpm, input.rpmBreakpoints());
            double fuelVolumetricEfficiency = interpolate(
                    input.fuelGrid(), rpm, fullLoad, input.rpmBreakpoints(), input.loadBreakpoints());
            double advanceDegrees = interpolate(
                    input.ignitionGrid(), rpm, fullLoad, input.rpmBreakpoints(), input.loadBreakpoints());

            double afr = calculateAFR(airVolumetricEfficiency, fuelVolumetricEfficiency);
            double torque = calculateTorque(input, rpm, airVolumetricEfficiency, advanceDegrees);
            double power = calculatePower(torque, rpm);

            rpmAxis.add(rpm);
            torqueCurve.add(torque);
            powerCurve.add(power);
            afrCurve.add(afr);

            finalStatus = checkEngineHealth(afr, advanceDegrees, rpm, fullLoad, input.compressionRatio());
            if (finalStatus != EngineStatus.INTEGRO) {
                break;
            }
        }

        return new SimulationResult(
                rpmAxis,
                powerCurve,
                torqueCurve,
                afrCurve,
                finalStatus,
                peakOf(powerCurve),
                peakOf(torqueCurve),
                averageOf(afrCurve));
    }

    /**
     * Eficiência volumétrica real do motor em função da rotação, com pico por volta de 60% da faixa útil.
     */
    private static double airVolumetricEfficiency(int rpm, List<Integer> rpmBreakpoints) {
        double rpmRatio = rpm / (double) rpmBreakpoints.get(rpmBreakpoints.size() - 1);
        double curve = 1.0 - AIR_VE_CURVATURE * Math.pow(rpmRatio - AIR_VE_PEAK_RATIO, 2);
        return AIR_VE_BASE * Math.max(AIR_VE_FLOOR_RATIO, curve);
    }

    /**
     * Torque (kgfm): massa de ar admitida × constante de torque × eficiência de combustão × atritos.
     */
    private static double calculateTorque(
            SimulationInput input, int rpm, double airVolumetricEfficiency, double advanceDegrees) {

        double pressureFactor = input.forcedInduction()
                ? PRESSURE_FACTOR_FORCED
                : PRESSURE_FACTOR_ASPIRATED;
        double airMassGrams = input.displacementLiters()
                * AIR_GRAMS_PER_LITER
                * (airVolumetricEfficiency / 100.0)
                * pressureFactor;

        double combustionEfficiency = combustionEfficiency(
                advanceDegrees, mbtAdvanceDegrees(input.compressionRatio(), rpm));
        double frictionFactor = 1.0
                - HIGH_RPM_FRICTION_FACTOR * highRpmFrictionRatio(rpm, input.rpmBreakpoints());

        return airMassGrams * TORQUE_PER_GRAM_AIR * combustionEfficiency * frictionFactor;
    }

    /**
     * Potência (cv) = Torque (kgfm) × RPM / 716,2.
     */
    private static double calculatePower(double torqueKgfm, int rpm) {
        return torqueKgfm * rpm / POWER_CONSTANT_KGFM;
    }

    /**
     * AFR = massa de ar / massa de combustível, expressa em função das duas curvas de VE.
     * Quando o mapa entrega combustível igual à estimativa de ar, a mistura fica estequiométrica.
     */
    private static double calculateAFR(double airVolumetricEfficiency, double fuelVolumetricEfficiency) {
        if (fuelVolumetricEfficiency <= 0) {
            return AFR_OUTPUT_MAX;
        }
        double afr = STOICHIOMETRIC_AFR * airVolumetricEfficiency / fuelVolumetricEfficiency;
        return Math.min(AFR_OUTPUT_MAX, Math.max(AFR_OUTPUT_MIN, afr));
    }

    /**
     * Ponto de melhor torque (MBT): avanço que maximiza o torque para a compressão e RPM atuais.
     */
    private static double mbtAdvanceDegrees(double compressionRatio, int rpm) {
        return MBT_BASE_DEGREES
                + (compressionRatio - 9.0) * MBT_COMPRESSION_FACTOR
                + ((rpm - MIN_RPM) / 1000.0) * MBT_RPM_FACTOR;
    }

    /**
     * Eficiência de combustão decai quadraticamente quando o avanço se afasta do MBT.
     */
    private static double combustionEfficiency(double advanceDegrees, double mbtDegrees) {
        double deviation = advanceDegrees - mbtDegrees;
        double efficiency = 1.0 - COMBUSTION_EFFICIENCY_PENALTY * deviation * deviation;
        return Math.max(COMBUSTION_EFFICIENCY_MIN, Math.min(1.0, efficiency));
    }

    /**
     * Avanço máximo seguro para a taxa de compressão e RPM atuais; acima disso há risco de detonação.
     */
    private static double knockLimitDegrees(double compressionRatio, int rpm) {
        return KNOCK_BASE_LIMIT_DEGREES
                - (compressionRatio - 9.0) * KNOCK_COMPRESSION_FACTOR
                + ((rpm - 2000) / 1000.0) * KNOCK_RPM_FACTOR;
    }

    /**
     * Perda de torque por atritos e bombeamento em altas rotações.
     */
    private static double highRpmFrictionRatio(int rpm, List<Integer> rpmBreakpoints) {
        double rpmRatio = rpm / (double) rpmBreakpoints.get(rpmBreakpoints.size() - 1);
        double overRatio = (rpmRatio - HIGH_RPM_FRICTION_START_RATIO) / (1.0 - HIGH_RPM_FRICTION_START_RATIO);
        return Math.max(0.0, Math.min(1.0, overRatio));
    }

    /**
     * Tolerância física das peças (flutuação de válvulas): motores menores giram mais.
     */
    private static int physicalRpmLimit(double displacementLiters) {
        return (int) Math.round(PHYSICAL_RPM_BASE - displacementLiters * PHYSICAL_RPM_DISPLACEMENT_FACTOR);
    }

    private static EngineStatus checkEngineHealth(
            double afr, double advanceDegrees, int rpm, double loadPercent, double compressionRatio) {

        if (loadPercent >= HIGH_LOAD_THRESHOLD_PERCENT && afr > LEAN_AFR_LIMIT) {
            return EngineStatus.QUEBRADO_POBRE;
        }

        if (loadPercent >= HIGH_LOAD_THRESHOLD_PERCENT
                && advanceDegrees > knockLimitDegrees(compressionRatio, rpm)) {
            return EngineStatus.QUEBRADO_DETONACAO;
        }

        return EngineStatus.INTEGRO;
    }



    /**
     * Interpolação bilinear da matriz (carga × RPM) para um ponto arbitrário da puxada.
     */
    private static double interpolate(
            List<List<Integer>> grid, double rpm, double load,
            List<Integer> rpmBreakpoints, List<Integer> loadBreakpoints) {

        int rpmSegment = segmentIndex(rpmBreakpoints, rpm);
        int loadSegment = segmentIndex(loadBreakpoints, load);
        double rpmFraction = segmentFraction(rpmBreakpoints, rpmSegment, rpm);
        double loadFraction = segmentFraction(loadBreakpoints, loadSegment, load);

        double lowerLoadValue = lerp(grid, rpmSegment, loadSegment, rpmFraction);
        double upperLoadValue = lerp(grid, rpmSegment, loadSegment + 1, rpmFraction);
        return lowerLoadValue + (upperLoadValue - lowerLoadValue) * loadFraction;
    }

    private static double lerp(List<List<Integer>> grid, int rpmSegment, int loadRow, double rpmFraction) {
        double startValue = grid.get(loadRow).get(rpmSegment);
        double endValue = grid.get(loadRow).get(rpmSegment + 1);
        return startValue + (endValue - startValue) * rpmFraction;
    }

    private static int segmentIndex(List<Integer> breakpoints, double value) {
        for (int index = 0; index < breakpoints.size() - 1; index++) {
            double current = breakpoints.get(index);
            double next = breakpoints.get(index + 1);
            if (value >= Math.min(current, next) && value <= Math.max(current, next)) {
                return index;
            }
        }
        return Math.max(0, breakpoints.size() - 2);
    }

    private static double segmentFraction(List<Integer> breakpoints, int segment, double value) {
        double start = breakpoints.get(segment);
        double end = breakpoints.get(segment + 1);
        if (start == end) {
            return 0.0;
        }
        double fraction = (value - start) / (end - start);
        return Math.min(1.0, Math.max(0.0, fraction));
    }

    private static double peakOf(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private static double averageOf(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
