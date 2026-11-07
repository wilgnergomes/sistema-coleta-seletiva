package br.edu.ifs.coletaseletiva.exception;

public class ConflitoRegraException extends NegocioException {
    public ConflitoRegraException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
