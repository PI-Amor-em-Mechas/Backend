package br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos;

import java.time.LocalDate;

public class DadosMedicosResponseDto {

    private Integer id;
    private String motivo;
    private String tipoCancer;
    private String justificativa;
    private String RelatorioMedicoBase64;
    private LocalDate dtInicioTratamento;
    private String tipoAtendimento;

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

    public String getRelatorioMedicoBase64() {
        return RelatorioMedicoBase64;
    }

    public void setRelatorioMedicoBase64(String relatorioMedicoBase64) {
        RelatorioMedicoBase64 = relatorioMedicoBase64;
    }
}