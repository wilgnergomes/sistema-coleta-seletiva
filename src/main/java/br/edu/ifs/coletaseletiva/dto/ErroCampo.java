package br.edu.ifs.coletaseletiva.dto;

public record ErroCampo(
    String campo,
    String mensagem
) {

}
