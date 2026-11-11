package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Total efetivamente recolhido de um material em uma unidade (segundo bloco do resumo gerencial, RF15). A unidade é a
 * copiada no item (ADR 0008), então unidades diferentes nunca se misturam em uma mesma linha.
 */
public record QuantidadeRecolhidaDTO(
        @Schema(description = "Id do material.", example = "1")
        Integer materialId,
        @Schema(description = "Nome do material.")
        String nomeMaterial,
        @Schema(description = "Unidade copiada nos itens. Unidades diferentes nunca são somadas.")
        Unidade unidade,
        @Schema(description = "Soma das quantidades efetivas recolhidas.")
        BigDecimal quantidade
) {}
