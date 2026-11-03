package br.com.amorEmMechas_Formulario.api.para.formulario.repository.usuario;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
}

