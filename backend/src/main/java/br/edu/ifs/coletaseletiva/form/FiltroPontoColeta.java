package br.edu.ifs.coletaseletiva.form;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;

/**
 * Filtros da pesquisa de pontos de coleta (RF06). Todos são opcionais; os informados se combinam (E).
 * Texto em branco vale "sem filtro".
 */
public record FiltroPontoColeta(
        String nome,
        String municipio,
        String bairro,
        Integer materialId,
        SituacaoCadastro situacao
) {

    public FiltroPontoColeta {
        nome = vazioParaNulo(nome);
        municipio = vazioParaNulo(municipio);
        bairro = vazioParaNulo(bairro);
    }

    private static String vazioParaNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
