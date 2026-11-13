package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.exception.CodigoErro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RespostaErro (
    @Schema(description = "Código do erro: `VALIDACAO`, `VALOR_INVALIDO`, `ACESSO_NEGADO`, `NAO_ENCONTRADO`, `CONFLITO_REGRA`, `CONFLITO_VERSAO` ou `ERRO_INTERNO`.")
    CodigoErro codigo,
    @Schema(description = "Texto explicativo, no idioma pedido em `Accept-Language`.")
    String mensagem,
    @Schema(description = "Campos rejeitados, quando o erro é de validação. Lista vazia nos demais casos.")
    List<ErroCampo> erros
) {

}
