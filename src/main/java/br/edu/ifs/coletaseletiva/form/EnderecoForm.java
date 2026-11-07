package br.edu.ifs.coletaseletiva.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo de criação e de atualização de endereço (schema EnderecoEntrada do contrato). */
public record EnderecoForm(
        @NotBlank(message = "{endereco.logradouro.obrigatorio}")
        @Size(max = 150, message = "{endereco.logradouro.tamanho-maximo}")
        String logradouro,

        @NotBlank(message = "{endereco.numero.obrigatorio}")
        @Size(max = 10, message = "{endereco.numero.tamanho-maximo}")
        String numero,

        @NotBlank(message = "{endereco.bairro.obrigatorio}")
        @Size(max = 100, message = "{endereco.bairro.tamanho-maximo}")
        String bairro,

        @NotBlank(message = "{endereco.municipio.obrigatorio}")
        @Size(max = 100, message = "{endereco.municipio.tamanho-maximo}")
        String municipio,

        @NotBlank(message = "{endereco.uf.obrigatorio}")
        @Pattern(regexp = "^[A-Z]{2}$", message = "{endereco.uf.formato}")
        String uf,

        @NotBlank(message = "{endereco.cep.obrigatorio}")
        @Pattern(regexp = "^[0-9]{8}$", message = "{endereco.cep.formato}")
        String cep,

        @Size(max = 100, message = "{endereco.complemento.tamanho-maximo}")
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
