package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoradorCriacaoForm(
        @NotBlank(message = "o nome é obrigatório")
        @Size(max = 80, message = "deve ter no máximo 80 caracteres")
        String nome,

        @NotBlank(message = "o e-mail é obrigatório")
        @Email(message = "deve ser um endereço de e-mail válido")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 20, message = "deve ter no máximo 20 caracteres")
        String fone
) {

    /** Apara o e-mail na leitura, para que a validação veja o mesmo texto que o service normaliza. */
    public MoradorCriacaoForm {
        email = email == null ? null : email.trim();
    }
}