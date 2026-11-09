package br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.StatusSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StatusSolicitacaoRequestDto(
        @NotNull
        @Schema(description = "Encerramento do pedido ativo. Nao permite reabrir pedidos.",
                allowableValues = {"CONCLUIDA", "CANCELADA"}, example = "CONCLUIDA")
        StatusSolicitacao status) {
}
