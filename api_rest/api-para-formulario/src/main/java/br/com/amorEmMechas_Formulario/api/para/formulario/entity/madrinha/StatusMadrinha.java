package br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha;

import java.util.Arrays;
import java.util.List;

public enum StatusMadrinha {
    ATIVA("Ativa"),
    AFASTADA("Afastada"),
    DESASSOCIADA("Desassociada");

    private final String apiValue;

    StatusMadrinha(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static boolean isAllowed(String value) {
        return value != null && Arrays.stream(values())
                .anyMatch(status -> status.apiValue.equals(value));
    }

    public static List<String> apiValues() {
        return Arrays.stream(values())
                .map(StatusMadrinha::getApiValue)
                .toList();
    }
}