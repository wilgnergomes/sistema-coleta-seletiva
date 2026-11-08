package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;

import java.math.BigDecimal;

public record ItemDTO(
        Integer materialId,
        String nomeMaterial,
        Unidade unidade,
        BigDecimal quantidadeEstimada,
        BigDecimal quantidadeEfetiva
) {}
