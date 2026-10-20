package com.mapa.domain.enums;

public enum ChallengeStudentStatus {

    NOT_STARTED("Não iniciado"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluído");

    private final String label;

    ChallengeStudentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}