package br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.EnderecoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.endereco.EnderecoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

    @Mock
    private EnderecoRepository repository;

    @Mock
    private EnderecoMapper mapper;

    @InjectMocks
    private EnderecoService service;

    private Endereco endereco;
    private EnderecoRequestDto requestDto;
    private EnderecoResponseDto responseDto;

    @BeforeEach
    void setUp() {
        endereco = new Endereco();
        endereco.setId(1);
        endereco.setRua("Rua A");
        endereco.setCep("00000-000");

        requestDto = new EnderecoRequestDto();
        requestDto.setRua("Rua B");
        requestDto.setBairro("Centro");
        requestDto.setCep("11111-111");
        requestDto.setCidade("Uberaba");
        requestDto.setEstado("MG");
        requestDto.setNumero("100");

        responseDto = new EnderecoResponseDto();
        responseDto.setId(1);
    }

    @Test
    void create_deveSalvarECarregarResponse() {
        when(mapper.toEntity(requestDto)).thenReturn(endereco);
        when(repository.save(endereco)).thenReturn(endereco);
        when(mapper.toResponse(endereco)).thenReturn(responseDto);

        EnderecoResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        verify(repository).save(endereco);
    }

    @Test
    void update_quandoExiste_deveAtualizarCampos() {
        when(repository.findById(1)).thenReturn(Optional.of(endereco));
        when(repository.save(any(Endereco.class))).thenReturn(endereco);
        when(mapper.toResponse(endereco)).thenReturn(responseDto);

        service.update(1, requestDto);

        assertThat(endereco.getRua()).isEqualTo("Rua B");
        assertThat(endereco.getBairro()).isEqualTo("Centro");
        assertThat(endereco.getCep()).isEqualTo("11111-111");
        assertThat(endereco.getCidade()).isEqualTo("Uberaba");
        assertThat(endereco.getEstado()).isEqualTo("MG");
        assertThat(endereco.getNumero()).isEqualTo("100");
    }

    @Test
    void update_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1, requestDto))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void findAll_deveRetornarLista() {
        when(repository.findAll()).thenReturn(List.of(endereco));
        when(mapper.toResponse(endereco)).thenReturn(responseDto);

        List<EnderecoResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(endereco));
        when(mapper.toResponse(endereco)).thenReturn(responseDto);

        assertThat(service.findById(1)).isNotNull();
    }

    @Test
    void findById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void deleteById_quandoExiste_deveChamarDeleteById() {
        when(repository.existsById(1)).thenReturn(true);

        service.deleteById(1);

        verify(repository).deleteById(1);
    }

    @Test
    void deleteById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.existsById(1)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(1))
                .isInstanceOf(IdNotFoundException.class);
    }
}
