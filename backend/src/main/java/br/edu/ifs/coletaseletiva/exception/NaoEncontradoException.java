package br.edu.ifs.coletaseletiva.exception;

public class NaoEncontradoException extends NegocioException {
    public NaoEncontradoException(String chave, Object... argumentos) {
        super(chave, argumentos);
    }
}
