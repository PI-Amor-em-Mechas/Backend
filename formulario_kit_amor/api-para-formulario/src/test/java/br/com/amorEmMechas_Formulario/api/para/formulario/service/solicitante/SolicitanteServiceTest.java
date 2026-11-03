package br.com.amorEmMechas_Formulario.api.para.formulario.service.solicitante;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.solicitante.SolicitanteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
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
class SolicitanteServiceTest {

    @Mock
    private SolicitanteRepository repository;

    @Mock
    private SolicitanteMapper mapper;

    @InjectMocks
    private SolicitanteService service;

    private Solicitante solicitante;
    private SolicitanteRequestDto requestDto;
    private SolicitanteResponseDto responseDto;

    @BeforeEach
    void setUp() {
        solicitante = new Solicitante();
        solicitante.setId(1);
        solicitante.setNomeCompleto("Maria");
        solicitante.setRg("MG-12345");

        requestDto = new SolicitanteRequestDto();
        requestDto.setNomeCompleto("Maria");
        requestDto.setRg("MG-12345");

        responseDto = new SolicitanteResponseDto();
        responseDto.setId(1);
        responseDto.setNomeCompleto("Maria");
        responseDto.setRg("MG-12345");
    }

    @Test
    void create_deveSalvarERetornarResponseDto() {
        when(mapper.toEntity(requestDto)).thenReturn(solicitante);
        when(repository.save(solicitante)).thenReturn(solicitante);
        when(mapper.toResponse(solicitante)).thenReturn(responseDto);

        SolicitanteResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getNomeCompleto()).isEqualTo("Maria");
        verify(repository).save(solicitante);
    }

    @Test
    void update_quandoIdExiste_deveAtualizarECarregarCampos() {
        when(repository.findById(1)).thenReturn(Optional.of(solicitante));
        when(repository.save(any(Solicitante.class))).thenReturn(solicitante);
        when(mapper.toResponse(solicitante)).thenReturn(responseDto);

        SolicitanteResponseDto result = service.update(1, requestDto);

        assertThat(result).isNotNull();
        verify(repository).findById(1);
        verify(repository).save(solicitante);
    }

    @Test
    void update_quandoIdNaoExiste_deveLancarIdNotFoundException() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99, requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository, never()).save(any());
    }

    @Test
    void findAll_deveRetornarListaMapeada() {
        when(repository.findAll()).thenReturn(List.of(solicitante));
        when(mapper.toResponse(solicitante)).thenReturn(responseDto);

        List<SolicitanteResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(solicitante));
        when(mapper.toResponse(solicitante)).thenReturn(responseDto);

        SolicitanteResponseDto result = service.findById(1);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void findById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(5))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("5");
    }

    @Test
    void deleteById_quandoExiste_deveChamarDeleteById() {
        when(repository.existsById(1)).thenReturn(true);

        service.deleteById(1);

        verify(repository).deleteById(1);
    }

    @Test
    void deleteById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.existsById(7)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(7))
                .isInstanceOf(IdNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}
