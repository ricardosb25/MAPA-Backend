package com.mapa.exception;

public class ActiveMotorNotSelectedException extends RuntimeException {

    public ActiveMotorNotSelectedException() {
        super("Nenhum motor ativo selecionado. Selecione um motor na bancada antes de executar o dinamômetro.");
    }
}
