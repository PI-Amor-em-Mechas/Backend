package br.com.amorEmMechas_Formulario.api.para.formulario.repository.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MadrinhaRepository extends JpaRepository<Madrinha, Integer> {

    List<Madrinha> findByStatus(String status);

    Optional<Madrinha> findByColaboradorId(String colaboradorId);

    @Query(value = """
            SELECT c.id, c.nome, c.criado_em
            FROM colaborador c
            WHERE c.anonimizado_em IS NULL
            ORDER BY c.criado_em
            """, nativeQuery = true)
    List<Object[]> findColaboradoresFaciaisAtivos();

    @Query(value = """
            SELECT p.data_hora, p.tipo
            FROM ponto p
            WHERE p.colaborador_id = :colaboradorId
            ORDER BY p.data_hora
            """, nativeQuery = true)
    List<Object[]> findPontosByColaboradorId(@Param("colaboradorId") String colaboradorId);
}
