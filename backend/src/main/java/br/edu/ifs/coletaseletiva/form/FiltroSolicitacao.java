package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;

import java.time.LocalDate;

/**
 * Filtros da consulta de solicitações (RF09). Todos são opcionais e os informados se combinam (E).
 * O período {@code de}/{@code ate} vale para a data de criação, com os dois dias inclusive (ADR 0014).
 */
public record FiltroSolicitacao(
        SituacaoSolicitacao situacao,
        Integer pontoId,
        LocalDate de,
        LocalDate ate
) {}
