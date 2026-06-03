package br.com.amorEmMechas_Formulario.api.para.formulario.service.arquivo;


import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.arquivo.ArquivoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ArquivoService {



private final ArquivoRepository arquivoRepository;

    public ArquivoService(ArquivoRepository arquivoRepository) {
        this.arquivoRepository = arquivoRepository;
    }

    public Arquivo upload(MultipartFile file, String tipo) throws IOException {

        Arquivo arquivo = new Arquivo();

        arquivo.setNomeOriginal(file.getOriginalFilename());
        arquivo.setMimeType(file.getContentType());

        // 🔥 ISSO AQUI ESTAVA FALTANDO
        arquivo.setNome(file.getOriginalFilename());

        // 🔥 tamanho do arquivo em bytes
        arquivo.setTamanho(file.getSize());

        arquivo.setConteudo(file.getBytes());

        arquivo.setTipo(tipo);

        return arquivoRepository.save(arquivo);
    }


public List<Arquivo> listUpload(){
        return arquivoRepository.findAll();
}

    public Arquivo buscarPorId(Long id){

        return arquivoRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST));

    }
}
