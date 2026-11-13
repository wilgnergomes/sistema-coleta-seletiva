package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

public record HistoricoDTO(
        @Schema(description = "Identificador da linha do histórico.", example = "1")
        Integer id,
        @Schema(description = "Situação antes da mudança. Nulo na primeira linha (criação).")
        SituacaoSolicitacao situacaoAnterior,
        @Schema(description = "Situação depois da mudança.")
        SituacaoSolicitacao situacaoNova,
        @Schema(description = "Data e hora da mudança.")
        OffsetDateTime data,
        @Schema(description = "Quem fez a mudança: um morador ou um operador.")
        ResponsavelDTO responsavel,
        @Schema(description = "Justificativa, quando a ação exige uma. Nulo nos demais casos.")
        String motivo,
        @Schema(description = "Data e hora marcadas, nas ações de agendamento. Nulo nos demais casos.")
        OffsetDateTime agendamento,
        @Schema(description = "Relato da tentativa sem sucesso. Nulo nos demais casos.")
        String informacoesTentativa
) {}
