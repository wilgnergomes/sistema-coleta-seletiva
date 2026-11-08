package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Corpo do reagendamento (RF14, schema {@code ReagendamentoEntrada}): a nova data futura (conferida no serviço com o
 * relógio injetado), a justificativa obrigatória e a versão lida pelo cliente (ADR 0009). O motivo é guardado sem os
 * espaços das pontas. O responsável do histórico não vem daqui: sai do ator (ADR 0011).
 */
public record ReagendamentoForm(
        @NotNull(message = "{agendamento.data-hora.obrigatoria}")
        OffsetDateTime dataHora,

        @NotBlank(message = "{motivo.obrigatorio}")
        @Size(max = 500, message = "{motivo.tamanho-maximo}")
        String motivo,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        Integer versao
) {

    public ReagendamentoForm {
        motivo = motivo == null ? null : motivo.trim();
    }
}
