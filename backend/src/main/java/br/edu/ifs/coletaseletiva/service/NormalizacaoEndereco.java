package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.form.EnderecoForm;

/**
 * Normalização do endereço compartilhada pelo morador (RF02) e pelo ponto de coleta (RF04):
 * o ADR 0005 exige a mesma regra nas duas tabelas. Os espaços já foram removidos pelo form.
 */
final class NormalizacaoEndereco {

    private static final String SEM_NUMERO = "S/N";

    private NormalizacaoEndereco() {
    }

    /** "s/n" vira "S/N" e complemento vazio vira nulo. */
    static EnderecoForm normalizar(EnderecoForm f) {
        String numero = SEM_NUMERO.equalsIgnoreCase(f.numero()) ? SEM_NUMERO : f.numero();
        String complemento = (f.complemento() == null || f.complemento().isBlank()) ? null : f.complemento();
        return new EnderecoForm(f.logradouro(), numero, f.bairro(), f.municipio(), f.uf(), f.cep(), complemento);
    }
}
