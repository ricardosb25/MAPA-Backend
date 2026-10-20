package com.mapa.domain.enums;

public enum ChallengeDifficulty {

    EASY("Fácil"),
    MEDIUM("Médio"),
    HARD("Difícil");

    private final String label;

    ChallengeDifficulty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
