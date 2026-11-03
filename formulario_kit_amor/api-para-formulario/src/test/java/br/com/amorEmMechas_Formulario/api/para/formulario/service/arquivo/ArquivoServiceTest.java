package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
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

    @TempDir
    Path tempDir;

    private ArquivoService arquivoService;
    private Arquivo arquivoMock;

    @BeforeEach
    void setUp() {
        arquivoService = new ArquivoService(arquivoRepository);
        ReflectionTestUtils.setField(arquivoService, "uploadDir", tempDir.toString());

        arquivoMock = new Arquivo();
        arquivoMock.setNomeOriginal("documento.pdf");
        arquivoMock.setNome("documento.pdf");
        arquivoMock.setMimeType("application/pdf");
        arquivoMock.setTamanho(1024L);
        arquivoMock.setCaminhoArquivo(tempDir.resolve("documento.pdf").toString());
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
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(arquivoRepository.save(any(Arquivo.class))).thenReturn(arquivoMock);

        // Act
        Arquivo resultado = arquivoService.upload(multipartFile, "PDF");

        // Assert
        assertNotNull(resultado);
        assertEquals("documento.pdf", resultado.getNomeOriginal());
        assertEquals("application/pdf", resultado.getMimeType());
        assertEquals(1024L, resultado.getTamanho());
        assertEquals("PDF", resultado.getTipo());
        assertNotNull(resultado.getCaminhoArquivo());

        verify(arquivoRepository, times(1)).save(any(Arquivo.class));
    }

    @Test
    @DisplayName("upload: deve propagar IOException quando getInputStream() falhar")
    void upload_deveLancarIOException_quandoGetInputStreamFalhar() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getInputStream()).thenThrow(new IOException("Erro ao ler stream"));

        // Act & Assert
        assertThrows(IOException.class, () -> arquivoService.upload(multipartFile, "PDF"));

        verify(arquivoRepository, never()).save(any());
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
    @DisplayName("listUpload: deve retornar lista vazia quando nao houver arquivos")
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
    @DisplayName("buscarPorId: deve lancar IdNotFoundException quando ID nao existir")
    void buscarPorId_deveLancarIdNotFoundException_quandoIdNaoExistir() {
        // Arrange
        when(arquivoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IdNotFoundException.class, () -> arquivoService.buscarPorId(99L));

        verify(arquivoRepository, times(1)).findById(99L);
    }

    // -------------------------------------------------------
    // getConteudo()
    // -------------------------------------------------------

    @Test
    @DisplayName("getConteudo: deve retornar bytes do arquivo quando existir no filesystem")
    void getConteudo_deveRetornarBytes_quandoArquivoExistir() throws IOException {
        // Arrange - criar arquivo fisico no tempDir
        Path filePath = tempDir.resolve("test-file.pdf");
        java.nio.file.Files.write(filePath, new byte[]{10, 20, 30});

        Arquivo arquivo = new Arquivo();
        arquivo.setCaminhoArquivo(filePath.toString());
        when(arquivoRepository.findById(1L)).thenReturn(Optional.of(arquivo));

        // Act
        byte[] resultado = arquivoService.getConteudo(1L);

        // Assert
        assertArrayEquals(new byte[]{10, 20, 30}, resultado);
    }
}