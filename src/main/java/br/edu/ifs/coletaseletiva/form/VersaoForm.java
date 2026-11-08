package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo das ações que só precisam da versão lida pelo cliente (schema {@code VersaoEntrada} do contrato), como o
 * início do recolhimento (RF13). Se a versão não for a atual, a ação é rejeitada com 409 (ADR 0009).
 */
public record VersaoForm(
        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        Integer versao
) {}
