package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Corpo do agendamento (RF10). A data precisa ser futura, o que é conferido no serviço com o relógio injetado.
 * A versão é a que o cliente leu (ADR 0009). O responsável nunca vem do corpo: sai do ator (ADR 0011).
 */
public record AgendamentoForm(
        @NotNull(message = "{agendamento.data-hora.obrigatoria}")
        OffsetDateTime dataHora,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        Integer versao
) {}
