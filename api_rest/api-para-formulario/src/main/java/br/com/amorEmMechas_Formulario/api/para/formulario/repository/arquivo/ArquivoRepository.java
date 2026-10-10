package br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacaoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {
	List<Arquivo> findByStatusValidacao(StatusValidacaoDocumento statusValidacao);

}
