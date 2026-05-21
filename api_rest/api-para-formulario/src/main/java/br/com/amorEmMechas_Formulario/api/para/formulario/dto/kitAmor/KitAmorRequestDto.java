package br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import jakarta.validation.constraints.NotNull;

public class KitAmorRequestDto {


    @NotNull(message = "Cor da peruca não deve ser vazia")
    private String corPeruca;

    @NotNull(message = "Solicitante não deve ser vazio")
    private Integer solicitanteId;

    @NotNull(message = "Paciente não deve ser vazio")
    private Integer pacienteId;

    public String getCorPeruca() {
        return corPeruca;
    }

    public void setCorPeruca(String corPeruca) {
        this.corPeruca = corPeruca;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Integer getSolicitanteId() {
        return solicitanteId;
    }

    public void setSolicitanteId(Integer solicitanteId) {
        this.solicitanteId = solicitanteId;
    }
}
