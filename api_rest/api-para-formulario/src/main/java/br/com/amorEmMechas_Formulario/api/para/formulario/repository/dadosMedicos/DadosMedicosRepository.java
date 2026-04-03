package br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DadosMedicosRepository extends JpaRepository<DadosMedicos, Integer> {
}
