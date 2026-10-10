package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacaoDocumento;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ArquivoService {

    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        PDF_MIME_TYPE,
        JPEG_MIME_TYPE,
        PNG_MIME_TYPE
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".pdf",
        ".jpg",
        ".jpeg",
        ".png"
    );

    private static final int MAX_IMAGE_DIMENSION = 8192;
    private static final long MAX_IMAGE_PIXELS = 64L * 1024 * 1024;
        private static final Set<String> FORBIDDEN_PDF_KEYS = Set.of(
            "JavaScript", "JS", "OpenAction", "AA", "EmbeddedFiles", "EF", "AF", "XFA",
            "RichMedia", "Movie", "Sound", "Rendition", "3D", "Launch", "SubmitForm",
            "ImportData", "GoToR", "GoToE", "URI"
        );
        private static final Set<String> FORBIDDEN_PDF_SUBTYPES = Set.of(
            "FileAttachment", "RichMedia", "Movie", "Sound", "3D"
        );

    private final ArquivoRepository arquivoRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Value("${app.upload.pdf.max-pages:100}")
    private int maxPdfPages;

    @Value("${app.upload.pdf.max-objects:100000}")
    private int maxPdfObjects;

    @Value("${app.upload.pdf.max-depth:100}")
    private int maxPdfDepth;

    @Value("${app.upload.pdf.max-processing-ms:3000}")
    private long maxPdfProcessingMillis;

    public ArquivoService(ArquivoRepository arquivoRepository) {
        this.arquivoRepository = arquivoRepository;
    }

    @PostConstruct
    public void init() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public Arquivo upload(MultipartFile file, String tipo) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o tamanho permitido");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do arquivo obrigatorio");
        }
        String safeOriginalName = Paths.get(originalName.replace('\\', '/')).getFileName().toString();
        if (safeOriginalName.contains("..") || safeOriginalName.contains("/") || safeOriginalName.contains("\\") || safeOriginalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo invalido");
        }
        byte[] content = file.getBytes();
        if (content.length == 0 || content.length > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo vazio ou excede o tamanho permitido");
        }
        String detectedMimeType = detectMimeType(content);
        validateDocumentContent(content, detectedMimeType);
        validateExtension(safeOriginalName, detectedMimeType);

        String nomeUnico = UUID.randomUUID() + "_" + safeOriginalName;
        Path destino = Paths.get(uploadDir).resolve(nomeUnico).normalize();
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!destino.toAbsolutePath().normalize().startsWith(uploadPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo invalido");
        }

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeOriginal(safeOriginalName);
        arquivo.setNome(nomeUnico);
        arquivo.setMimeType(detectedMimeType);
        arquivo.setTamanho((long) content.length);
        arquivo.setCaminhoArquivo(destino.toString());
        arquivo.setTipo(tipo);
        arquivo.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);

        boolean persisted = false;
        try {
            Files.write(destino, content);
            Arquivo salvo = arquivoRepository.save(arquivo);
            persisted = true;
            return salvo;
        } finally {
            if (!persisted) {
                Files.deleteIfExists(destino);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Arquivo> findPendingValidation() {
        return arquivoRepository.findByStatusValidacao(StatusValidacaoDocumento.PENDENTE);
    }

    @Transactional
    public Arquivo review(Long id, StatusValidacaoDocumento decision, String reviewer, String observation) {
        if (decision == null || decision == StatusValidacaoDocumento.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decisao deve ser APROVADO ou REJEITADO");
        }
        if (reviewer == null || reviewer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Revisor autenticado obrigatorio");
        }
        if (decision == StatusValidacaoDocumento.REJEITADO && (observation == null || observation.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o motivo da rejeicao");
        }

        Arquivo arquivo = buscarPorId(id);
        if (arquivo.getStatusValidacao() != StatusValidacaoDocumento.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento ja foi revisado");
        }

        arquivo.setStatusValidacao(decision);
        arquivo.setValidadoPor(reviewer);
        arquivo.setValidadoEm(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        arquivo.setObservacaoValidacao(observation == null || observation.isBlank() ? null : observation.trim());
        return arquivoRepository.save(arquivo);
    }

    private String detectMimeType(byte[] content) {
        if (startsWith(content, new byte[]{'%', 'P', 'D', 'F', '-'})) {
            return PDF_MIME_TYPE;
        }
        if (startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return JPEG_MIME_TYPE;
        }
        if (startsWith(content, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10})) {
            return PNG_MIME_TYPE;
        }
        if (startsWith(content, new byte[]{'P', 'K'})) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Arquivo compactado nao e permitido. Envie PDF, JPG ou PNG");
        }
        if (startsWith(content, new byte[]{'M', 'Z'})) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Arquivo executavel nao e permitido");
        }
        String text = new String(content, java.nio.charset.StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
        if (text.contains("<svg") || text.contains("<html") || text.contains("<script") || text.contains("javascript:")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Conteudo scriptado ou html nao e permitido");
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato nao suportado ou assinatura de arquivo invalida");
    }

    private void validateDocumentContent(byte[] content, String detectedMimeType) {
        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de arquivo nao permitido");
        }

        switch (detectedMimeType) {
            case PDF_MIME_TYPE -> validatePdfContent(content);
            case JPEG_MIME_TYPE, PNG_MIME_TYPE -> validateImageContent(content);
            default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato nao suportado");
        }
    }

    private void validatePdfContent(byte[] content) {
        long deadline = System.nanoTime() + maxPdfProcessingMillis * 1_000_000L;
        try (PDDocument document = new PDFParser(new RandomAccessReadBuffer(content), "").parse(false)) {
            if (document.isEncrypted()) {
                rejectPdf("PDF criptografado nao e permitido");
            }
            if (document.getNumberOfPages() < 1 || document.getNumberOfPages() > maxPdfPages) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "PDF excede o limite de paginas permitido");
            }

            for (PDPage page : document.getPages()) {
                checkPdfProcessingDeadline(deadline);
                page.getAnnotations();
            }

            PdfScanState state = new PdfScanState(deadline);
            COSDictionary catalog = document.getDocumentCatalog().getCOSObject();
            scanPdfObject(catalog, 0, state);
            Set<COSObjectKey> objectKeys = document.getDocument().getXrefTable().keySet();
            if (objectKeys.size() > maxPdfObjects) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "PDF excede o limite de objetos permitido");
            }
            for (COSObjectKey objectKey : objectKeys) {
                checkPdfProcessingDeadline(deadline);
                COSObject object = document.getDocument().getObjectFromPool(objectKey);
                scanPdfObject(object.getObject(), 0, state);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "PDF invalido, criptografado ou nao suportado pela politica de inspecao");
        }
    }

    private void scanPdfObject(COSBase value, int depth, PdfScanState state) {
        checkPdfProcessingDeadline(state.deadlineNanos);
        if (depth > maxPdfDepth) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Estrutura do PDF excede a profundidade permitida");
        }
        if (value == null) {
            return;
        }
        if (!state.visited.add(value)) {
            return;
        }
        if (++state.objectCount > maxPdfObjects) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "PDF excede o limite de objetos inspecionados");
        }

        if (value instanceof COSObject) {
            return;
        }
        if (value instanceof COSDictionary dictionary) {
            validatePdfDictionary(dictionary);
            for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet()) {
                if (COSName.getPDFName("A").equals(entry.getKey())) {
                    validateActionChain(entry.getValue(), 0,
                            Collections.newSetFromMap(new IdentityHashMap<>()), state);
                } else {
                    scanPdfObject(entry.getValue(), depth + 1, state);
                }
            }
        } else if (value instanceof COSArray array) {
            for (COSBase element : array) {
                scanPdfObject(element, depth + 1, state);
            }
        }
    }

    private void validatePdfDictionary(COSDictionary dictionary) {
        for (COSName key : dictionary.keySet()) {
            if (FORBIDDEN_PDF_KEYS.contains(key.getName())) {
                rejectPdf("PDF contem recursos ativos ou anexos nao permitidos");
            }
        }

        String type = dictionary.getNameAsString("Type");
        String subtype = dictionary.getNameAsString("Subtype");
        if ("Filespec".equals(type) || (subtype != null && FORBIDDEN_PDF_SUBTYPES.contains(subtype))) {
            rejectPdf("PDF contem anexos ou conteudo multimidia nao permitido");
        }

    }

    private void validateActionChain(COSBase actionValue, int depth, Set<COSDictionary> visitedActions,
                                     PdfScanState state) {
        checkPdfProcessingDeadline(state.deadlineNanos);
        if (depth > maxPdfDepth || ++state.actionCount > maxPdfObjects) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Cadeia de acoes do PDF excede os limites permitidos");
        }
        Set<COSObject> visitedReferences = Collections.newSetFromMap(new IdentityHashMap<>());
        while (actionValue instanceof COSObject reference) {
            if (!visitedReferences.add(reference)) {
                rejectPdf("PDF contem uma cadeia de acoes ciclica");
            }
            actionValue = reference.getObject();
        }
        if (!(actionValue instanceof COSDictionary action)) {
            rejectPdf("PDF contem uma cadeia de acoes invalida");
            return;
        }
        COSBase actionType = action.getDictionaryObject(COSName.getPDFName("S"));
        if (!(actionType instanceof COSName actionName) || !"GoTo".equals(actionName.getName())) {
            rejectPdf("PDF contem uma acao nao permitida");
        }
        if (!visitedActions.add(action)) {
            rejectPdf("PDF contem uma cadeia de acoes ciclica");
        }

        try {
            COSBase next = action.getItem(COSName.getPDFName("Next"));
            if (next instanceof COSArray actions) {
                for (COSBase chainedAction : actions) {
                    validateActionChain(chainedAction, depth + 1, visitedActions, state);
                }
            } else if (next != null) {
                validateActionChain(next, depth + 1, visitedActions, state);
            }
        } finally {
            visitedActions.remove(action);
        }
    }

    private void checkPdfProcessingDeadline(long deadlineNanos) {
        if (System.nanoTime() > deadlineNanos) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Inspecao do PDF excedeu o tempo de processamento permitido");
        }
    }

    private void rejectPdf(String reason) {
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
    }

    private void validateImageContent(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Imagem corrompida ou invalida");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            long pixels = (long) width * height;
            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION || pixels > MAX_IMAGE_PIXELS) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Imagem excede os limites de dimensao permitidos");
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Imagem invalida");
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) return false;
        }
        return true;
    }

    private void validateExtension(String filename, String detectedMimeType) {
        String lowerName = filename.toLowerCase(Locale.ROOT);
        String extension = lowerName.contains(".") ? lowerName.substring(lowerName.lastIndexOf('.')) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Extensao de arquivo nao permitida");
        }
        boolean extensionMatches = switch (detectedMimeType) {
            case PDF_MIME_TYPE -> lowerName.endsWith(".pdf");
            case JPEG_MIME_TYPE -> lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
            case PNG_MIME_TYPE -> lowerName.endsWith(".png");
            default -> false;
        };
        if (!extensionMatches) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Extensao nao corresponde ao conteudo real do arquivo");
        }
    }

    public List<Arquivo> listUpload() {
        return arquivoRepository.findAll();
    }

    public Arquivo buscarPorId(Long id) {
        return arquivoRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("ID ARQUIVO: " + id + " Nao Encontrado"));
    }

    public byte[] getConteudo(Long id) throws IOException {
        Arquivo arquivo = buscarPorId(id);
        Path path = Paths.get(arquivo.getCaminhoArquivo());
        return Files.readAllBytes(path);
    }

    private final class PdfScanState {
        private final long deadlineNanos;
        private final Set<COSBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        private int objectCount;
        private int actionCount;

        private PdfScanState(long deadlineNanos) {
            this.deadlineNanos = deadlineNanos;
        }
    }
}
