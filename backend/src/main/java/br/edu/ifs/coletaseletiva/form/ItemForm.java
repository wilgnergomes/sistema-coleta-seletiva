package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Item pedido: material e quantidade estimada positiva (RN02). */
public record ItemForm(
        @NotNull(message = "{item.material-id.obrigatorio}")
        @Schema(description = "Id do material (ativo e aceito pelo ponto).", example = "1")
        Integer materialId,

        @NotNull(message = "{item.quantidade.obrigatoria}")
        @DecimalMin(value = "0", inclusive = false, message = "{item.quantidade.minimo}")
        @Digits(integer = 9, fraction = 3, message = "{item.quantidade.formato}")
        @Schema(description = "Quantidade estimada, maior que zero, com até 3 casas decimais. Em `UN`, só inteiros. Pode ter um limite definido pelo ponto.", example = "5.5")
        BigDecimal quantidadeEstimada
) {}
