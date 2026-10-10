package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacaoDocumento;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
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
        ReflectionTestUtils.setField(arquivoService, "maxFileSizeBytes", 10 * 1024 * 1024L);
        ReflectionTestUtils.setField(arquivoService, "maxPdfPages", 100);
        ReflectionTestUtils.setField(arquivoService, "maxPdfObjects", 100000);
        ReflectionTestUtils.setField(arquivoService, "maxPdfDepth", 100);
        ReflectionTestUtils.setField(arquivoService, "maxPdfProcessingMillis", 3000L);

        arquivoMock = new Arquivo();
        arquivoMock.setNomeOriginal("documento.pdf");
        arquivoMock.setNome("documento.pdf");
        arquivoMock.setMimeType("application/pdf");
        arquivoMock.setTamanho(8L);
        arquivoMock.setCaminhoArquivo(tempDir.resolve("documento.pdf").toString());
        arquivoMock.setTipo("PDF");
        arquivoMock.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);
    }

    // -------------------------------------------------------
    // upload()
    // -------------------------------------------------------

    @Test
    @DisplayName("upload: deve salvar e retornar o arquivo com sucesso")
    void upload_deveRetornarArquivoSalvo_quandoDadosValidos() throws IOException {
        // Arrange
        byte[] validPdf = createPdf(document -> document.addPage(new PDPage()));
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getSize()).thenReturn((long) validPdf.length);
        when(multipartFile.getBytes()).thenReturn(validPdf);
        when(arquivoRepository.save(any(Arquivo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Arquivo resultado = arquivoService.upload(multipartFile, "PDF");

        // Assert
        assertNotNull(resultado);
        assertEquals("documento.pdf", resultado.getNomeOriginal());
        assertEquals("application/pdf", resultado.getMimeType());
        assertEquals(StatusValidacaoDocumento.PENDENTE, resultado.getStatusValidacao());
        assertEquals((long) validPdf.length, resultado.getTamanho());
        assertEquals("PDF", resultado.getTipo());
        assertNotNull(resultado.getCaminhoArquivo());
        assertTrue(java.nio.file.Files.exists(Path.of(resultado.getCaminhoArquivo())));

        verify(arquivoRepository, times(1)).save(any(Arquivo.class));
    }

    @Test
    @DisplayName("upload: deve propagar IOException quando getInputStream() falhar")
    void upload_deveLancarIOException_quandoGetInputStreamFalhar() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getBytes()).thenThrow(new IOException("Erro ao ler stream"));

        // Act & Assert
        assertThrows(IOException.class, () -> arquivoService.upload(multipartFile, "PDF"));

        verify(arquivoRepository, never()).save(any());
    }

    private byte[] createPdf(PdfConfigurer configure) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configure.configure(document);
            document.save(output);
            return output.toByteArray();
        }
    }

    @FunctionalInterface
    private interface PdfConfigurer {
        void configure(PDDocument document) throws IOException;
    }

    private COSDictionary action(String subtype) {
        COSDictionary action = new COSDictionary();
        action.setName(COSName.getPDFName("S"), subtype);
        return action;
    }

    private void assertRejectedPdf(byte[] pdf) throws IOException {
        reset(multipartFile, arquivoRepository);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn((long) pdf.length);
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getBytes()).thenReturn(pdf);

        assertThrows(ResponseStatusException.class, () -> arquivoService.upload(multipartFile, "LAUDO_MEDICO"));
        verify(arquivoRepository, never()).save(any());
        try (var files = java.nio.file.Files.list(tempDir)) {
            assertEquals(0, files.count(), "PDF rejeitado nao deve deixar arquivo no armazenamento");
        }
    }

    @Test
    @DisplayName("upload: deve rejeitar conteudo que nao corresponde a um tipo permitido")
    void upload_deveRejeitarAssinaturaInvalida() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(3L);
        when(multipartFile.getOriginalFilename()).thenReturn("documento.pdf");
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});

        assertThrows(ResponseStatusException.class, () -> arquivoService.upload(multipartFile, "LAUDO_MEDICO"));
        verify(arquivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload: deve rejeitar extensao que nao corresponde a assinatura real")
    void upload_deveRejeitarExtensaoFalsa() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(8L);
        when(multipartFile.getOriginalFilename()).thenReturn("documento.jpg");
        when(multipartFile.getBytes()).thenReturn("%PDF-1.7".getBytes());

        assertThrows(ResponseStatusException.class, () -> arquivoService.upload(multipartFile, "LAUDO_MEDICO"));
        verify(arquivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload: deve rejeitar ZIP disfarçado de PDF")
    void upload_deveRejeitarZipDisfarcadoComoPdf() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(22L);
        when(multipartFile.getOriginalFilename()).thenReturn("arquivo.pdf");
        when(multipartFile.getBytes()).thenReturn(new byte[]{'P','K',3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22});

        assertThrows(ResponseStatusException.class, () -> arquivoService.upload(multipartFile, "LAUDO_MEDICO"));
        verify(arquivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload: deve rejeitar HTML ou script disfarçado como imagem")
    void upload_deveRejeitarHtmlDisfarcadoComoImagem() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(20L);
        when(multipartFile.getOriginalFilename()).thenReturn("arquivo.png");
        when(multipartFile.getBytes()).thenReturn("<svg onload=alert(1)></svg>".getBytes());

        assertThrows(ResponseStatusException.class, () -> arquivoService.upload(multipartFile, "LAUDO_MEDICO"));
        verify(arquivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload: deve rejeitar executavel disfarçado como imagem")
    void upload_deveRejeitarExecutavelDisfarcadoComoImagem() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(20L);
        when(multipartFile.getOriginalFilename()).thenReturn("arquivo.jpg");
        when(multipartFile.getBytes()).thenReturn(new byte[]{'M','Z',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});

        assertThrows(ResponseStatusException.class, () -> arquivoService.upload(multipartFile, "LAUDO_MEDICO"));
        verify(arquivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload: deve manter PDF aceito como pendente")
    void upload_deveManterPdfAceitoComoPendente() throws IOException {
        byte[] pdf = createPdf(document -> {
            PDPage page = new PDPage();
            COSDictionary transparencyGroup = new COSDictionary();
            transparencyGroup.setName(COSName.getPDFName("S"), "Transparency");
            page.getCOSObject().setItem(COSName.getPDFName("Group"), transparencyGroup);
            document.addPage(page);
        });
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn((long) pdf.length);
        when(multipartFile.getOriginalFilename()).thenReturn("laudo.pdf");
        when(multipartFile.getBytes()).thenReturn(pdf);
        when(arquivoRepository.save(any(Arquivo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Arquivo result = arquivoService.upload(multipartFile, "LAUDO_MEDICO");

        assertEquals(StatusValidacaoDocumento.PENDENTE, result.getStatusValidacao());
        assertTrue(java.nio.file.Files.exists(Path.of(result.getCaminhoArquivo())));
    }

    @Test
    @DisplayName("upload: deve rejeitar PDF truncado sem persistir nem criar arquivo")
    void upload_deveRejeitarPdfTruncadoSemPersistencia() throws IOException {
        assertRejectedPdf("%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>".getBytes());
    }

    @Test
    @DisplayName("upload: deve rejeitar PDF criptografado mesmo com senha vazia")
    void upload_deveRejeitarPdfCriptografadoComSenhaVazia() throws IOException {
        byte[] pdf = createPdf(document -> {
            document.addPage(new PDPage());
            document.protect(new StandardProtectionPolicy("owner-password", "", new AccessPermission()));
        });
        assertRejectedPdf(pdf);
    }

    @Test
    @DisplayName("upload: deve rejeitar JavaScript e OpenAction no catalogo")
    void upload_deveRejeitarJavaScriptEOpenActionNoCatalogo() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            document.addPage(new PDPage());
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("JavaScript"), action("JavaScript"));
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar OpenAction mesmo quando aponta para navegacao interna")
    void upload_deveRejeitarOpenAction() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            document.addPage(new PDPage());
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("OpenAction"), action("GoTo"));
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar JavaScript em anotacao e acao adicional de campo")
    void upload_deveRejeitarJavaScriptEmAnotacaoECampo() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), action("JavaScript"));
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);

            COSDictionary field = new COSDictionary();
            field.setName(COSName.getPDFName("FT"), "Tx");
            field.setItem(COSName.getPDFName("AA"), action("JavaScript"));
            COSArray fields = new COSArray();
            fields.add(field);
            COSDictionary form = new COSDictionary();
            form.setItem(COSName.getPDFName("Fields"), fields);
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("AcroForm"), form);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar JavaScript em campo de formulario")
    void upload_deveRejeitarJavaScriptEmCampo() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            document.addPage(new PDPage());
            COSDictionary field = new COSDictionary();
            field.setName(COSName.getPDFName("FT"), "Tx");
            field.setItem(COSName.getPDFName("AA"), action("JavaScript"));
            COSArray fields = new COSArray();
            fields.add(field);
            COSDictionary form = new COSDictionary();
            form.setItem(COSName.getPDFName("Fields"), fields);
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("AcroForm"), form);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar JavaScript em anotacao")
    void upload_deveRejeitarJavaScriptEmAnotacao() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), action("JavaScript"));
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar Launch em cadeia de acoes")
    void upload_deveRejeitarLaunchEmCadeiaDeAcoes() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary first = action("GoTo");
            first.setItem(COSName.getPDFName("Next"), action("Launch"));
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), first);
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar cadeia ciclica de acoes")
    void upload_deveRejeitarCadeiaCiclicaDeAcoes() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary cyclicAction = action("GoTo");
            COSObject selfReference = new COSObject(cyclicAction);
            cyclicAction.setItem(COSName.getPDFName("Next"), selfReference);
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), selfReference);
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar anexos em arvore de nomes e anotacoes")
    void upload_deveRejeitarAnexosEmArvoreDeNomesEAnotacoes() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            document.addPage(new PDPage());
            COSDictionary names = new COSDictionary();
            names.setItem(COSName.getPDFName("EmbeddedFiles"), new COSDictionary());
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("Names"), names);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar anotacao de arquivo")
    void upload_deveRejeitarAnotacaoDeArquivo() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary attachment = new COSDictionary();
            attachment.setName(COSName.getPDFName("Subtype"), "FileAttachment");
            COSArray annotations = new COSArray();
            annotations.add(attachment);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar XFA")
    void upload_deveRejeitarXfa() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            document.addPage(new PDPage());
            COSDictionary form = new COSDictionary();
            form.setItem(COSName.getPDFName("XFA"), new COSString("<xfa/>"));
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("AcroForm"), form);
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar acao externa e conteudo multimidia")
    void upload_deveRejeitarAcaoExternaEConteudoMultimidia() throws IOException {
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), action("GoToR"));
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), action("SubmitForm"));
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
        assertRejectedPdf(createPdf(document -> {
            PDPage page = new PDPage();
            document.addPage(page);
            COSDictionary annotation = new COSDictionary();
            annotation.setName(COSName.getPDFName("Subtype"), "Link");
            annotation.setItem(COSName.getPDFName("A"), action("URI"));
            COSArray annotations = new COSArray();
            annotations.add(annotation);
            page.getCOSObject().setItem(COSName.getPDFName("Annots"), annotations);
        }));
        assertRejectedPdf(createPdf(document -> {
            document.addPage(new PDPage());
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("RichMedia"), new COSDictionary());
        }));
    }

    @Test
    @DisplayName("upload: deve rejeitar limites de paginas, objetos, profundidade e processamento")
    void upload_deveRejeitarLimitesDeInspecaoExcedidos() throws IOException {
        byte[] pdf = createPdf(document -> {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
        });
        ReflectionTestUtils.setField(arquivoService, "maxPdfPages", 1);
        assertRejectedPdf(pdf);

        ReflectionTestUtils.setField(arquivoService, "maxPdfPages", 100);
        ReflectionTestUtils.setField(arquivoService, "maxPdfObjects", 1);
        assertRejectedPdf(pdf);

        ReflectionTestUtils.setField(arquivoService, "maxPdfObjects", 100000);
        ReflectionTestUtils.setField(arquivoService, "maxPdfDepth", 0);
        assertRejectedPdf(pdf);

        ReflectionTestUtils.setField(arquivoService, "maxPdfDepth", 100);
        ReflectionTestUtils.setField(arquivoService, "maxPdfProcessingMillis", 0L);
        assertRejectedPdf(pdf);
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