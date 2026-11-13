package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SITUACAO_CADASTRO_CORPO;

/** Corpo de criação e atualização de operador (schema OperadorEntrada). {@code situacao} é opcional. */
public record OperadorForm(
        @NotBlank(message = "{operador.nome.obrigatorio}")
        @Size(max = 80, message = "{operador.nome.tamanho-maximo}")
        @Schema(description = "Nome do operador. Até 80 caracteres.", example = "Fernanda Lima")
        String nome,

        @NotBlank(message = "{operador.email.obrigatorio}")
        @Email(message = "{operador.email.invalido}")
        @Size(max = 150, message = "{operador.email.tamanho-maximo}")
        @Schema(description = "E-mail do operador. É guardado em minúsculas e não pode repetir. Até 150 caracteres.", example = "fernanda.lima@exemplo.com")
        String email,

        @NotNull(message = "{operador.ponto-id.obrigatorio}")
        @Schema(description = "Id do ponto de coleta ao qual o operador pertence. O ponto precisa existir.", example = "1")
        Integer pontoId,

        @Schema(description = SITUACAO_CADASTRO_CORPO)
        SituacaoCadastro situacao
) {

    /** Apara nome e e-mail antes da validação (mesmo critério do morador, ADR 0021). */
    public OperadorForm {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim();
    }
}
