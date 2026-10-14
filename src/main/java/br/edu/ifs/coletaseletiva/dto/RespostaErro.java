package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.exception.CodigoErro;

import java.util.List;

public record RespostaErro (
    CodigoErro codigo,
    String mensagem,
    List<ErroCampo> erros
) {

}
