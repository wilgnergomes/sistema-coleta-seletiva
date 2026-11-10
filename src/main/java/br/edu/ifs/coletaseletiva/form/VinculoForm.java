package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corpo do vínculo ponto-material (schema VinculoEntrada). Os dois campos são opcionais. */
public record VinculoForm(
        @Size(max = 500, message = "{vinculo.observacoes.tamanho-maximo}")
        @Schema(description = "Observações sobre o material neste ponto. Opcional. Até 500 caracteres.", example = "Aceita apenas embalagens limpas")
        String observacoes,

        /* inclusive = false: o zero também é rejeitado ("maior que zero"). */
        @DecimalMin(value = "0", inclusive = false, message = "{vinculo.limite.minimo}")
        @Digits(integer = 9, fraction = 3, message = "{vinculo.limite.formato}")
        @Schema(description = "Quantidade máxima aceita por solicitação, maior que zero, com até 3 casas decimais. Opcional; sem limite se omitido.", example = "50")
        BigDecimal limite
) {

    public VinculoForm {
        observacoes = observacoes == null ? null : observacoes.trim();
    }
}
