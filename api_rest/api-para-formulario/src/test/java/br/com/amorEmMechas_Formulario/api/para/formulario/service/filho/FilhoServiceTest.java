package br.com.amorEmMechas_Formulario.api.para.formulario.service.filho;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.filho.FilhoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.filho.FilhoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
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
class FilhoServiceTest {

    @Mock
    private FilhoRepository repository;

    @Mock
    private FilhoMapper mapper;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private FilhoService service;

    private Filho filho;
    private Paciente paciente;
    private FilhoRequestDto requestDto;
    private FilhoResponseDto responseDto;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(100);

        filho = new Filho();
        filho.setId(1);
        filho.setIdade(5);
        filho.setPaciente(paciente);

        requestDto = new FilhoRequestDto();
        requestDto.setId(1);
        requestDto.setIdade(10);
        requestDto.setPacienteId(100);

        responseDto = new FilhoResponseDto();
        responseDto.setId(1);
        responseDto.setIdade(10);
        responseDto.setPacienteId(100);
    }

    @Test
    void create_deveSalvarECarregarResponse() {
        when(mapper.toEntity(requestDto)).thenReturn(filho);
        when(repository.save(filho)).thenReturn(filho);
        when(mapper.toResponse(filho)).thenReturn(responseDto);

        FilhoResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        verify(repository).save(filho);
    }

    @Test
    void update_quandoExiste_deveAtualizarIdadeEPaciente() {
        when(repository.findById(1)).thenReturn(Optional.of(filho));
        when(pacienteRepository.findById(100)).thenReturn(Optional.of(paciente));
        when(repository.save(any(Filho.class))).thenReturn(filho);
        when(mapper.toResponse(filho)).thenReturn(responseDto);

        service.update(1, requestDto);

        assertThat(filho.getIdade()).isEqualTo(10);
        assertThat(filho.getPaciente()).isEqualTo(paciente);
    }

    @Test
    void update_quandoFilhoNaoExiste_deveLancarExcecao() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1, requestDto))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void update_quandoPacienteNaoExiste_deveLancarExcecao() {
        when(repository.findById(1)).thenReturn(Optional.of(filho));
        when(pacienteRepository.findById(100)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1, requestDto))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void updateMany_deveAtualizarCadaFilho() {
        when(repository.findById(1)).thenReturn(Optional.of(filho));
        when(pacienteRepository.findById(100)).thenReturn(Optional.of(paciente));
        when(repository.save(any(Filho.class))).thenReturn(filho);
        when(mapper.toResponse(filho)).thenReturn(responseDto);

        List<FilhoResponseDto> result = service.updateMany(List.of(requestDto));

        assertThat(result).hasSize(1);
        verify(repository).save(any(Filho.class));
    }

    @Test
    void findAll_deveRetornarComQuantidade() {
        when(repository.findAll()).thenReturn(List.of(filho));
        when(repository.countByPacienteId(100)).thenReturn(2);

        List<FilhoResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQtdFilho()).isEqualTo(2);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(filho));
        when(repository.countByPacienteId(100)).thenReturn(3);

        FilhoResponseDto result = service.findById(1);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getQtdFilho()).isEqualTo(3);
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
