package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corpo da conclusão do recolhimento (RF13, schema {@code ConclusaoEntrada}). O {@code @NotNull} no elemento da lista
 * evita que {@code [null]} passe pela validação e quebre o serviço (achado C-04 da auditoria). A data da conclusão e o
 * responsável não vêm daqui: a data é do servidor e o responsável sai do ator (ADR 0011).
 */
public record ConclusaoForm(
        @NotEmpty(message = "{conclusao.itens.obrigatorio}")
        List<@NotNull(message = "{conclusao.item.nulo}") @Valid ItemConclusaoForm> itens,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        Integer versao
) {}
