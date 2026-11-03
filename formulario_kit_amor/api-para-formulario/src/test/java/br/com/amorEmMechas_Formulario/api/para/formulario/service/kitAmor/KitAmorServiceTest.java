package br.com.amorEmMechas_Formulario.api.para.formulario.service.kitAmor;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.kitAmor.KitAmorResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.kitAmor.KitAmor;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.kitAmor.KitAmorMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.kitAmor.KitAmorRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class KitAmorServiceTest {

    @Mock
    private KitAmorRepository repository;

    @Mock
    private KitAmorMapper mapper;

    @Mock
    private SolicitanteRepository solicitanteRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private KitAmorService service;

    private Solicitante solicitante;
    private Paciente paciente;
    private KitAmor kitAmor;
    private KitAmorRequestDto requestDto;
    private KitAmorResponseDto responseDto;

    @BeforeEach
    void setUp() {
        solicitante = new Solicitante();
        solicitante.setId(1);

        paciente = new Paciente();
        paciente.setId(2);

        kitAmor = new KitAmor();
        kitAmor.setId(3);

        requestDto = new KitAmorRequestDto();
        requestDto.setCorPeruca("Castanho");
        requestDto.setSolicitanteId(1);
        requestDto.setPacienteId(2);

        responseDto = new KitAmorResponseDto();
        responseDto.setId(3);
    }

    @Test
    void create_deveAmarrarSolicitanteEPaciente() {
        when(solicitanteRepository.findById(1)).thenReturn(Optional.of(solicitante));
        when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente));
        when(mapper.toEntity(requestDto)).thenReturn(kitAmor);
        when(repository.save(any(KitAmor.class))).thenReturn(kitAmor);
        when(mapper.toResponse(kitAmor)).thenReturn(responseDto);

        KitAmorResponseDto result = service.create(requestDto);

        ArgumentCaptor<KitAmor> captor = ArgumentCaptor.forClass(KitAmor.class);
        verify(repository).save(captor.capture());
        KitAmor salvo = captor.getValue();

        assertThat(salvo.getSolicitante()).isEqualTo(solicitante);
        assertThat(salvo.getPaciente()).isEqualTo(paciente);
        assertThat(result).isNotNull();
    }

    @Test
    void create_quandoSolicitanteNaoExiste_deveLancarExcecao() {
        when(solicitanteRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("SOLICITANTE");
    }

    @Test
    void create_quandoPacienteNaoExiste_deveLancarExcecao() {
        when(solicitanteRepository.findById(1)).thenReturn(Optional.of(solicitante));
        when(pacienteRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("PACIENTE");
    }

    @Test
    void update_quandoExiste_deveAtualizarCampos() {
        when(solicitanteRepository.findById(1)).thenReturn(Optional.of(solicitante));
        when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente));
        when(repository.findById(3)).thenReturn(Optional.of(kitAmor));
        when(repository.save(any(KitAmor.class))).thenReturn(kitAmor);
        when(mapper.toResponse(kitAmor)).thenReturn(responseDto);

        service.update(3, requestDto);

        assertThat(kitAmor.getCorPeruca()).isEqualTo("Castanho");
        assertThat(kitAmor.getPaciente()).isEqualTo(paciente);
        assertThat(kitAmor.getSolicitante()).isEqualTo(solicitante);
    }

    @Test
    void update_quandoKitAmorNaoExiste_deveLancarExcecao() {
        when(solicitanteRepository.findById(1)).thenReturn(Optional.of(solicitante));
        when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente));
        when(repository.findById(3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(3, requestDto))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void findAll_deveRetornarLista() {
        when(repository.findAll()).thenReturn(List.of(kitAmor));
        when(mapper.toResponse(kitAmor)).thenReturn(responseDto);

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(3)).thenReturn(Optional.of(kitAmor));
        when(mapper.toResponse(kitAmor)).thenReturn(responseDto);

        assertThat(service.findById(3)).isNotNull();
    }

    @Test
    void findById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(3))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void deleteById_quandoExiste_deveChamarDeleteById() {
        when(repository.existsById(3)).thenReturn(true);

        service.deleteById(3);

        verify(repository).deleteById(3);
    }

    @Test
    void deleteById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.existsById(3)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(3))
                .isInstanceOf(IdNotFoundException.class);
    }
}
