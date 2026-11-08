package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Corpo do registro de tentativa sem sucesso (RF14, schema {@code FalhaEntrada}): a justificativa e as informações
 * da tentativa, ambas obrigatórias e guardadas sem os espaços das pontas, a nova data futura e a versão lida pelo
 * cliente. Aqui a nova data pode coincidir com a anterior, porque a situação muda de EM_RECOLHIMENTO para AGENDADA.
 */
public record FalhaForm(
        @NotBlank(message = "{motivo.obrigatorio}")
        @Size(max = 500, message = "{motivo.tamanho-maximo}")
        String motivo,

        @NotBlank(message = "{informacoes-tentativa.obrigatorio}")
        @Size(max = 500, message = "{informacoes-tentativa.tamanho-maximo}")
        String informacoesTentativa,

        @NotNull(message = "{agendamento.data-hora.obrigatoria}")
        OffsetDateTime dataHora,

        @NotNull(message = "{solicitacao.versao.obrigatoria}")
        @Min(value = 0, message = "{solicitacao.versao.minimo}")
        Integer versao
) {

    public FalhaForm {
        motivo = motivo == null ? null : motivo.trim();
        informacoesTentativa = informacoesTentativa == null ? null : informacoesTentativa.trim();
    }
}
