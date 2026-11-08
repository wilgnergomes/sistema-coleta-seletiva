package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corpo do vínculo ponto-material (schema VinculoEntrada). Os dois campos são opcionais. */
public record VinculoForm(
        @Size(max = 500, message = "{vinculo.observacoes.tamanho-maximo}")
        String observacoes,

        /* inclusive = false: o zero também é rejeitado ("maior que zero"). */
        @DecimalMin(value = "0", inclusive = false, message = "{vinculo.limite.minimo}")
        @Digits(integer = 9, fraction = 3, message = "{vinculo.limite.formato}")
        BigDecimal limite
) {

    public VinculoForm {
        observacoes = observacoes == null ? null : observacoes.trim();
    }
}
