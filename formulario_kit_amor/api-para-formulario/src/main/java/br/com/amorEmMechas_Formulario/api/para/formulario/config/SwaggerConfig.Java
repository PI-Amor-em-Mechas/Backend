package br.com.amorEmMechas_Formulario.api.para.formulario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Amor em Mechas - API de Formulário")
                        .description("API para gerenciamento de solicitações de perucas para pacientes em tratamento")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Amor em Mechas")
                                .email("contato@amoremmechas.com.br")));
    }
}
