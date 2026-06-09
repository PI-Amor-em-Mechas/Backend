package br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {

    Optional<Paciente> findBySolicitanteId(Integer solicitanteId);

    @Query("SELECT p FROM Paciente p LEFT JOIN p.endereco e WHERE " +
           "(:estado IS NULL OR e.estado = :estado) AND " +
           "(:dataInicio IS NULL OR p.dtPedido >= :dataInicio) AND " +
           "(:dataFim IS NULL OR p.dtPedido <= :dataFim)")
    List<Paciente> findWithFilters(@Param("estado") String estado,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim);
}
