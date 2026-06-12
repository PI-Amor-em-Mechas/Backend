package br.com.amorEmMechas_Formulario.api.para.formulario.service.paciente;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.paciente.PacienteResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.endereco.Endereco;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.filho.Filho;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.paciente.Paciente;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.solicitante.Solicitante;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.paciente.PacienteMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.endereco.EnderecoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.filho.FilhoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.solicitante.SolicitanteRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.security.PhiEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    private ArquivoRepository arquivoRepository;

    @Mock
    private FilhoRepository filhoRepository;

    @Mock
    private SolicitanteRepository solicitanteRepository;

    @Mock
    private PhiEncryptionUtil phiEncryptionUtil;

    @InjectMocks
    private PacienteService service;

    private Endereco endereco;
    private Solicitante solicitante;
    private Paciente paciente;
    private PacienteRequestDto requestDto;
    private PacienteResponseDto responseDto;

    @BeforeEach
    void setUp() {
        endereco = new Endereco();
        endereco.setId(10);

        solicitante = new Solicitante();
        solicitante.setId(20);

        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNomeCompleto("Ana");

        requestDto = new PacienteRequestDto();
        requestDto.setNomeCompleto("Ana");
        requestDto.setCpf("12345678900");
        requestDto.setEnderecoId(10);
        requestDto.setSolicitanteId(20);
        requestDto.setTemFilhos(false);
        requestDto.setQtdFilhos(0);
        requestDto.setConsentimentoLgpd(true);

        responseDto = new PacienteResponseDto();
        responseDto.setId(1);
        responseDto.setNomeCompleto("Ana");
    }

    @Test
    void create_semFilhos_deveSalvarECarregarResponse() {
        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(solicitanteRepository.findById(20)).thenReturn(Optional.of(solicitante));
        when(phiEncryptionUtil.encrypt("12345678900")).thenReturn("cpf-encrypted");
        when(mapper.toEntity(requestDto)).thenReturn(paciente);
        when(repository.save(any(Paciente.class))).thenReturn(paciente);
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        PacienteResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        assertThat(requestDto.getDtPedido()).isEqualTo(LocalDate.now());
        assertThat(paciente.getEndereco()).isEqualTo(endereco);
        assertThat(paciente.getSolicitante()).isEqualTo(solicitante);
        verify(repository, atLeastOnce()).save(paciente);
    }

    @Test
    void create_comFilhos_deveCriarFilhos() {
        requestDto.setTemFilhos(true);
        requestDto.setIdadesFilhos(List.of(3, 7));

        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(solicitanteRepository.findById(20)).thenReturn(Optional.of(solicitante));
        when(phiEncryptionUtil.encrypt("12345678900")).thenReturn("cpf-encrypted");
        when(mapper.toEntity(requestDto)).thenReturn(paciente);
        when(repository.save(any(Paciente.class))).thenReturn(paciente);
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        service.create(requestDto);

        assertThat(paciente.getFilhos()).hasSize(2);
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
    void create_quandoSolicitanteNaoExiste_deveLancarExcecao() {
        when(enderecoRepository.findById(10)).thenReturn(Optional.of(endereco));
        when(solicitanteRepository.findById(20)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("SOLICITANTE");
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
        when(phiEncryptionUtil.encrypt("12345678900")).thenReturn("cpf-encrypted");
        when(repository.save(any(Paciente.class))).thenReturn(paciente);
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        requestDto.setNomeCompleto("Beatriz");

        service.update(1, requestDto);

        assertThat(paciente.getNomeCompleto()).isEqualTo("Beatriz");
        assertThat(paciente.getCpf()).isEqualTo("cpf-encrypted");
        assertThat(paciente.getEndereco()).isEqualTo(endereco);
    }

    @Test
    void findAll_deveRetornarLista() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Paciente> page = new PageImpl<>(List.of(paciente));
        when(repository.findAll(pageable)).thenReturn(page);
        when(mapper.toResponse(paciente)).thenReturn(responseDto);

        assertThat(service.findAll(pageable).getContent()).hasSize(1);
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
