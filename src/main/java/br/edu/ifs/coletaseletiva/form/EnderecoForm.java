package br.edu.ifs.coletaseletiva.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo de criação e de atualização de endereço (schema EnderecoEntrada do contrato). */
public record EnderecoForm(
        @NotBlank(message = "{endereco.logradouro.obrigatorio}")
        @Size(max = 150, message = "{endereco.logradouro.tamanho-maximo}")
        @Schema(description = "Rua, avenida ou similar. Até 150 caracteres.", example = "Rua das Flores")
        String logradouro,

        @NotBlank(message = "{endereco.numero.obrigatorio}")
        @Size(max = 10, message = "{endereco.numero.tamanho-maximo}")
        @Schema(description = "Número do imóvel, ou `S/N` quando não houver. Até 10 caracteres.", example = "120")
        String numero,

        @NotBlank(message = "{endereco.bairro.obrigatorio}")
        @Size(max = 100, message = "{endereco.bairro.tamanho-maximo}")
        @Schema(description = "Bairro. Até 100 caracteres.", example = "Centro")
        String bairro,

        @NotBlank(message = "{endereco.municipio.obrigatorio}")
        @Size(max = 100, message = "{endereco.municipio.tamanho-maximo}")
        @Schema(description = "Município. Até 100 caracteres.", example = "Lagarto")
        String municipio,

        @NotBlank(message = "{endereco.uf.obrigatorio}")
        @Pattern(regexp = "^[A-Z]{2}$", message = "{endereco.uf.formato}")
        @Schema(description = "Sigla do estado, com duas letras maiúsculas.", example = "SE")
        String uf,

        @NotBlank(message = "{endereco.cep.obrigatorio}")
        @Pattern(regexp = "^[0-9]{8}$", message = "{endereco.cep.formato}")
        @Schema(description = "CEP com 8 dígitos, sem hífen.", example = "49400000")
        String cep,

        @Size(max = 100, message = "{endereco.complemento.tamanho-maximo}")
        @Schema(description = "Complemento do endereço. Opcional. Até 100 caracteres.", example = "Casa dos fundos")
        String complemento
) {

    /** Remove espaços das pontas antes da validação (mesmo critério do e-mail, ADR 0021). */
    public EnderecoForm {
        logradouro = aparar(logradouro);
        numero = aparar(numero);
        bairro = aparar(bairro);
        municipio = aparar(municipio);
        uf = aparar(uf);
        cep = aparar(cep);
        complemento = aparar(complemento);
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
