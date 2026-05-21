package br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AvaliacaoResponseDto {


    private Integer id;

    private SolicitanteResponseDto solicitante;

    private Integer notaFormulario;

    private Boolean concluido;

    private Boolean consentimento;

    private LocalDate dtConclusao;


    public Boolean getConcluido() {
        return concluido;
    }

    public void setConcluido(Boolean concluido) {
        this.concluido = concluido;
    }

    public Boolean getConsentimento() {
        return consentimento;
    }

    public void setConsentimento(Boolean consentimento) {
        this.consentimento = consentimento;
    }

    public LocalDate getDtConclusao() {
        return dtConclusao;
    }

    public void setDtConclusao(LocalDate dtConclusao) {
        this.dtConclusao = dtConclusao;
    }

    public Integer getNotaFormulario() {
        return notaFormulario;
    }

    public void setNotaFormulario(Integer notaFormulario) {
        this.notaFormulario = notaFormulario;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SolicitanteResponseDto getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(SolicitanteResponseDto solicitante) {
        this.solicitante = solicitante;
    }
}
