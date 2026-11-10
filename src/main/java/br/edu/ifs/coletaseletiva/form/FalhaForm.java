package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.MOTIVO_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.VERSAO_DESCRICAO;

/**
 * Corpo do registro de tentativa sem sucesso (RF14, schema {@code FalhaEntrada}): a justificativa e as informações
 * da tentativa, ambas obrigatórias e guardadas sem os espaços das pontas, a nova data futura e a versão lida pelo
 * cliente. Aqui a nova data pode coincidir com a anterior, porque a situação muda de EM_RECOLHIMENTO para AGENDADA.
 */
public record FalhaForm(
        @NotBlank(message = "{motivo.obrigatorio}")
        @Size(max = 500, message = "{motivo.tamanho-maximo}")
        @Schema(description = MOTIVO_DESCRICAO, example = "Morador ausente")
        String motivo,

        @NotBlank(message = "{informacoes-tentativa.obrigatorio}")
        @Size(max = 500, message = "{informacoes-tentativa.tamanho-maximo}")
        @Schema(description = "Relato da tentativa sem sucesso. Obrigatório, até 500 caracteres.", example = "Portão fechado, ninguém atendeu")
        String informacoesTentativa,

        @NotNull(message = "{agendamento.data-hora.obrigatoria}")
        @Schema(description = "Nova data e hora, com deslocamento de fuso. Precisa ser posterior ao momento atual (pode ser igual à data anterior).", example = "2026-12-17T09:00:00-03:00")
        OffsetDateTime dataHora,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        @Schema(description = VERSAO_DESCRICAO, example = "2")
        Integer versao
) {

    public FalhaForm {
        motivo = motivo == null ? null : motivo.trim();
        informacoesTentativa = informacoesTentativa == null ? null : informacoesTentativa.trim();
    }
}
