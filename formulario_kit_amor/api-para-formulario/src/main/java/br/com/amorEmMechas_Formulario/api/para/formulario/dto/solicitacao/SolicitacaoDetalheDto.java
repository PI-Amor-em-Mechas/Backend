package br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.StatusSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Detalhes da solicitacao, com CPF completo para a equipe autorizada e sem expor arquivos ou RG.")
public record SolicitacaoDetalheDto(
        Integer id, StatusSolicitacao status, PacienteResponseDto paciente,
        String nomeSolicitante, List<KitResumo> kits) {
    public record KitResumo(Integer id, String corPeruca) {}
}
