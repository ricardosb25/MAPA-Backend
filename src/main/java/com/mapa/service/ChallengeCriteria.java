package com.mapa.service;

import com.mapa.domain.enums.EngineStatus;

import java.util.List;

public final class ChallengeCriteria {

    private ChallengeCriteria() {
    }

    public static List<String> labels(int goalPowerCv, EngineStatus requiredEngineStatus) {
        return List.of(
                "Atingir " + goalPowerCv + " cv de pico",
                "Status final: " + requiredStatus(requiredEngineStatus).getLabel());
    }

    public static int completedCount(
            int goalPowerCv,
            EngineStatus requiredEngineStatus,
            Double lastPeakPower,
            EngineStatus lastEngineStatus) {

        if (lastPeakPower == null || lastEngineStatus == null) {
            return 0;
        }

        int completedCriteria = 0;
        if (lastPeakPower >= goalPowerCv) {
            completedCriteria++;
        }
        if (lastEngineStatus == requiredStatus(requiredEngineStatus)) {
            completedCriteria++;
        }
        return completedCriteria;
    }

    public static boolean isCompleted(
            int goalPowerCv,
            EngineStatus requiredEngineStatus,
            Double lastPeakPower,
            EngineStatus lastEngineStatus) {

        return completedCount(goalPowerCv, requiredEngineStatus, lastPeakPower, lastEngineStatus)
                == labels(goalPowerCv, requiredEngineStatus).size();
    }

    private static EngineStatus requiredStatus(EngineStatus requiredEngineStatus) {
        return requiredEngineStatus == null ? EngineStatus.INTEGRO : requiredEngineStatus;
    }
}
