package br.com.amorEmMechas_Formulario.api.para.formulario.controller.solicitacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitacao.*;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitacao.SolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitacoes")
@Tag(name = "Solicitacoes", description = "Pedidos de peruca da equipe. Novos e legados sem status sao ATIVA; anonimizados sao excluidos.")
@SecurityRequirement(name = "bearerAuth")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@ApiResponse(responseCode = "401", description = "JWT ausente ou invalido")
@ApiResponse(responseCode = "403", description = "Restrito a ADMIN, MEDICO, ENFERMEIRO e ATENDENTE")
public class SolicitacaoController {
    private final SolicitacaoService service;

    public SolicitacaoController(SolicitacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todas as solicitacoes ativas, paginadas",
            description = "Busca por nome ou email em todo o banco. Ordenacao padrao: pedido e ID decrescentes. Inclui cadastros ainda incompletos.")
    @ApiResponse(responseCode = "200", description = "Pagina de pedidos ativos; content vazio quando nao houver resultados")
    public Page<SolicitacaoResumoDto> listar(
            @Parameter(description = "Trecho do nome ou email, sem diferenciar maiusculas")
            @RequestParam(defaultValue = "") String busca,
            @ParameterObject @PageableDefault(size = 20, sort = {"dtPedido", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarAtivas(busca, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma solicitacao",
            description = "Inclui paciente, contato, endereco, dados familiares e medicos, solicitante e kits. CPF completo somente para a equipe autorizada nesta rota; demais rotas de pacientes mantem o mascaramento. Campos ainda nao cadastrados podem estar ausentes.")
    @ApiResponse(responseCode = "200", description = "Detalhes do pedido")
    @ApiResponse(responseCode = "404", description = "Pedido inexistente ou anonimizado")
    public SolicitacaoDetalheDto detalhar(@PathVariable Integer id) {
        return service.detalhar(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Conclui ou cancela uma solicitacao ativa",
            description = "Persiste CONCLUIDA ou CANCELADA e remove da fila ativa. Nao exclui o paciente, nao altera entregas e nao permite reabertura.")
    @ApiResponse(responseCode = "200", description = "Resumo com status atualizado")
    @ApiResponse(responseCode = "400", description = "Status ausente, invalido ou ATIVA")
    @ApiResponse(responseCode = "404", description = "Pedido inexistente ou anonimizado")
    @ApiResponse(responseCode = "409", description = "Pedido ja encerrado por outro atendimento")
    public SolicitacaoResumoDto encerrar(@PathVariable Integer id,
                                         @Valid @RequestBody StatusSolicitacaoRequestDto dto) {
        return service.encerrar(id, dto.status());
    }
}
