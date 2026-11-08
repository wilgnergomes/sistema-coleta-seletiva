package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.entity.MaterialAceito;
import org.springframework.stereotype.Component;

@Component
public class MaterialAceitoMapper {

    public MaterialAceitoDTO paraDTO(MaterialAceito vinculo) {
        return new MaterialAceitoDTO(
                vinculo.getId().getMaterialId(),
                vinculo.getMaterial().getNome(),
                vinculo.getMaterial().getUnidade(),
                vinculo.getMaterial().getOrientacao(),
                vinculo.getObservacoes(),
                vinculo.getLimite()
        );
    }
}
