package com.mapa.service;

import com.mapa.domain.Motor;
import com.mapa.domain.enums.EcuMapType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

 //Define os eixos genéricos e os limites realistas de uma ECU automotiva.

public record EcuMapSpec(
        EcuMapType mapType,
        String label,
        String unit,
        int minValue,
        int maxValue,
        int step,
        List<Integer> rpmBreakpoints,
        List<Integer> loadBreakpoints) {

    private static final List<Integer> GENERIC_RPM_BREAKPOINTS =
            List.of(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000);
    private static final List<Integer> LOAD_BREAKPOINTS = List.of(100, 85, 70, 55, 40, 25);

    private static final List<BigDecimal> DISPLACEMENT_CLASSES = List.of(
            new BigDecimal("0.6"),
            new BigDecimal("1.0"),
            new BigDecimal("1.2"),
            new BigDecimal("1.4"),
            new BigDecimal("1.6"),
            new BigDecimal("1.8"),
            new BigDecimal("2.0"),
            new BigDecimal("2.5"),
            new BigDecimal("3.0"),
            new BigDecimal("4.0"));
    private static final BigDecimal FALLBACK_DISPLACEMENT_CLASS = new BigDecimal("1.8");

    private static final int IGNITION_MIN_DEGREES = 0;
    private static final int IGNITION_MAX_DEGREES_ASPIRATED = 45;
    private static final int IGNITION_MAX_DEGREES_FORCED = 35;
    private static final int FUEL_MIN_VE_PERCENT = 10;
    private static final int FUEL_MAX_VE_PERCENT_ASPIRATED = 100;
    private static final int FUEL_MAX_VE_PERCENT_FORCED = 150;

    public static EcuMapSpec forMotor(Motor motor, EcuMapType mapType) {
        boolean forcedInduction = isForcedInduction(motor);

        return switch (mapType) {
            case IGNITION -> new EcuMapSpec(
                    mapType,
                    mapType.getLabel(),
                    mapType.getUnit(),
                    IGNITION_MIN_DEGREES,
                    forcedInduction ? IGNITION_MAX_DEGREES_FORCED : IGNITION_MAX_DEGREES_ASPIRATED,
                    1,
                    GENERIC_RPM_BREAKPOINTS,
                    LOAD_BREAKPOINTS);
            case FUEL -> new EcuMapSpec(
                    mapType,
                    mapType.getLabel(),
                    mapType.getUnit(),
                    FUEL_MIN_VE_PERCENT,
                    forcedInduction ? FUEL_MAX_VE_PERCENT_FORCED : FUEL_MAX_VE_PERCENT_ASPIRATED,
                    1,
                    GENERIC_RPM_BREAKPOINTS,
                    LOAD_BREAKPOINTS);
        };
    }

    /**
     * Classe de litragem aproximada usada para selecionar o mapa genérico de fábrica.
     * Ex.: 1.781 L cai na classe 1.8; 1.390 L cai na classe 1.4.
     */
    public static BigDecimal resolveDisplacementClass(BigDecimal displacementLiters) {
        if (displacementLiters == null) {
            return FALLBACK_DISPLACEMENT_CLASS;
        }

        BigDecimal nearestClass = DISPLACEMENT_CLASSES.get(0);
        BigDecimal smallestDistance = distanceTo(nearestClass, displacementLiters);

        for (BigDecimal candidate : DISPLACEMENT_CLASSES) {
            BigDecimal distance = distanceTo(candidate, displacementLiters);
            if (distance.compareTo(smallestDistance) < 0) {
                smallestDistance = distance;
                nearestClass = candidate;
            }
        }

        return nearestClass;
    }

    public static boolean isForcedInduction(Motor motor) {
        String aspiration = motor.getAspirationType() == null
                ? ""
                : motor.getAspirationType().trim().toUpperCase(Locale.ROOT);
        return "TURBO".equals(aspiration) || "SUPERCHARGER".equals(aspiration);
    }

    public boolean isValidValue(int value) {
        return value >= minValue
                && value <= maxValue
                && value % step == 0;
    }

    public boolean isValidLoadIndex(int loadIndex) {
        return loadIndex >= 0 && loadIndex < loadBreakpoints.size();
    }

    public boolean isValidRpmIndex(int rpmIndex) {
        return rpmIndex >= 0 && rpmIndex < rpmBreakpoints.size();
    }

    private static BigDecimal distanceTo(BigDecimal candidate, BigDecimal displacementLiters) {
        return candidate.subtract(displacementLiters).abs();
    }
}