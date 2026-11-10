package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corpo da criação (schema SolicitacaoEntrada). O morador NÃO vem aqui: sai do ator (ADR 0019). */
public record SolicitacaoForm(
        @NotNull(message = "{solicitacao.ponto-id.obrigatorio}")
        @Schema(description = "Id do ponto de coleta. Precisa estar ativo, fazer recolhimento domiciliar e aceitar os materiais.", example = "1")
        Integer pontoId,

        @NotNull(message = "{solicitacao.endereco-id.obrigatorio}")
        @Schema(description = "Id de um endereço do próprio morador, onde o recolhimento será feito.", example = "3")
        Integer enderecoId,

        @Size(max = 500, message = "{solicitacao.observacoes.tamanho-maximo}")
        @Schema(description = "Observações para o operador. Opcional. Até 500 caracteres.", example = "Caixas na calçada")
        String observacoes,

        @NotEmpty(message = "{solicitacao.itens.obrigatorio}")
        @Schema(description = "Materiais a recolher. Pelo menos um item, sem repetir o mesmo material.")
        List<@NotNull(message = "{solicitacao.item.nulo}") @Valid ItemForm> itens
) {

    public SolicitacaoForm {
        observacoes = observacoes == null ? null : observacoes.trim();
    }
}
