package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.StatusValidacao;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.paciente.PacienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

@Service
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;
    private final PacienteRepository pacienteRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public ArquivoService(ArquivoRepository arquivoRepository, PacienteRepository pacienteRepository) {
        this.arquivoRepository = arquivoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @PostConstruct
    public void init() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @Transactional
    public Arquivo upload(MultipartFile file, String tipo) throws IOException {
        return upload(file, tipo, null);
    }

    @Transactional
    public Arquivo upload(MultipartFile file, String tipo, Integer pacienteId) throws IOException {
        if (pacienteId != null && !podeRevisar()) {
            throw new AccessDeniedException("Associacao ao paciente exige perfil revisor");
        }
        Arquivo arquivo = new Arquivo();
        if (pacienteId != null) {
            arquivo.setPaciente(pacienteRepository.findById(pacienteId)
                    .filter(paciente -> !Boolean.TRUE.equals(paciente.getDadosAnonimizados()))
                    .orElseThrow(() -> new IdNotFoundException("Paciente nao encontrado")));
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || original.length() > 200) {
            throw new IllegalArgumentException("Nome do arquivo invalido");
        }
        String nomeSeguro = original.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        String extensao = nomeSeguro.substring(nomeSeguro.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String mime = Map.of("pdf", "application/pdf", "png", "image/png", "jpg", "image/jpeg",
                "jpeg", "image/jpeg").get(extensao);
        if (mime == null || (file.getContentType() != null
                && !file.getContentType().equals("application/octet-stream") && !mime.equals(file.getContentType()))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use PDF, JPG ou PNG");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Limite de 10 MiB por arquivo");
        }
        byte[] conteudo;
        try (var stream = file.getInputStream()) {
            conteudo = stream.readNBytes(10 * 1024 * 1024 + 1);
        }
        if (conteudo.length > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Limite de 10 MiB por arquivo");
        }
        if (mime.equals("application/pdf")) {
            String pdf = new String(conteudo, StandardCharsets.ISO_8859_1);
            if (!pdf.startsWith("%PDF-") || !pdf.stripTrailing().endsWith("%%EOF")) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF invalido ou incompleto");
            }
        } else {
            boolean assinatura = mime.equals("image/png")
                    ? conteudo.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(conteudo, 8),
                            new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10})
                    : conteudo.length >= 3 && conteudo[0] == (byte) 255
                            && conteudo[1] == (byte) 216 && conteudo[2] == (byte) 255;
            if (!assinatura) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Imagem incompativel com o formato");
            }
            try {
                if (ImageIO.read(new ByteArrayInputStream(conteudo)) == null) {
                    throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Imagem invalida");
                }
            } catch (javax.imageio.IIOException ex) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Imagem invalida ou incompleta", ex);
            }
        }
        String nomeUnico = UUID.randomUUID() + "_" + nomeSeguro;
        Path destino = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(nomeUnico);
        Files.write(destino, conteudo);
        arquivo.setNomeOriginal(nomeSeguro);
        arquivo.setNome(nomeUnico);
        arquivo.setMimeType(mime);
        arquivo.setTamanho((long) conteudo.length);
        arquivo.setCaminhoArquivo(destino.toString());
        arquivo.setTipo(tipo);
        arquivo.setDataUpload(LocalDateTime.now());
        return arquivoRepository.save(arquivo);
    }

    public boolean podeRevisar() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> List.of("ROLE_ADMIN", "ROLE_MEDICO", "ROLE_ENFERMEIRO").contains(a.getAuthority()));
    }

    public List<Arquivo> listarPendentes() {
        return arquivoRepository.findPendentesValidacao();
    }

    @Transactional
    public Arquivo validar(Long id, StatusValidacao decisao, String observacao) {
        if (!podeRevisar()) throw new AccessDeniedException("Revisao restrita a equipe autorizada");
        if (decisao == null || decisao == StatusValidacao.PENDENTE) {
            throw new IllegalArgumentException("Informe APROVADO ou REJEITADO");
        }
        String texto = observacao == null ? "" : observacao.strip();
        if (texto.length() > 2000 || (decisao == StatusValidacao.REJEITADO && texto.isEmpty())) {
            throw new IllegalArgumentException("Informe observacao de rejeicao com ate 2000 caracteres");
        }
        Arquivo arquivo = arquivoRepository.findParaValidar(id)
                .orElseThrow(() -> new IdNotFoundException("Arquivo nao encontrado"));
        if (arquivo.getStatusValidacao() != StatusValidacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento ja revisado");
        }
        arquivo.setStatusValidacao(decisao);
        arquivo.setObservacaoValidacao(texto);
        arquivo.setValidadoEm(LocalDateTime.now());
        arquivo.setValidadoPor(SecurityContextHolder.getContext().getAuthentication().getName());
        return arquivoRepository.save(arquivo);
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
        if (arquivo.getStatusValidacao() != StatusValidacao.APROVADO && !podeRevisar()) {
            throw new AccessDeniedException("Documento ainda nao aprovado; acesso restrito a revisores");
        }
        Path path = Paths.get(arquivo.getCaminhoArquivo());
        return Files.readAllBytes(path);
    }
}
