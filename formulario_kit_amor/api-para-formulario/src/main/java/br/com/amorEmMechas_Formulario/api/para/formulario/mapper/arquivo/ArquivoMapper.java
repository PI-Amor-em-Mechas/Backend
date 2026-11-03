package br.com.amorEmMechas_Formulario.api.para.formulario.mapper.arquivo;

import br.com.amorEmMechas_Formulario.api.para.formulario.dto.arquivo.ArquivoResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.arquivo.Arquivo;

import java.util.ArrayList;
import java.util.List;

public class ArquivoMapper {

    public static ArquivoResponseDto toResponse(Arquivo arquivo){
        ArquivoResponseDto arquivoResponseDto = new ArquivoResponseDto();
        arquivoResponseDto.setId(arquivo.getId().longValue());
        arquivoResponseDto.setNome(arquivo.getNome());
        arquivoResponseDto.setMimeType(arquivo.getMimeType());
        arquivoResponseDto.setTamanho(arquivo.getTamanho());
        arquivoResponseDto.setNomeOriginal(arquivo.getNomeOriginal());

        return arquivoResponseDto;
    }


    public static List<ArquivoResponseDto> toResponseList(List<Arquivo> arquivos){

        List<ArquivoResponseDto> dtos = new ArrayList<>();

        for (Arquivo arquivo : arquivos) {
            dtos.add(toResponse(arquivo));
        }

        return dtos;

    }

}
