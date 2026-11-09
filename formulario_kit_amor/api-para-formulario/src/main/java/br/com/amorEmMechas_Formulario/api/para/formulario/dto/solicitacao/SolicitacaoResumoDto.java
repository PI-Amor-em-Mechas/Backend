package br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.StatusSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Resumo de um pedido de peruca. O ID e o mesmo do paciente.")
public record SolicitacaoResumoDto(
        Integer id, String nomePaciente, String email, LocalDate dtPedido,
        String cidade, String estado, StatusSolicitacao status) {
}
