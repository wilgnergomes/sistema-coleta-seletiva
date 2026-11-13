package br.edu.ifs.coletaseletiva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Resumo gerencial (RF15): solicitações por situação e quantidades recolhidas por material e unidade. */
public record ResumoGerencialDTO(
        @Schema(description = "Total de solicitações em cada uma das seis situações, filtrado pela data de criação.")
        List<SituacaoTotalDTO> solicitacoesPorSituacao,
        @Schema(description = "Quantidade efetivamente recolhida por material e unidade, filtrada pela data de conclusão.")
        List<QuantidadeRecolhidaDTO> quantidadesRecolhidas
) {}
