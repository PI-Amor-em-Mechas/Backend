package br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {



}
