package br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class DadosMedicosRequestDto {

    @NotBlank(message = "Motivo não pode estar vazio")
    private String motivo;

    @NotBlank(message = "Tipo do câncer não pode estar vazio")
    private String tipoCancer;

    @NotBlank(message = "Justificativa não pode estar vazia")
    private String justificativa;

    @NotNull(message = "Data de início do tratamento não pode estar vazia")
    private LocalDate dtInicioTratamento;

    @NotBlank(message = "Tipo de atendimento não pode estar vazio")
    private String tipoAtendimento;

    @NotBlank(message = "Relatório médico não pode ser vazio")
    private String relatorioMedicoBase64;

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
        return relatorioMedicoBase64;
    }

    public void setRelatorioMedicoBase64(String relatorioMedicoBase64) {
        this.relatorioMedicoBase64 = relatorioMedicoBase64;
    }
}