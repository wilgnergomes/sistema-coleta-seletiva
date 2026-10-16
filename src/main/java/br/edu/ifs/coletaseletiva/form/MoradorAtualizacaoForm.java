package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoradorAtualizacaoForm(
        @NotBlank(message = "o nome é obrigatório")
        @Size(max = 80, message = "deve ter no máximo 80 caracteres")
        String nome,

        @NotBlank(message = "o e-mail é obrigatório")
        @Email(message = "deve ser um endereço de e-mail válido")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 20, message = "deve ter no máximo 20 caracteres")
        String fone,

        SituacaoCadastro situacao
) {}