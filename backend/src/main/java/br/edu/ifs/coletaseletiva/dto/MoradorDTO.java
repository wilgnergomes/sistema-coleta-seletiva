package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import io.swagger.v3.oas.annotations.media.Schema;

public record MoradorDTO(
        @Schema(description = "Identificador do morador.", example = "1")
        Integer id,
        String nome,
        String email,
        String fone,
        @Schema(description = "`ATIVO` ou `INATIVO`. Morador inativo não pode criar solicitações.")
        SituacaoCadastro situacao
) {}