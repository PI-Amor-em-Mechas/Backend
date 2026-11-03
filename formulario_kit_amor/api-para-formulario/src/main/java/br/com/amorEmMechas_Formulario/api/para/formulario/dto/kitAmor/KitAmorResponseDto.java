package br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;

public class KitAmorResponseDto {


    private Integer id;
    private String corPeruca;
    private SolicitanteResponseDto solicitante;
    private PacienteResponseDto paciente;
    private Integer solicitanteId;
    private Integer pacienteId;

    public String getCorPeruca() {
        return corPeruca;
    }

    public void setCorPeruca(String corPeruca) {
        this.corPeruca = corPeruca;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PacienteResponseDto getPaciente() {
        return paciente;
    }

    public void setPaciente(PacienteResponseDto paciente) {
        this.paciente = paciente;
    }

    public SolicitanteResponseDto getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(SolicitanteResponseDto solicitante) {
        this.solicitante = solicitante;
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
