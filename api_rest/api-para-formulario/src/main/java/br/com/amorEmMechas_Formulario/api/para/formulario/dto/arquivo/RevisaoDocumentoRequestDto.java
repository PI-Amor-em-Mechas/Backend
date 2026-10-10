package br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacaoDocumento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RevisaoDocumentoRequestDto {

    @NotNull
    private StatusValidacaoDocumento decisao;

    @Size(max = 1000)
    private String observacao;

    public StatusValidacaoDocumento getDecisao() {
        return decisao;
    }

    public void setDecisao(StatusValidacaoDocumento decisao) {
        this.decisao = decisao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}