package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação e de atualização de morador (schema MoradorEntrada). {@code situacao} é opcional: na
 * atualização, só o administrador pode alterá-la; na criação ela é sempre ignorada e o morador nasce ATIVO
 * (ADR 0026, regra aplicada pelo {@code MoradorMapper.paraEntidade}, que não copia esse campo).
 */
public record MoradorForm(
        @NotBlank(message = "{morador.nome.obrigatorio}")
        @Size(max = 80, message = "{morador.nome.tamanho-maximo}")
        String nome,

        @NotBlank(message = "{morador.email.obrigatorio}")
        @Email(message = "{morador.email.invalido}")
        @Size(max = 150, message = "{morador.email.tamanho-maximo}")
        String email,

        @Size(max = 20, message = "{morador.fone.tamanho-maximo}")
        String fone,

        SituacaoCadastro situacao
) {

    /** Apara o e-mail na leitura, para que a validação veja o mesmo texto que o service normaliza. */
    public MoradorForm {
        email = email == null ? null : email.trim();
    }
}