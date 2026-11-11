package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.MOTIVO_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.VERSAO_DESCRICAO;

/**
 * Corpo das ações que exigem justificativa (schema {@code MotivoEntrada} do contrato): a recusa (RF11) e, depois,
 * o cancelamento (RF12). Texto ausente, vazio ou só com espaços não é justificativa (400 no campo {@code motivo}).
 * O motivo é guardado sem os espaços das pontas. Quem é o responsável do histórico não vem daqui: sai do ator.
 */
public record MotivoForm(
        @NotBlank(message = "{motivo.obrigatorio}")
        @Size(max = 500, message = "{motivo.tamanho-maximo}")
        @Schema(description = MOTIVO_DESCRICAO, example = "Não será possível atender neste período")
        String motivo,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        @Schema(description = VERSAO_DESCRICAO, example = "0")
        Integer versao
) {

    public MotivoForm {
        motivo = motivo == null ? null : motivo.trim();
    }
}
