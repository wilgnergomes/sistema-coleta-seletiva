package br.edu.ifs.coletaseletiva.exception;

public class ConflitoVersaoException extends NegocioException {
    public ConflitoVersaoException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
