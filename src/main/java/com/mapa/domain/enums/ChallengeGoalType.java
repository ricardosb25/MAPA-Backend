package com.mapa.domain.enums;

public enum ChallengeGoalType {

    POWER("Potência (cv)"),
    TORQUE("Torque (kgfm)");

    private final String label;

    ChallengeGoalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}