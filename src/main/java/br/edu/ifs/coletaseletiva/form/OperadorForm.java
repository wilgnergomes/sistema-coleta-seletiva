package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de criação e atualização de operador (schema OperadorEntrada). {@code situacao} é opcional. */
public record OperadorForm(
        @NotBlank(message = "{operador.nome.obrigatorio}")
        @Size(max = 80, message = "{operador.nome.tamanho-maximo}")
        String nome,

        @NotBlank(message = "{operador.email.obrigatorio}")
        @Email(message = "{operador.email.invalido}")
        @Size(max = 150, message = "{operador.email.tamanho-maximo}")
        String email,

        @NotNull(message = "{operador.ponto-id.obrigatorio}")
        Integer pontoId,

        SituacaoCadastro situacao
) {

    /** Apara nome e e-mail antes da validação (mesmo critério do morador, ADR 0021). */
    public OperadorForm {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim();
    }
}
