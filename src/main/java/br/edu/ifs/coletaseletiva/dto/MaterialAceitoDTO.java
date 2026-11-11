package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Forma do item de {@code materiaisAceitos} no contrato. Será preenchido a partir do RF05. */
public record MaterialAceitoDTO(
        @Schema(description = "Id do material aceito.", example = "1")
        Integer materialId,
        String nomeMaterial,
        Unidade unidade,
        @Schema(description = "Orientação do material, a mesma do cadastro do material.")
        String orientacao,
        @Schema(description = "Observações sobre o material neste ponto.")
        String observacoes,
        @Schema(description = "Quantidade máxima por solicitação. Nulo quando não há limite.")
        BigDecimal limite
) {}
