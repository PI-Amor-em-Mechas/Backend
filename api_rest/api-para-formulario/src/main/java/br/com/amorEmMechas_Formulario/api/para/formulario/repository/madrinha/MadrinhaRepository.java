package br.com.amorEmMechas_Formulario.api.para.formulario.repository.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MadrinhaRepository extends JpaRepository<Madrinha, Integer> {

    List<Madrinha> findByStatus(String status);
}
