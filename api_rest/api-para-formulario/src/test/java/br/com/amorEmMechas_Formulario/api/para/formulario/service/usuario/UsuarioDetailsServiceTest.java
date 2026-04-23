package br.com.amorEmMechas_Formulario.api.para.formulario.service.usuario;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.usuario.Usuario;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("senha");
        usuario.setRole("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_quandoExiste_deveRetornarUserDetails() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        UserDetails result = service.loadUserByUsername("admin");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_quandoNaoExiste_deveLancarUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("desconhecido"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("desconhecido");
    }
}
