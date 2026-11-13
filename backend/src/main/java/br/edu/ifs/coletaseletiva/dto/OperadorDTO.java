package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import io.swagger.v3.oas.annotations.media.Schema;

public record OperadorDTO(
        @Schema(description = "Identificador do operador.", example = "1")
        Integer id,
        String nome,
        String email,
        @Schema(description = "Id do ponto de coleta ao qual o operador pertence.", example = "1")
        Integer pontoId,
        @Schema(description = "`ATIVO` ou `INATIVO`. Operador inativo não pode agir sobre solicitações.")
        SituacaoCadastro situacao
) {}
