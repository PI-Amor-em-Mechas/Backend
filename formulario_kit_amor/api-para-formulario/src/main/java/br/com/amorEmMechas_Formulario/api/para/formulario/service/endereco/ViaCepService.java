package br.com.amorEmMechas_Formulario.api.para.formulario.service.endereco;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.endereco.ViaCepResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ViaCepService {


    private final RestTemplate restTemplate = new RestTemplate();

    public ViaCepResponseDto buscarEnderecoPorCep(String cep) {
        String url = "https://viacep.com.br/ws/" + cep + "/json/";
        return restTemplate.getForObject(url, ViaCepResponseDto.class);
    }
}
