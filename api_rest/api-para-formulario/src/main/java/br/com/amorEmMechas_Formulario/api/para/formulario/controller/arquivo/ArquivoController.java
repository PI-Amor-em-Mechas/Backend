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
            @RequestParam("tipo") String tipo) {

        Arquivo arquivoSalvo = null;
        try {
            arquivoSalvo = arquivoService.upload(arquivo, tipo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(ArquivoMapper.toResponse(arquivoSalvo));
    }

    @GetMapping
    public ResponseEntity<List<ArquivoResponseDto>> listUpload(){
        List<Arquivo> arquivos = arquivoService.listUpload();
        return ResponseEntity.ok(ArquivoMapper.toResponseList(arquivos));

    }
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id){
        Arquivo arquivo = arquivoService.buscarPorId(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(arquivo.getMimeType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(arquivo.getNomeOriginal()).build());
        return ResponseEntity.ok().headers(headers).body(arquivo.getConteudo());
    }



}
