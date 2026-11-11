package br.edu.ifs.coletaseletiva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErroCampo(
    @Schema(description = "Nome do campo rejeitado, por exemplo `email` ou `endereco.cep`.", example = "email")
    String campo,
    @Schema(description = "Motivo da rejeição.")
    String mensagem
) {

}
