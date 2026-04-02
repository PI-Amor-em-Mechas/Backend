package br.com.amorEmMechas_Formulario.api.para.formulario.repository.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Integer> {
}
