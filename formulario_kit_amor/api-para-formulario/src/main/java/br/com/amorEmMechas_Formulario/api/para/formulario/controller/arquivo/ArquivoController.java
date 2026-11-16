package br.com.amorEmMechas_Formulario.api.para.formulario.controller.arquivo;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo.ArquivoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.arquivo.ArquivoMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo.ArquivoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo.ValidacaoArquivoRequestDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/arquivos")
public class ArquivoController {

    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArquivoResponseDto> upload(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "pacienteId", required = false) Integer pacienteId,
            @RequestHeader(value = "X-Application-Token", required = false) String applicationToken) throws IOException {

        Arquivo arquivoSalvo = arquivoService.upload(arquivo, tipo, pacienteId, applicationToken);
        return ResponseEntity.ok(ArquivoMapper.toResponse(arquivoSalvo));
    }

    @GetMapping("/pendentes-validacao")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMEIRO')")
    public ResponseEntity<List<ArquivoResponseDto>> pendentes() {
        return ResponseEntity.ok(ArquivoMapper.toResponseList(arquivoService.listarPendentes()));
    }

    @PatchMapping("/{id}/validacao")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'ENFERMEIRO')")
    public ResponseEntity<ArquivoResponseDto> validar(@PathVariable Integer id,
            @RequestBody @Valid ValidacaoArquivoRequestDto dto) {
        return ResponseEntity.ok(ArquivoMapper.toResponse(arquivoService.validar(id, dto.decisao(), dto.observacao())));
    }

    @GetMapping
    public ResponseEntity<List<ArquivoResponseDto>> listUpload() {
        List<Arquivo> arquivos = arquivoService.listUpload();
        return ResponseEntity.ok(ArquivoMapper.toResponseList(arquivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArquivoResponseDto> buscarPorId(@PathVariable Integer id) {
        Arquivo arquivo = arquivoService.buscarPorId(id);
        return ResponseEntity.ok(ArquivoMapper.toResponse(arquivo));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@PathVariable Integer id) {
        Arquivo arquivo = arquivoService.buscarPorId(id);

        try {
            byte[] conteudo = arquivoService.getConteudo(id);
            HttpHeaders headers = new HttpHeaders();
            MediaType mimeType = MediaType.APPLICATION_OCTET_STREAM;
            if (arquivo.getMimeType() != null) {
                try {
                    mimeType = MediaType.parseMediaType(arquivo.getMimeType());
                } catch (IllegalArgumentException ignored) {
                    // Conteúdo desconhecido permanece como octet-stream.
                }
            }
            String nomeSeguro = arquivo.getNomeOriginal() == null
                    ? "arquivo-" + id
                    : arquivo.getNomeOriginal().replaceAll("[^\\p{L}\\p{N}._-]", "_");
            headers.setContentType(mimeType);
            headers.setContentDisposition(ContentDisposition.attachment().filename(nomeSeguro).build());
            return ResponseEntity.ok().headers(headers).body(conteudo);
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}