package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import io.swagger.v3.oas.annotations.media.Schema;

/** Quantas solicitações estão em uma situação (primeiro bloco do resumo gerencial, RF15). */
public record SituacaoTotalDTO(
        @Schema(description = "Situação da solicitação.")
        SituacaoSolicitacao situacao,
        @Schema(description = "Quantidade de solicitações nessa situação (zero quando não há).")
        long total
) {}
