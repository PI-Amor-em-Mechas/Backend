package br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class DadosMedicosServiceTest {

    @Mock
    private DadosMedicosRepository repository;

    @Mock
    private DadosMedicosMapper mapper;

    @Mock
    private ArquivoRepository arquivoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private DadosMedicosService service;

    private DadosMedicos entity;
    private DadosMedicosRequestDto requestDto;
    private DadosMedicosResponseDto responseDto;

    @BeforeEach
    void setUp() {
        entity = new DadosMedicos();
        entity.setId(1);

        requestDto = new DadosMedicosRequestDto();
        requestDto.setMotivo("Tratamento");
        requestDto.setTipoCancer("Mama");
        requestDto.setJustificativa("Urgente");
        requestDto.setTipoAtendimento("SUS");
        requestDto.setDtInicioTratamento(LocalDate.now());
<<<<<<< HEAD
=======
        requestDto.setRelatorioMedicoId(1);
        requestDto.setPacienteId(1);
>>>>>>> afcf7a5df0c834943ff062711e5510d7bee16e27

        responseDto = new DadosMedicosResponseDto();
        responseDto.setId(1);
    }

<<<<<<< HEAD
//    @Test
//    void create_deveSalvarECarregarResponse() {
//        when(mapper.toEntity(requestDto)).thenReturn(entity);
//        when(repository.save(entity)).thenReturn(entity);
//        when(mapper.toResponse(entity)).thenReturn(responseDto);
//
//        DadosMedicosResponseDto result = service.create(requestDto);
//
//        assertThat(result).isNotNull();
//        verify(repository).save(entity);
//    }
=======
    @Test
    void create_deveSalvarECarregarResponse() {
        Arquivo arquivo = new Arquivo();
        arquivo.setId(1);
        Paciente paciente = new Paciente();
        paciente.setId(1);

        when(mapper.toEntity(requestDto)).thenReturn(entity);
        when(arquivoRepository.findById(1L)).thenReturn(Optional.of(arquivo));
        when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(responseDto);

        DadosMedicosResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        verify(repository).save(entity);
    }
>>>>>>> afcf7a5df0c834943ff062711e5510d7bee16e27

    @Test
    void update_quandoExiste_deveAtualizarCampos() {
        Arquivo arquivo = new Arquivo();
        arquivo.setId(1);

        when(repository.findById(1)).thenReturn(Optional.of(entity));
        when(arquivoRepository.findById(1L)).thenReturn(Optional.of(arquivo));
        when(repository.save(any(DadosMedicos.class))).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(responseDto);

        service.update(1, requestDto);

        assertThat(entity.getMotivo()).isEqualTo("Tratamento");
        assertThat(entity.getTipoCancer()).isEqualTo("Mama");
        assertThat(entity.getJustificativa()).isEqualTo("Urgente");
        assertThat(entity.getTipoAtendimento()).isEqualTo("SUS");
    }

    @Test
    void update_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1, requestDto))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void findAll_deveRetornarLista() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(responseDto);

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(responseDto);

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
