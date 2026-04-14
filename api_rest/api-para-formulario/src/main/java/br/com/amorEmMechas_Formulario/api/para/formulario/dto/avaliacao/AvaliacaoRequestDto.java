package br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AvaliacaoRequestDto {



    @NotNull(message = "Solicitante não pode ser vazio")
    @JsonProperty("solicitanteId")
    private Integer solicitanteId;

    @NotNull(message = "Nota não pode ser vazia")
    @Min(value = 0)
    @Max(value = 5)
    private Integer notaFormulario;

    @NotNull(message = "Concluido não deve ser null")
    private Boolean concluido;

    @NotNull(message = "Assine o termo de consentimento")
    private Boolean consentimento;


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



    public Integer getNotaFormulario() {
        return notaFormulario;
    }

    public void setNotaFormulario(Integer notaFormulario) {
        this.notaFormulario = notaFormulario;
    }

    public Integer getSolicitanteId() {
        return solicitanteId;
    }

    public void setSolicitanteId(Integer solicitanteId) {
        this.solicitanteId = solicitanteId;
    }
}
