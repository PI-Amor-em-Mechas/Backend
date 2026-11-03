package br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante;

import jakarta.validation.constraints.NotNull;

public class SolicitanteRequestDto {

    @NotNull(message = "Nome não pode ser vazio")
    private String nomeCompleto;

    @NotNull(message = "rg não deve ser vazio")
    private String rg;


    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getRg() {
        return rg;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }
}
