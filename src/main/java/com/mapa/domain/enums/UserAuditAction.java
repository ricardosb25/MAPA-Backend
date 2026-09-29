package com.mapa.domain.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum UserAuditAction {

    USER_CREATED("Usuário criado", UserAuditLogLevel.INFO),
    USER_UPDATED("Usuário editado", UserAuditLogLevel.INFO),
    USER_ANONYMIZED("Usuário anonimizado", UserAuditLogLevel.AVISO),
    PASSWORD_RESET("Senha redefinida", UserAuditLogLevel.AVISO),
    TERMS_ACCEPTED("Termos aceitos", UserAuditLogLevel.INFO);

    private final String label;
    private final UserAuditLogLevel level;

    UserAuditAction(String label, UserAuditLogLevel level) {
        this.label = label;
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public UserAuditLogLevel getLevel() {
        return level;
    }

    public static Set<UserAuditAction> ofLevel(UserAuditLogLevel level) {
        return Arrays.stream(values())
                .filter(action -> action.level == level)
                .collect(Collectors.toUnmodifiableSet());
    }
}