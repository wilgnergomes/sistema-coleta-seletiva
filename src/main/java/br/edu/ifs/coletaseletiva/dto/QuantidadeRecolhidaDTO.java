package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.Unidade;

import java.math.BigDecimal;

/**
 * Total efetivamente recolhido de um material em uma unidade (segundo bloco do resumo gerencial, RF15). A unidade é a
 * copiada no item (ADR 0008), então unidades diferentes nunca se misturam em uma mesma linha.
 */
public record QuantidadeRecolhidaDTO(Integer materialId, String nomeMaterial, Unidade unidade, BigDecimal quantidade) {}
