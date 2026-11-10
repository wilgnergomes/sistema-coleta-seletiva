package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ItemDTO(
        @Schema(description = "Id do material.", example = "1")
        Integer materialId,
        @Schema(description = "Nome do material.")
        String nomeMaterial,
        @Schema(description = "Unidade copiada do material na criação da solicitação.")
        Unidade unidade,
        @Schema(description = "Quantidade pedida pelo morador.")
        BigDecimal quantidadeEstimada,
        @Schema(description = "Quantidade realmente recolhida. Nulo até a conclusão.")
        BigDecimal quantidadeEfetiva
) {}
