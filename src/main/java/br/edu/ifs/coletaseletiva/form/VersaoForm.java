package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.VERSAO_DESCRICAO;

/**
 * Corpo das ações que só precisam da versão lida pelo cliente (schema {@code VersaoEntrada} do contrato), como o
 * início do recolhimento (RF13). Se a versão não for a atual, a ação é rejeitada com 409 (ADR 0009).
 */
public record VersaoForm(
        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        @Schema(description = VERSAO_DESCRICAO, example = "1")
        Integer versao
) {}
