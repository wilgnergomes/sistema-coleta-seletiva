package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corpo da correção antes do agendamento (schema SolicitacaoCorrecao): exige a versão lida pelo cliente. */
public record SolicitacaoCorrecaoForm(
        @NotNull(message = "{solicitacao.endereco-id.obrigatorio}")
        Integer enderecoId,

        @Size(max = 500, message = "{solicitacao.observacoes.tamanho-maximo}")
        String observacoes,

        @NotEmpty(message = "{solicitacao.itens.obrigatorio}")
        List<@NotNull(message = "{solicitacao.item.nulo}") @Valid ItemForm> itens,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        Integer versao
) {

    public SolicitacaoCorrecaoForm {
        observacoes = observacoes == null ? null : observacoes.trim();
    }
}
