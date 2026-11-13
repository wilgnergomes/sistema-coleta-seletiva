package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SITUACAO_CADASTRO_CORPO;

/** Corpo de criação e atualização de material (schema MaterialEntrada). {@code situacao} é opcional. */
public record MaterialForm(
        @NotBlank(message = "{material.nome.obrigatorio}")
        @Size(max = 80, message = "{material.nome.tamanho-maximo}")
        @Schema(description = "Nome do material. É único, sem diferenciar maiúsculas de minúsculas. Até 80 caracteres.", example = "Óleo de cozinha")
        String nome,

        @NotBlank(message = "{material.descricao.obrigatoria}")
        @Size(max = 500, message = "{material.descricao.tamanho-maximo}")
        @Schema(description = "Descrição do material. Até 500 caracteres.", example = "Óleo de fritura usado, já frio")
        String descricao,

        @NotNull(message = "{material.unidade.obrigatoria}")
        @Schema(description = "Unidade de medida: `KG` (peso), `L` (volume) ou `UN` (peça, só quantidades inteiras).", example = "L")
        Unidade unidade,

        @NotBlank(message = "{material.orientacao.obrigatoria}")
        @Size(max = 1000, message = "{material.orientacao.tamanho-maximo}")
        @Schema(description = "Orientação de preparo e entrega mostrada ao morador. Até 1000 caracteres.", example = "Guardar em garrafa PET bem fechada, sem misturar com água")
        String orientacao,

        @Schema(description = SITUACAO_CADASTRO_CORPO)
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
