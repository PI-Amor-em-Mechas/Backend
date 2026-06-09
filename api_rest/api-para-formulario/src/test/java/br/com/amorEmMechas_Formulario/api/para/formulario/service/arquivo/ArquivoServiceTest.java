package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArquivoServiceTest {

    @Mock
    private ArquivoRepository arquivoRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ArquivoService arquivoService;

    private Arquivo arquivoMock;

    @BeforeEach
    void setUp() {
        arquivoMock = new Arquivo();
        arquivoMock.setNomeOriginal("documento.pdf");
        arquivoMock.setNome("documento.pdf");
        arquivoMock.setMimeType("application/pdf");
        arquivoMock.setTamanho(1024L);
        arquivoMock.setConteudo(new byte[]{1, 2, 3});
        arquivoMock.setTipo("PDF");
    }

    // -------------------------------------------------------
    // upload()
    // -------------------------------------------------------

    @Test
    @DisplayName("upload: deve salvar e retornar o arquivo com sucesso")
    void upload_deveRetornarArquivoSalvo_quandoDadosValidos() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(arquivoRepository.save(any(Arquivo.class))).thenReturn(arquivoMock);

        // Act
        Arquivo resultado = arquivoService.upload(multipartFile, "PDF");

        // Assert
        assertNotNull(resultado);
        assertEquals("documento.pdf", resultado.getNomeOriginal());
        assertEquals("documento.pdf", resultado.getNome());
        assertEquals("application/pdf", resultado.getMimeType());
        assertEquals(1024L, resultado.getTamanho());
        assertEquals("PDF", resultado.getTipo());
        assertArrayEquals(new byte[]{1, 2, 3}, resultado.getConteudo());

        verify(arquivoRepository, times(1)).save(any(Arquivo.class));
    }

    @Test
    @DisplayName("upload: deve propagar IOException quando getBytes() falhar")
    void upload_deveLancarIOException_quandoGetBytesFalhar() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenThrow(new IOException("Erro ao ler bytes"));

        // Act & Assert
        assertThrows(IOException.class, () -> arquivoService.upload(multipartFile, "PDF"));

        verify(arquivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload: deve aceitar arquivo com nome e tipo nulos (MultipartFile retornando null)")
    void upload_deveAceitarValoresNulos_quandoMultipartFileRetornarNull() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getContentType()).thenReturn(null);
        when(multipartFile.getSize()).thenReturn(0L);
        when(multipartFile.getBytes()).thenReturn(new byte[]{});

        Arquivo arquivoNulo = new Arquivo();
        arquivoNulo.setNomeOriginal(null);
        arquivoNulo.setNome(null);
        arquivoNulo.setMimeType(null);
        when(arquivoRepository.save(any(Arquivo.class))).thenReturn(arquivoNulo);

        // Act
        Arquivo resultado = arquivoService.upload(multipartFile, "IMAGEM");

        // Assert
        assertNotNull(resultado);
        assertNull(resultado.getNomeOriginal());
        assertNull(resultado.getMimeType());
        verify(arquivoRepository, times(1)).save(any(Arquivo.class));
    }

    // -------------------------------------------------------
    // listUpload()
    // -------------------------------------------------------

    @Test
    @DisplayName("listUpload: deve retornar lista com todos os arquivos")
    void listUpload_deveRetornarTodosArquivos() {
        // Arrange
        Arquivo arquivo2 = new Arquivo();
        arquivo2.setNome("imagem.png");
        when(arquivoRepository.findAll()).thenReturn(Arrays.asList(arquivoMock, arquivo2));

        // Act
        List<Arquivo> resultado = arquivoService.listUpload();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(arquivoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("listUpload: deve retornar lista vazia quando não houver arquivos")
    void listUpload_deveRetornarListaVazia_quandoNaoHouverArquivos() {
        // Arrange
        when(arquivoRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Arquivo> resultado = arquivoService.listUpload();

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(arquivoRepository, times(1)).findAll();
    }

    // -------------------------------------------------------
    // buscarPorId()
    // -------------------------------------------------------

    @Test
    @DisplayName("buscarPorId: deve retornar o arquivo quando ID existir")
    void buscarPorId_deveRetornarArquivo_quandoIdExistir() {
        // Arrange
        when(arquivoRepository.findById(1L)).thenReturn(Optional.of(arquivoMock));

        // Act
        Arquivo resultado = arquivoService.buscarPorId(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals("documento.pdf", resultado.getNomeOriginal());
        verify(arquivoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId: deve lançar ResponseStatusException (400) quando ID não existir")
    void buscarPorId_deveLancarResponseStatusException_quandoIdNaoExistir() {
        // Arrange
        when(arquivoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> arquivoService.buscarPorId(99L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(arquivoRepository, times(1)).findById(99L);
    }
}