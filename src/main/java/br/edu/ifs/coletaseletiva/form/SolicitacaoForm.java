package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corpo da criação (schema SolicitacaoEntrada). O morador NÃO vem aqui: sai do ator (ADR 0019). */
public record SolicitacaoForm(
        @NotNull(message = "{solicitacao.ponto-id.obrigatorio}")
        Integer pontoId,

        @NotNull(message = "{solicitacao.endereco-id.obrigatorio}")
        Integer enderecoId,

        @Size(max = 500, message = "{solicitacao.observacoes.tamanho-maximo}")
        String observacoes,

        @NotEmpty(message = "{solicitacao.itens.obrigatorio}")
        List<@NotNull(message = "{solicitacao.item.nulo}") @Valid ItemForm> itens
) {

    public SolicitacaoForm {
        observacoes = observacoes == null ? null : observacoes.trim();
    }
}
