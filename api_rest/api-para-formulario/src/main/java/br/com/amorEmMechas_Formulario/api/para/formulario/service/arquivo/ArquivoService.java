package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
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

@Service
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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

    @Transactional
    public Arquivo upload(MultipartFile file, String tipo) throws IOException {
        String nomeUnico = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destino = Paths.get(uploadDir).resolve(nomeUnico);

        Files.copy(file.getInputStream(), destino);

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeOriginal(file.getOriginalFilename());
        arquivo.setNome(nomeUnico);
        arquivo.setMimeType(file.getContentType());
        arquivo.setTamanho(file.getSize());
        arquivo.setCaminhoArquivo(destino.toString());
        arquivo.setTipo(tipo);

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
        Path path = Paths.get(arquivo.getCaminhoArquivo());
        return Files.readAllBytes(path);
    }
}
