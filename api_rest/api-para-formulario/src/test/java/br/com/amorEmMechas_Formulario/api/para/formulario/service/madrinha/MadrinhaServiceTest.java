package br.com.amorEmMechas_Formulario.api.para.formulario.service.madrinha;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.madrinha.MadrinhaResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.madrinha.Madrinha;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.madrinha.MadrinhaMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.madrinha.MadrinhaRepository;
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
class MadrinhaServiceTest {

    @Mock private MadrinhaRepository repository;
    @Mock private MadrinhaMapper mapper;

    @InjectMocks
    private MadrinhaService service;

    private Madrinha madrinha;
    private MadrinhaRequestDto requestDto;
    private MadrinhaResponseDto responseDto;

    @BeforeEach
    void setUp() {
        madrinha = new Madrinha();
        madrinha.setId(1);
        madrinha.setNomeCompleto("Marcela Borges");
        madrinha.setEmail("marcela@email.com");
        madrinha.setHorasVoluntarias(200);
        madrinha.setFuncao("Montagem dos Kits");
        madrinha.setDataCadastro(LocalDate.of(2023, 5, 1));
        madrinha.setStatus("Ativa");

        requestDto = new MadrinhaRequestDto();
        requestDto.setNomeCompleto("Marcela Borges");
        requestDto.setEmail("marcela@email.com");
        requestDto.setHorasVoluntarias(200);
        requestDto.setFuncao("Montagem dos Kits");
        requestDto.setDataCadastro(LocalDate.of(2023, 5, 1));
        requestDto.setStatus("Ativa");

        responseDto = new MadrinhaResponseDto();
        responseDto.setId(1);
        responseDto.setNomeCompleto("Marcela Borges");
        responseDto.setEmail("marcela@email.com");
        responseDto.setHorasVoluntarias(200);
        responseDto.setFuncao("Montagem dos Kits");
        responseDto.setStatus("Ativa");
    }

    // ===== CREATE =====

    @Test
    void create_comDataCadastro_deveSalvarComDataInformada() {
        when(mapper.toEntity(requestDto)).thenReturn(madrinha);
        when(repository.save(madrinha)).thenReturn(madrinha);
        when(mapper.toResponse(madrinha)).thenReturn(responseDto);

        MadrinhaResponseDto result = service.create(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getNomeCompleto()).isEqualTo("Marcela Borges");
        verify(repository).save(madrinha);
    }

    @Test
    void create_semDataCadastro_deveSetarDataDeHoje() {
        Madrinha semData = new Madrinha();
        semData.setNomeCompleto("Nova");
        semData.setDataCadastro(null);

        when(mapper.toEntity(requestDto)).thenReturn(semData);
        when(repository.save(semData)).thenReturn(semData);
        when(mapper.toResponse(semData)).thenReturn(responseDto);

        service.create(requestDto);

        assertThat(semData.getDataCadastro()).isEqualTo(LocalDate.now());
    }

    // ===== UPDATE =====

    @Test
    void update_quandoExiste_deveAtualizarCamposInformados() {
        requestDto.setNomeCompleto("Maria Atualizada");
        requestDto.setEmail("maria.nova@email.com");
        requestDto.setHorasVoluntarias(300);
        requestDto.setFuncao("Reciclagem");
        requestDto.setStatus("Afastada");

        when(repository.findById(1)).thenReturn(Optional.of(madrinha));
        when(repository.save(any(Madrinha.class))).thenReturn(madrinha);
        when(mapper.toResponse(madrinha)).thenReturn(responseDto);

        service.update(1, requestDto);

        assertThat(madrinha.getNomeCompleto()).isEqualTo("Maria Atualizada");
        assertThat(madrinha.getEmail()).isEqualTo("maria.nova@email.com");
        assertThat(madrinha.getHorasVoluntarias()).isEqualTo(300);
        assertThat(madrinha.getFuncao()).isEqualTo("Reciclagem");
        assertThat(madrinha.getStatus()).isEqualTo("Afastada");
    }

    @Test
    void update_comCamposNull_naoDeveAlterarCamposExistentes() {
        MadrinhaRequestDto parcial = new MadrinhaRequestDto();
        parcial.setNomeCompleto(null);
        parcial.setEmail(null);
        parcial.setHorasVoluntarias(null);
        parcial.setFuncao(null);
        parcial.setStatus(null);

        when(repository.findById(1)).thenReturn(Optional.of(madrinha));
        when(repository.save(any(Madrinha.class))).thenReturn(madrinha);
        when(mapper.toResponse(madrinha)).thenReturn(responseDto);

        service.update(1, parcial);

        assertThat(madrinha.getNomeCompleto()).isEqualTo("Marcela Borges");
        assertThat(madrinha.getHorasVoluntarias()).isEqualTo(200);
    }

    @Test
    void update_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99, requestDto))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ===== FIND =====

    @Test
    void findAll_deveRetornarListaCompleta() {
        when(repository.findAll()).thenReturn(List.of(madrinha));
        when(mapper.toResponse(madrinha)).thenReturn(responseDto);

        List<MadrinhaResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findByStatus_deveUsarRepositorioComFiltro() {
        when(repository.findByStatus("Ativa")).thenReturn(List.of(madrinha));
        when(mapper.toResponse(madrinha)).thenReturn(responseDto);

        List<MadrinhaResponseDto> result = service.findByStatus("Ativa");

        assertThat(result).hasSize(1);
        verify(repository).findByStatus("Ativa");
    }

    @Test
    void findById_quandoExiste_deveRetornarDto() {
        when(repository.findById(1)).thenReturn(Optional.of(madrinha));
        when(mapper.toResponse(madrinha)).thenReturn(responseDto);

        MadrinhaResponseDto result = service.findById(1);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void findById_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99))
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ===== DELETE =====

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
                .isInstanceOf(IdNotFoundException.class)
                .hasMessageContaining("1");
    }
}
