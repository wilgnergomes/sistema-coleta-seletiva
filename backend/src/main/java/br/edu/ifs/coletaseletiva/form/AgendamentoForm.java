package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.VERSAO_DESCRICAO;

/**
 * Corpo do agendamento (RF10). A data precisa ser futura, o que é conferido no serviço com o relógio injetado.
 * A versão é a que o cliente leu (ADR 0009). O responsável nunca vem do corpo: sai do ator (ADR 0011).
 */
public record AgendamentoForm(
        @NotNull(message = "{agendamento.data-hora.obrigatoria}")
        @Schema(description = "Data e hora do recolhimento, com deslocamento de fuso. Precisa ser posterior ao momento atual.", example = "2026-12-15T09:00:00-03:00")
        OffsetDateTime dataHora,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        @Schema(description = VERSAO_DESCRICAO, example = "0")
        Integer versao
) {}
