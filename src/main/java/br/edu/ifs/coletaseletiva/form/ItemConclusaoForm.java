package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Quantidade efetivamente recolhida de um item (RN02): não negativa, e zero para o item não recolhido. */
public record ItemConclusaoForm(
        @NotNull(message = "{item.material-id.obrigatorio}")
        Integer materialId,

        @NotNull(message = "{item.quantidade-efetiva.obrigatoria}")
        @DecimalMin(value = "0", message = "{item.quantidade-efetiva.minimo}")
        @Digits(integer = 9, fraction = 3, message = "{item.quantidade.formato}")
        BigDecimal quantidadeEfetiva
) {}
