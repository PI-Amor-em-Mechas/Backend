package br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
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
class PacienteServiceTest {

    @Mock
    private PacienteRepository repository;

    @Mock
    private PacienteMapper mapper;

    @Mock
    private EnderecoRepository enderecoRepository;

    @Mock
    private DadosMedicosRepository dadosMedicosRepository;

    @Mock
    private FilhoRepository filhoRepository;

    @InjectMocks
    private PacienteService service;

    private Endereco endereco;
    private DadosMedicos dadosMedicos;
    private Paciente paciente;
    private PacienteRequestDto requestDto;
    private PacienteResponseDto responseDto;

    @BeforeEach
    void setUp() {
        endereco = new Endereco();
        endereco.setId(10);

        dadosMedicos = new DadosMedicos();
        dadosMedicos.setId(20);

        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNomeCompleto("Ana");

        requestDto = new PacienteRequestDto();
        requestDto.setNomeCompleto("Ana");
        requestDto.setCpf("12345678900");
        requestDto.setEnderecoId(10);
        requestDto.setDadosMedicosId(20);
        requestDto.setTemFilhos(false);
        requestDto.setQtdFilhos(0);

        responseDto = new PacienteResponseDto();
        responseDto.setId(1);
        responseDto.setNomeCompleto("Ana");
    }

    @Test
    void create_semFilhos_deveSalvarECarregarResponse() {
        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(dadosMedicosRepository.findById(20)).thenReturn(Optional.of(dadosMedicos));
        when(mapper.toEntity(requestDto)).thenReturn(paciente);
        when(repository.save(any(Paciente.class))).thenReturn(paciente);
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        PacienteResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        assertThat(requestDto.getDtPedido()).isEqualTo(LocalDate.now());
        assertThat(paciente.getEndereco()).isEqualTo(endereco);
        assertThat(paciente.getDadosMedicos()).isEqualTo(dadosMedicos);
        verify(repository, atLeastOnce()).save(paciente);
    }

    @Test
    void create_comFilhos_deveCriarFilhos() {
        requestDto.setTemFilhos(true);
        requestDto.setIdadesFilhos(List.of(3, 7));

        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(dadosMedicosRepository.findById(20)).thenReturn(Optional.of(dadosMedicos));
        when(mapper.toEntity(requestDto)).thenReturn(paciente);
        when(repository.save(any(Paciente.class))).thenReturn(paciente);
        when(filhoRepository.save(any(Filho.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        service.create(requestDto);

        verify(filhoRepository, times(2)).save(any(Filho.class));
        assertThat(paciente.getQtdFilhos()).isEqualTo(2);
    }

    @Test
    void create_quandoEnderecoNaoExiste_deveLancarExcecao() {
        when(enderecoRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("ENDERECO");
    }

    @Test
    void create_quandoDadosMedicosNaoExiste_deveLancarExcecao() {
        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(dadosMedicosRepository.findById(20)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("DADOS MÉDICOS");
    }

    @Test
    void update_quandoPacienteNaoExiste_deveLancarExcecao() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1, requestDto))
                .isInstanceOf(IdNotFoundException.class);
    }

    @Test
    void update_deveAtualizarCamposInformados() {
        when(repository.findById(1)).thenReturn(Optional.of(paciente));
        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(dadosMedicosRepository.findById(20)).thenReturn(Optional.of(dadosMedicos));
        when(repository.save(any(Paciente.class))).thenReturn(paciente);
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        requestDto.setNomeCompleto("Beatriz");

        service.update(1, requestDto);

        assertThat(paciente.getNomeCompleto()).isEqualTo("Beatriz");
        assertThat(paciente.getCpf()).isEqualTo("12345678900");
        assertThat(paciente.getEndereco()).isEqualTo(endereco);
        assertThat(paciente.getDadosMedicos()).isEqualTo(dadosMedicos);
    }

    @Test
    void findAll_deveRetornarLista() {
        when(repository.findAll()).thenReturn(List.of(paciente));
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(paciente));
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

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
