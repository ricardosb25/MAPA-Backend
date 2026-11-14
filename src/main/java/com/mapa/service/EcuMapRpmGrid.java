package com.mapa.service;

import com.mapa.domain.EcuMapDefault;
import com.mapa.domain.EcuMapTuning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

record EcuMapRpmGrid(List<Integer> rpmBreakpoints, List<List<Integer>> values) {
    static EcuMapRpmGrid factory(EcuMapDefault factoryMap) {
        return new EcuMapRpmGrid(factoryMap.getRpmBreakpoints(), factoryMap.getFactoryValues());
    }

    EcuMapRpmGrid withTuning(EcuMapTuning tuning) {
        if (tuning == null) {
            return this;
        }
        List<Integer> tuningRpm = tuning.getRpmBreakpoints();
        // Legacy tunings stored values using the shared factory RPM axis.
        if (tuningRpm == null || tuningRpm.isEmpty()) {
            tuningRpm = rpmBreakpoints;
        }
        int expectedColumns = tuningRpm.size();
        List<List<Integer>> tunedValues = tuning.getTunedValues();
        if (tunedValues.size() != values.size()
                || tunedValues.stream().anyMatch(row -> row.size() != expectedColumns)) {
            return this;
        }
        return new EcuMapRpmGrid(tuningRpm, tunedValues);
    }

    List<List<Integer>> valuesAt(List<Integer> targetRpm) {
        return values.stream()
                .map(row -> targetRpm.stream().map(rpm -> valueAt(row, rpm)).toList())
                .toList();
    }

    EcuMapRpmGrid editCell(int loadIndex, int rpm, int value) {
        List<Integer> updatedRpm = new ArrayList<>(rpmBreakpoints);
        int position = Collections.binarySearch(updatedRpm, rpm);
        if (position < 0) {
            position = -position - 1;
            updatedRpm.add(position, rpm);
        }
        List<List<Integer>> updatedValues = new ArrayList<>(valuesAt(updatedRpm));
        List<Integer> updatedRow = new ArrayList<>(updatedValues.get(loadIndex));
        updatedRow.set(position, value);
        updatedValues.set(loadIndex, updatedRow);
        return new EcuMapRpmGrid(updatedRpm, updatedValues);
    }

    private int valueAt(List<Integer> row, int rpm) {
        int position = Collections.binarySearch(rpmBreakpoints, rpm);
        if (position >= 0) {
            return row.get(position);
        }
        int upperIndex = -position - 1;
        if (upperIndex == 0) {
            return row.getFirst();
        }
        if (upperIndex == rpmBreakpoints.size()) {
            return row.getLast();
        }
        int lowerIndex = upperIndex - 1;
        int lowerRpm = rpmBreakpoints.get(lowerIndex);
        double fraction = (double) (rpm - lowerRpm) / (rpmBreakpoints.get(upperIndex) - lowerRpm);
        return (int) Math.round(row.get(lowerIndex) + fraction * (row.get(upperIndex) - row.get(lowerIndex)));
    }
}
