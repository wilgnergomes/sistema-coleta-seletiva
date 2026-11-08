package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;

import java.math.BigDecimal;

/** Forma do item de {@code materiaisAceitos} no contrato. Será preenchido a partir do RF05. */
public record MaterialAceitoDTO(
        Integer materialId,
        String nomeMaterial,
        Unidade unidade,
        String orientacao,
        String observacoes,
        BigDecimal limite
) {}
