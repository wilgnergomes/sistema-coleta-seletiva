package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação e atualização do ponto de coleta (schema PontoEntrada). O endereço reaproveita o
 * {@link EnderecoForm} do RF02; {@code @Valid} faz o Bean Validation entrar também nos campos dele
 * (os erros saem como "endereco.cep", "endereco.uf"...).
 */
public record PontoColetaForm(
        @NotBlank(message = "{ponto.nome.obrigatorio}")
        @Size(max = 120, message = "{ponto.nome.tamanho-maximo}")
        String nome,

        @Size(max = 150, message = "{ponto.contato.tamanho-maximo}")
        String contato,

        @Size(max = 500, message = "{ponto.horarios.tamanho-maximo}")
        String horarios,

        Boolean recolhimentoDomiciliar,

        SituacaoCadastro situacao,

        @NotNull(message = "{ponto.endereco.obrigatorio}")
        @Valid
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
