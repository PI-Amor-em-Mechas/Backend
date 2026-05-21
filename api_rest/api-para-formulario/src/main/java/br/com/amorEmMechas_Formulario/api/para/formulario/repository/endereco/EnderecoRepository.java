package br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository extends JpaRepository<Endereco, Integer> {
}
