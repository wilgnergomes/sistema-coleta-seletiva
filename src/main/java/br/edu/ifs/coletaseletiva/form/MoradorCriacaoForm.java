package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoradorCriacaoForm(
        @NotBlank(message = "{morador.nome.obrigatorio}")
        @Size(max = 80, message = "{morador.nome.tamanho-maximo}")
        String nome,

        @NotBlank(message = "{morador.email.obrigatorio}")
        @Email(message = "{morador.email.invalido}")
        @Size(max = 150, message = "{morador.email.tamanho-maximo}")
        String email,

        @Size(max = 20, message = "{morador.fone.tamanho-maximo}")
        String fone
) {

    /** Apara o e-mail na leitura, para que a validação veja o mesmo texto que o service normaliza. */
    public MoradorCriacaoForm {
        email = email == null ? null : email.trim();
    }
}