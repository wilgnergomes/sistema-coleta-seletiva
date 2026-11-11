package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record SolicitacaoDetalheDTO(
        @Schema(description = "Identificador da solicitação.", example = "1")
        Integer id,
        @Schema(description = "Código único de acompanhamento, no formato `SOL-AAAAMMDD-XXXXXX`.", example = "SOL-20261110-A7K3PQ")
        String protocolo,
        @Schema(description = "Id do morador que fez o pedido.", example = "1")
        Integer moradorId,
        @Schema(description = "Id do ponto de coleta escolhido.", example = "1")
        Integer pontoId,
        @Schema(description = "Cópia do endereço feita na criação; não muda se o cadastro do endereço mudar depois.")
        String enderecoCopia,
        @Schema(description = "Situação atual: `SOLICITADA`, `AGENDADA`, `EM_RECOLHIMENTO`, `CONCLUIDA`, `RECUSADA` ou `CANCELADA`.")
        SituacaoSolicitacao situacao,
        @Schema(description = "Data e hora de criação.")
        OffsetDateTime criadaEm,
        @Schema(description = "Data e hora marcadas para o recolhimento. Nulo enquanto não agendada.")
        OffsetDateTime agendadaPara,
        @Schema(description = "Data e hora da conclusão. Nulo enquanto não concluída.")
        OffsetDateTime concluidaEm,
        @Schema(description = "Observações do morador. Pode ser nulo.")
        String observacoes,
        @Schema(description = "Versão atual. Informe-a nas ações sobre a solicitação; ela sobe 1 a cada alteração.", example = "0")
        Integer versao,
        @Schema(description = "Itens da solicitação, ordenados pelo id do material.")
        List<ItemDTO> itens,
        @Schema(description = "Histórico de mudanças de situação, do mais antigo para o mais recente.")
        List<HistoricoDTO> historico
) {}
