package br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.StatusSolicitacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {

    @Query("""
            SELECT p FROM Paciente p
            WHERE p.dadosAnonimizados = false
              AND (p.statusSolicitacao IS NULL OR p.statusSolicitacao = :status)
              AND (:busca = '' OR LOWER(p.nomeCompleto) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(p.email) LIKE LOWER(CONCAT('%', :busca, '%')))
            """)
    Page<Paciente> findSolicitacoesAtivas(@Param("status") StatusSolicitacao status,
                                         @Param("busca") String busca, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Paciente p WHERE p.id = :id AND p.dadosAnonimizados = false")
    Optional<Paciente> findSolicitacaoParaAtualizar(@Param("id") Integer id);

    Optional<Paciente> findBySolicitanteId(Integer solicitanteId);

    @Query("SELECT p FROM Paciente p LEFT JOIN p.endereco e WHERE " +
           "(:estado IS NULL OR e.estado = :estado) AND " +
           "(:dataInicio IS NULL OR p.dtPedido >= :dataInicio) AND " +
           "(:dataFim IS NULL OR p.dtPedido <= :dataFim)")
    List<Paciente> findWithFilters(@Param("estado") String estado,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p FROM Paciente p LEFT JOIN p.endereco e WHERE " +
           "(:estado IS NULL OR e.estado = :estado) AND " +
           "(:dataInicio IS NULL OR p.dtPedido >= :dataInicio) AND " +
           "(:dataFim IS NULL OR p.dtPedido <= :dataFim)")
    Page<Paciente> findWithFilters(@Param("estado") String estado,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim,
                                   Pageable pageable);
}
