package br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilhoRepository extends JpaRepository<Filho, Integer> {
    Integer countByPacienteId(Integer pacienteId);
}

