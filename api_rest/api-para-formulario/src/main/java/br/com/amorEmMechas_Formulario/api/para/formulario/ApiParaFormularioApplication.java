package br.com.amorEmMechas_Formulario.api.para.formulario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApiParaFormularioApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiParaFormularioApplication.class, args);
	}

}
