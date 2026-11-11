package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** Linha da listagem (RF09): os campos do schema {@code Solicitacao}, sem itens nem histórico. */
public record SolicitacaoResumoDTO(
        @Schema(description = "Identificador da solicitação.", example = "1")
        Integer id,
        @Schema(description = "Código único de acompanhamento, no formato `SOL-AAAAMMDD-XXXXXX`.", example = "SOL-20261110-A7K3PQ")
        String protocolo,
        @Schema(description = "Id do morador que fez o pedido.", example = "1")
        Integer moradorId,
        @Schema(description = "Id do ponto de coleta escolhido.", example = "1")
        Integer pontoId,
        @Schema(description = "Cópia do endereço feita na criação.")
        String enderecoCopia,
        @Schema(description = "Situação atual da solicitação.")
        SituacaoSolicitacao situacao,
        @Schema(description = "Data e hora de criação.")
        OffsetDateTime criadaEm,
        @Schema(description = "Data e hora marcadas para o recolhimento. Nulo enquanto não agendada.")
        OffsetDateTime agendadaPara,
        @Schema(description = "Data e hora da conclusão. Nulo enquanto não concluída.")
        OffsetDateTime concluidaEm,
        @Schema(description = "Observações do morador. Pode ser nulo.")
        String observacoes,
        @Schema(description = "Versão atual da solicitação.", example = "0")
        Integer versao
) {}
