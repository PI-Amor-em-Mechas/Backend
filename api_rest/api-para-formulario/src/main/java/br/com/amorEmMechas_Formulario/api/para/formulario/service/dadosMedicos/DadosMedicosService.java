package br.com.amorEmMechas_Formulario.api.para.formulario.service.dadosMedicos;


import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosRequestDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.dto.dadosMedicos.DadosMedicosResponseDto;
import br.com.amorEmMechas_Formulario.api.para.formulario.entity.dadosMedicos.DadosMedicos;
import br.com.amorEmMechas_Formulario.api.para.formulario.exception.IdNotFoundException;
import br.com.amorEmMechas_Formulario.api.para.formulario.mapper.dadosMedicos.DadosMedicosMapper;
import br.com.amorEmMechas_Formulario.api.para.formulario.repository.dadosMedicos.DadosMedicosRepository;
import org.springframework.stereotype.Service;

@Service
public class DadosMedicosService {


    private DadosMedicosRepository repository;
    private DadosMedicosMapper mapper;

    public DadosMedicosService(DadosMedicosMapper mapper, DadosMedicosRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }


    public DadosMedicosResponseDto create (DadosMedicosRequestDto dto){
        DadosMedicos entity = mapper.toEntity(dto);
        DadosMedicos saved = repository.save(entity);
        return mapper.toResponse(saved);

    }

    public DadosMedicosResponseDto update (Integer id, DadosMedicosRequestDto dto){
        DadosMedicos dadosMedicos = repository.findById(id).orElseThrow(() -> new IdNotFoundException("ID " + id + " Não foi encontrado"));

        dadosMedicos.setMotivo(dto.getMotivo());
        dadosMedicos.setRelatorioMedico(dadosMedicos.getRelatorioMedico());
        dadosMedicos.setDtInicioTratamento(dadosMedicos.getDtInicioTratamento());
        dadosMedicos.setTipoAtendimento(dto.getTipoAtendimento());
        dadosMedicos.setJustificativa(dto.getJustificativa());
        dadosMedicos.setTipoCancer(dto.getTipoCancer());

        DadosMedicos dadosSave = repository.save(dadosMedicos);
        return mapper.toResponse(dadosSave);
    }


}
