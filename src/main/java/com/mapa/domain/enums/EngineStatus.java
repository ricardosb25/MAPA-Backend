package com.mapa.domain.enums;

public enum EngineStatus {

    INTEGRO("Motor íntegro"),
    QUEBRADO_POBRE("Motor quebrado por mistura pobre"),
    QUEBRADO_DETONACAO("Motor quebrado por detonação"),
    OVERREV("Motor quebrado por excesso de giro");

    private final String label;

    EngineStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
