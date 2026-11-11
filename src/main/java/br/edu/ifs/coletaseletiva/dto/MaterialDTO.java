package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;

public record MaterialDTO(
        @Schema(description = "Identificador do material.", example = "1")
        Integer id,
        String nome,
        String descricao,
        @Schema(description = "`KG` (peso), `L` (volume) ou `UN` (peça).")
        Unidade unidade,
        String orientacao,
        @Schema(description = "`ATIVO` ou `INATIVO`. Só o administrador enxerga materiais inativos.")
        SituacaoCadastro situacao
) {}
