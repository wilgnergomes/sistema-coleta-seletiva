package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.VERSAO_DESCRICAO;

/** Corpo da correção antes do agendamento (schema SolicitacaoCorrecao): exige a versão lida pelo cliente. */
public record SolicitacaoCorrecaoForm(
        @NotNull(message = "{solicitacao.endereco-id.obrigatorio}")
        @Schema(description = "Id do endereço do morador onde o recolhimento será feito.", example = "3")
        Integer enderecoId,

        @Size(max = 500, message = "{solicitacao.observacoes.tamanho-maximo}")
        @Schema(description = "Observações para o operador. Opcional. Até 500 caracteres.", example = "Caixas na calçada")
        String observacoes,

        @NotEmpty(message = "{solicitacao.itens.obrigatorio}")
        @Schema(description = "Novos itens da solicitação, no lugar dos atuais. Pelo menos um, sem repetir o mesmo material.")
        List<@NotNull(message = "{solicitacao.item.nulo}") @Valid ItemForm> itens,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        @Schema(description = VERSAO_DESCRICAO, example = "0")
        Integer versao
) {

    public SolicitacaoCorrecaoForm {
        observacoes = observacoes == null ? null : observacoes.trim();
    }
}
