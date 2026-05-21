package br.com.amorEmMechas_Formulario.api.para.formulario.service.avaliacao;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.avaliacao.AvaliacaoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.avaliacao.Avaliacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.avaliacao.AvaliacaoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.solicitante.SolicitanteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.avaliacao.AvaliacaoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository repository;

    @Mock
    private SolicitanteRepository solicitanteRepository;

    @Mock
    private AvaliacaoMapper mapper;

    @Mock
    private SolicitanteMapper solicitanteMapper;

    @InjectMocks
    private AvaliacaoService service;

    private Solicitante solicitante;
    private Avaliacao avaliacao;
    private AvaliacaoRequestDto requestDto;
    private AvaliacaoResponseDto responseDto;

    @BeforeEach
    void setUp() {
        solicitante = new Solicitante();
        solicitante.setId(10);

        avaliacao = new Avaliacao();
        avaliacao.setId(1);

        requestDto = new AvaliacaoRequestDto();
        requestDto.setSolicitanteId(10);
        requestDto.setNotaFormulario(5);
        requestDto.setConcluido(true);
        requestDto.setConsentimento(true);

        responseDto = new AvaliacaoResponseDto();
        responseDto.setId(1);
        responseDto.setNotaFormulario(5);
    }

    @Test
    void create_deveDefinirSolicitanteEDataDeConclusao() {
        when(solicitanteRepository.findById(10)).thenReturn(Optional.of(solicitante));
        when(mapper.toEntity(requestDto)).thenReturn(avaliacao);
        when(repository.save(any(Avaliacao.class))).thenReturn(avaliacao);
        when(mapper.toResponse(avaliacao)).thenReturn(responseDto);

        AvaliacaoResponseDto result = service.create(requestDto);

        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
        verify(repository).save(captor.capture());
        Avaliacao salva = captor.getValue();

        assertThat(salva.getSolicitante()).isEqualTo(solicitante);
        assertThat(salva.getDtConclusao()).isEqualTo(LocalDate.now());
        assertThat(result).isNotNull();
    }

    @Test
    void create_quandoSolicitanteNaoExiste_deveLancarExcecao() {
        when(solicitanteRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("SOLICITANTE");

        verify(repository, never()).save(any());
    }

    @Test
    void findAll_deveRetornarLista() {
        when(repository.findAll()).thenReturn(List.of(avaliacao));
        when(mapper.toResponse(avaliacao)).thenReturn(responseDto);

        List<AvaliacaoResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(avaliacao));
        when(mapper.toResponse(avaliacao)).thenReturn(responseDto);

        AvaliacaoResponseDto result = service.findById(1);

        assertThat(result.getId()).isEqualTo(1);
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
