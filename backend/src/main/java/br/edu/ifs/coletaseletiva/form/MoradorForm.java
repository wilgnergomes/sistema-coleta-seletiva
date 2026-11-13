package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Nome completo do morador. Até 80 caracteres.", example = "Lucas Ferreira")
        String nome,

        @NotBlank(message = "{morador.email.obrigatorio}")
        @Email(message = "{morador.email.invalido}")
        @Size(max = 150, message = "{morador.email.tamanho-maximo}")
        @Schema(description = "E-mail do morador. É guardado em minúsculas, sem espaços nas pontas, e não pode repetir. Até 150 caracteres.", example = "lucas.ferreira@exemplo.com")
        String email,

        @Size(max = 20, message = "{morador.fone.tamanho-maximo}")
        @Schema(description = "Telefone com DDD. Opcional. Até 20 caracteres.", example = "79999990000")
        String fone,

        @Schema(description = "Opcional. **Na atualização**, só o administrador pode alterá-la; se omitida, a situação atual é mantida. **No cadastro é ignorada**: o morador sempre nasce `ATIVO`.")
        SituacaoCadastro situacao
) {

    /** Apara o e-mail na leitura, para que a validação veja o mesmo texto que o service normaliza. */
    public MoradorForm {
        email = email == null ? null : email.trim();
    }
}