package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;

/** Quantas solicitações estão em uma situação (primeiro bloco do resumo gerencial, RF15). */
public record SituacaoTotalDTO(SituacaoSolicitacao situacao, long total) {}
