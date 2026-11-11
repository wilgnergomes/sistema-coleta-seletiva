package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.MOTIVO_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.VERSAO_DESCRICAO;

/**
 * Corpo do reagendamento (RF14, schema {@code ReagendamentoEntrada}): a nova data futura (conferida no serviço com o
 * relógio injetado), a justificativa obrigatória e a versão lida pelo cliente (ADR 0009). O motivo é guardado sem os
 * espaços das pontas. O responsável do histórico não vem daqui: sai do ator (ADR 0011).
 */
public record ReagendamentoForm(
        @NotNull(message = "{agendamento.data-hora.obrigatoria}")
        @Schema(description = "Nova data e hora, com deslocamento de fuso. Precisa ser posterior ao momento atual e diferente da data já marcada.", example = "2026-12-16T14:00:00-03:00")
        OffsetDateTime dataHora,

        @NotBlank(message = "{motivo.obrigatorio}")
        @Size(max = 500, message = "{motivo.tamanho-maximo}")
        @Schema(description = MOTIVO_DESCRICAO, example = "Veículo em manutenção")
        String motivo,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        @Schema(description = VERSAO_DESCRICAO, example = "1")
        Integer versao
) {

    public ReagendamentoForm {
        motivo = motivo == null ? null : motivo.trim();
    }
}
