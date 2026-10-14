package br.edu.ifs.coletaseletiva.exception;

public class NaoEncontradoException extends RuntimeException{
    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
