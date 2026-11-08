package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;

import java.time.OffsetDateTime;
import java.util.List;

public record SolicitacaoDetalheDTO(
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
        Integer versao,
        List<ItemDTO> itens,
        List<HistoricoDTO> historico
) {}
