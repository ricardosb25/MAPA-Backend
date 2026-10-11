package com.mapa.domain.enums;

public enum EcuMapType {

    IGNITION("Mapa de ignição", "graus de avanço"),
    FUEL("Mapa de combustível", "VE %");

    private final String label;
    private final String unit;

    EcuMapType(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }
}
