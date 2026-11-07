package br.edu.ifs.coletaseletiva.exception;

public class AcessoNegadoException extends NegocioException {
    public AcessoNegadoException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
