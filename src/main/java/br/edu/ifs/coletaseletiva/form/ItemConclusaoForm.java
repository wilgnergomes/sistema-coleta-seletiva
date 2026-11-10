package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Quantidade efetivamente recolhida de um item (RN02): não negativa, e zero para o item não recolhido. */
public record ItemConclusaoForm(
        @NotNull(message = "{item.material-id.obrigatorio}")
        @Schema(description = "Id do material, que precisa ser um item da solicitação.", example = "1")
        Integer materialId,

        @NotNull(message = "{item.quantidade-efetiva.obrigatoria}")
        @DecimalMin(value = "0", message = "{item.quantidade-efetiva.minimo}")
        @Digits(integer = 9, fraction = 3, message = "{item.quantidade.formato}")
        @Schema(description = "Quantidade efetivamente recolhida: zero ou mais, com até 3 casas decimais. Use `0` para o item não recolhido. Item em `UN` só aceita inteiros.", example = "5")
        BigDecimal quantidadeEfetiva
) {}
