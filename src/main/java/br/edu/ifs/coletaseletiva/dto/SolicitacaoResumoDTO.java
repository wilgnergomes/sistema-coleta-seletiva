package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;

import java.time.OffsetDateTime;

/** Linha da listagem (RF09): os campos do schema {@code Solicitacao}, sem itens nem histórico. */
public record SolicitacaoResumoDTO(
        Integer id,
        String protocolo,
        Integer moradorId,
        Integer pontoId,
        String enderecoCopia,
        SituacaoSolicitacao situacao,
        OffsetDateTime criadaEm,
        OffsetDateTime agendadaPara,
        OffsetDateTime concluidaEm,
        String observacoes,
        Integer versao
) {}
