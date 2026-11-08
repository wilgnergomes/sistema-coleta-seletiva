package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;

import java.time.OffsetDateTime;

public record HistoricoDTO(
        Integer id,
        SituacaoSolicitacao situacaoAnterior,
        SituacaoSolicitacao situacaoNova,
        OffsetDateTime data,
        ResponsavelDTO responsavel,
        String motivo,
        OffsetDateTime agendamento,
        String informacoesTentativa
) {}
