package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Item pedido: material e quantidade estimada positiva (RN02). */
public record ItemForm(
        @NotNull(message = "{item.material-id.obrigatorio}")
        Integer materialId,

        @NotNull(message = "{item.quantidade.obrigatoria}")
        @DecimalMin(value = "0", inclusive = false, message = "{item.quantidade.minimo}")
        @Digits(integer = 9, fraction = 3, message = "{item.quantidade.formato}")
        BigDecimal quantidadeEstimada
) {}
