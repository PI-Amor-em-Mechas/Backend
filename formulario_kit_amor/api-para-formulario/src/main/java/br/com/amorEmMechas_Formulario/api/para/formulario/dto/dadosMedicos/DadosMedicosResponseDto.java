package br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos;

import java.time.LocalDate;

public class DadosMedicosResponseDto {

    private Integer id;
    private String motivo;
    private String tipoCancer;
    private String justificativa;

    private LocalDate dtInicioTratamento;
    private String tipoAtendimento;
    private Integer pacienteId;
    private Integer relatorioMedicoId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getTipoCancer() {
        return tipoCancer;
    }

    public void setTipoCancer(String tipoCancer) {
        this.tipoCancer = tipoCancer;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public LocalDate getDtInicioTratamento() {
        return dtInicioTratamento;
    }

    public void setDtInicioTratamento(LocalDate dtInicioTratamento) {
        this.dtInicioTratamento = dtInicioTratamento;
    }

    public String getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(String tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public Integer getRelatorioMedicoId() {
        return relatorioMedicoId;
    }

    public void setRelatorioMedicoId(Integer relatorioMedicoId) {
        this.relatorioMedicoId = relatorioMedicoId;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }
}