package br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidacaoArquivoRequestDto(
        @NotNull StatusValidacao decisao,
        @Size(max = 2000) String observacao) {
}
