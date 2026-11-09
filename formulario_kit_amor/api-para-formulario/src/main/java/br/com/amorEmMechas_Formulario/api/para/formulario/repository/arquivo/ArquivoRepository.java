package br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {
    @Query("select a from Arquivo a where (a.statusValidacao is null or a.statusValidacao = "
            + "br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacao.PENDENTE) "
            + "and (a.tipo is null or a.tipo <> 'FOTO_CABELO') order by a.id desc")
    List<Arquivo> findPendentesValidacao();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Arquivo a where a.id = :id")
    Optional<Arquivo> findParaValidar(@Param("id") Long id);
}
