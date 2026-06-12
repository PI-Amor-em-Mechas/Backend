package br.com.amorEmMechas_Formulario.api.para.formulario.repository.kitAmor;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface KitAmorRepository extends JpaRepository<KitAmor, Integer> {

    @Query("SELECT k FROM KitAmor k LEFT JOIN k.paciente p LEFT JOIN p.endereco e WHERE " +
           "(:estado IS NULL OR e.estado = :estado) AND " +
           "(:dataInicio IS NULL OR p.dtPedido >= :dataInicio) AND " +
           "(:dataFim IS NULL OR p.dtPedido <= :dataFim)")
    List<KitAmor> findWithFilters(@Param("estado") String estado,
                                   @Param("dataInicio") LocalDate dataInicio,
                                   @Param("dataFim") LocalDate dataFim);
}
