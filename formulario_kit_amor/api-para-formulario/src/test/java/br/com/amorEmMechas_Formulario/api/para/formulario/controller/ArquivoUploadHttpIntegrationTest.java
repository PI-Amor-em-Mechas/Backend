package br.com.amorEmMechas_Formulario.api.para.formulario.controller;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo.ArquivoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ArquivoUploadHttpIntegrationTest {
    @TempDir static Path uploads;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> uploads.toString());
    }

    @Autowired TestRestTemplate http;
    @Autowired ArquivoRepository arquivos;
    private final List<Long> criados = new ArrayList<>();

    @AfterEach
    void limparRegistros() {
        criados.forEach(arquivos::deleteById);
    }

    private HttpEntity<MultiValueMap<String, Object>> multipart(int tamanho) throws Exception {
        var buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", buffer);
        var conteudo = new ByteArrayResource(Arrays.copyOf(buffer.toByteArray(), tamanho)) {
            @Override
            public String getFilename() { return "limite.png"; }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("arquivo", new HttpEntity<>(conteudo, partHeaders));
        body.add("tipo", "RELATORIO_MEDICO");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void limiteExatoConsideraOverheadMultipartEPersisteUploadPublico() throws Exception {
        int limite = 10 * 1024 * 1024;
        var response = http.postForEntity("/arquivos", multipart(limite), ArquivoResponseDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var arquivo = response.getBody();
        assertThat(arquivo).isNotNull();
        criados.add(arquivo.getId());
        assertThat(arquivo.getTamanho()).isEqualTo(limite);
        assertThat(arquivos.findById(arquivo.getId())).isPresent();
        assertThat(http.getForEntity("/arquivos/" + arquivo.getId(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void byteAcimaDoLimiteRetorna413MesmoSemAutenticacao() throws Exception {
        var response = http.postForEntity("/arquivos", multipart(10 * 1024 * 1024 + 1), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).contains("Limite de 10 MiB");
    }
}
