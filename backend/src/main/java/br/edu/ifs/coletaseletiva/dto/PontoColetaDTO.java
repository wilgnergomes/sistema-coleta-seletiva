package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PontoColetaDTO(
        @Schema(description = "Identificador do ponto de coleta.", example = "1")
        Integer id,
        String nome,
        String contato,
        String horarios,
        @Schema(description = "Se o ponto recolhe na casa do morador.")
        boolean recolhimentoDomiciliar,
        @Schema(description = "`ATIVO` ou `INATIVO`. Só o administrador enxerga pontos inativos.")
        SituacaoCadastro situacao,
        EnderecoPontoDTO endereco,
        @Schema(description = "Materiais que o ponto aceita, com orientações e limites.")
        List<MaterialAceitoDTO> materiaisAceitos
) {}
