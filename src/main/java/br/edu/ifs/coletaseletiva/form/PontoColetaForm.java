package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SITUACAO_CADASTRO_CORPO;

/**
 * Corpo de criação e atualização do ponto de coleta (schema PontoEntrada). O endereço reaproveita o
 * {@link EnderecoForm} do RF02; {@code @Valid} faz o Bean Validation entrar também nos campos dele
 * (os erros saem como "endereco.cep", "endereco.uf"...).
 */
public record PontoColetaForm(
        @NotBlank(message = "{ponto.nome.obrigatorio}")
        @Size(max = 120, message = "{ponto.nome.tamanho-maximo}")
        @Schema(description = "Nome do ponto de coleta. Até 120 caracteres.", example = "Ecoponto Bairro Novo")
        String nome,

        @Size(max = 150, message = "{ponto.contato.tamanho-maximo}")
        @Schema(description = "Telefone ou e-mail de contato. Opcional. Até 150 caracteres.", example = "(79) 3333-0000")
        String contato,

        @Size(max = 500, message = "{ponto.horarios.tamanho-maximo}")
        @Schema(description = "Horários de funcionamento, em texto livre. Opcional. Até 500 caracteres.", example = "Segunda a sexta, das 8h às 17h")
        String horarios,

        @Schema(description = "Se o ponto recolhe na casa do morador. Opcional; se omitido, vale `false`. Só pontos com `true` recebem solicitações.", example = "true")
        Boolean recolhimentoDomiciliar,

        @Schema(description = SITUACAO_CADASTRO_CORPO)
        SituacaoCadastro situacao,

        @NotNull(message = "{ponto.endereco.obrigatorio}")
        @Valid
        @Schema(description = "Endereço do ponto. Todos os campos são obrigatórios, exceto `complemento`.")
        EnderecoForm endereco
) {

    public PontoColetaForm {
        nome = aparar(nome);
        contato = aparar(contato);
        horarios = aparar(horarios);
    }

    private static String aparar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
