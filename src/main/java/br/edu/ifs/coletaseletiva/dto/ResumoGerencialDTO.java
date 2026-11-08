package br.edu.ifs.coletaseletiva.dto;

import java.util.List;

/** Resumo gerencial (RF15): solicitações por situação e quantidades recolhidas por material e unidade. */
public record ResumoGerencialDTO(
        List<SituacaoTotalDTO> solicitacoesPorSituacao,
        List<QuantidadeRecolhidaDTO> quantidadesRecolhidas
) {}
