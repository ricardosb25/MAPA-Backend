package com.mapa.service;

import com.mapa.domain.enums.EcuMapType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

 //Gera os mapas genéricos de fábrica a partir da classe de litragem do motor.

public final class EcuMapFactory {

    private static final int IGNITION_BASE_DEGREES = 8;
    private static final double IGNITION_RPM_FACTOR = 1.5;
    private static final double IGNITION_LOAD_PENALTY = 0.10;
    private static final double IGNITION_CLASS_FACTOR = 2.0;
    private static final int IGNITION_CLASS_BONUS_MAX = 4;

    private static final int FUEL_BASE_VE = 35;
    private static final int FUEL_LOAD_RANGE = 75;
    private static final int FUEL_LOAD_AMPLITUDE = 45;
    private static final double FUEL_PEAK_RPM_RATIO = 0.6;
    private static final double FUEL_CURVE_WEIGHT = 1.6;
    private static final double FUEL_CLASS_FACTOR = 4.0;
    private static final double FUEL_CLASS_ADJUSTMENT_LIMIT = 6.0;

    private static final double REFERENCE_DISPLACEMENT_LITERS = 1.8;
    private static final int LOAD_REFERENCE_MIN = 25;

    private EcuMapFactory() {
    }

    public static List<List<Integer>> generate(BigDecimal displacementClass, EcuMapSpec spec) {
        return spec.mapType() == EcuMapType.IGNITION
                ? generateIgnitionMap(displacementClass, spec)
                : generateFuelMap(displacementClass, spec);
    }

    private static List<List<Integer>> generateIgnitionMap(BigDecimal displacementClass, EcuMapSpec spec) {
        int classBonus = ignitionClassBonus(displacementClass);
        List<List<Integer>> grid = new ArrayList<>();

        for (int loadIndex = 0; loadIndex < spec.loadBreakpoints().size(); loadIndex++) {
            int load = spec.loadBreakpoints().get(loadIndex);
            List<Integer> row = new ArrayList<>();

            for (int rpmIndex = 0; rpmIndex < spec.rpmBreakpoints().size(); rpmIndex++) {
                int rpm = spec.rpmBreakpoints().get(rpmIndex);
                double advance = IGNITION_BASE_DEGREES
                        + (rpm / 1000.0) * IGNITION_RPM_FACTOR
                        + classBonus
                        - (load - LOAD_REFERENCE_MIN) * IGNITION_LOAD_PENALTY;

                row.add(clamp(Math.round(advance), spec));
            }

            grid.add(row);
        }

        return grid;
    }

    private static List<List<Integer>> generateFuelMap(BigDecimal displacementClass, EcuMapSpec spec) {
        double classAdjustment = fuelClassAdjustment(displacementClass);
        double referenceRpm = spec.rpmBreakpoints().get(spec.rpmBreakpoints().size() - 1);
        List<List<Integer>> grid = new ArrayList<>();

        for (int loadIndex = 0; loadIndex < spec.loadBreakpoints().size(); loadIndex++) {
            int load = spec.loadBreakpoints().get(loadIndex);
            double loadFactor = (double) (load - LOAD_REFERENCE_MIN) / FUEL_LOAD_RANGE;
            List<Integer> row = new ArrayList<>();

            for (int rpmIndex = 0; rpmIndex < spec.rpmBreakpoints().size(); rpmIndex++) {
                int rpm = spec.rpmBreakpoints().get(rpmIndex);
                double rpmRatio = rpm / referenceRpm;
                double rpmCurve = 1.0 - FUEL_CURVE_WEIGHT * Math.pow(rpmRatio - FUEL_PEAK_RPM_RATIO, 2);
                double base = FUEL_BASE_VE + FUEL_LOAD_AMPLITUDE * loadFactor;

                double volumetricEfficiency = base * Math.max(rpmCurve, 0.4) + classAdjustment;
                row.add(clamp((long) Math.round(volumetricEfficiency), spec));
            }

            grid.add(row);
        }

        return grid;
    }

    private static int ignitionClassBonus(BigDecimal displacementClass) {
        long rawBonus = Math.round(
                (REFERENCE_DISPLACEMENT_LITERS - displacementClass.doubleValue()) * IGNITION_CLASS_FACTOR);
        return (int) Math.min(Math.max(rawBonus, 0), IGNITION_CLASS_BONUS_MAX);
    }

    private static double fuelClassAdjustment(BigDecimal displacementClass) {
        double rawAdjustment =
                (REFERENCE_DISPLACEMENT_LITERS - displacementClass.doubleValue()) * FUEL_CLASS_FACTOR;
        return Math.min(Math.max(rawAdjustment, -FUEL_CLASS_ADJUSTMENT_LIMIT), FUEL_CLASS_ADJUSTMENT_LIMIT);
    }

    private static int clamp(long value, EcuMapSpec spec) {
        return (int) Math.min(Math.max(value, spec.minValue()), spec.maxValue());
    }
}