package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de criação e atualização de material (schema MaterialEntrada). {@code situacao} é opcional. */
public record MaterialForm(
        @NotBlank(message = "{material.nome.obrigatorio}")
        @Size(max = 80, message = "{material.nome.tamanho-maximo}")
        String nome,

        @NotBlank(message = "{material.descricao.obrigatoria}")
        @Size(max = 500, message = "{material.descricao.tamanho-maximo}")
        String descricao,

        @NotNull(message = "{material.unidade.obrigatoria}")
        Unidade unidade,

        @NotBlank(message = "{material.orientacao.obrigatoria}")
        @Size(max = 1000, message = "{material.orientacao.tamanho-maximo}")
        String orientacao,

        SituacaoCadastro situacao
) {

    /** Remove espaços das pontas antes da validação (mesmo critério do e-mail e do endereço). */
    public MaterialForm {
        nome = aparar(nome);
        descricao = aparar(descricao);
        orientacao = aparar(orientacao);
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
