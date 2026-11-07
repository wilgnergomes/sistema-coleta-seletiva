package br.edu.ifs.coletaseletiva.exception;

/**
 * Base das exceções de negócio. Em vez de um texto pronto, carrega a chave de uma mensagem
 * (em messages.properties) e os argumentos que preenchem os {0}, {1}... do texto.
 * Quem escolhe o idioma e monta a frase final é o TratadorGlobalExceptions (ADR 0023).
 */
public abstract class NegocioException extends RuntimeException {

    private final String chave;
    private final Object[] argumentos;

    protected NegocioException(String chave, Object... argumentos) {
        super(chave);
        this.chave = chave;
        this.argumentos = argumentos;
    }

    public String getChave() {
        return chave;
    }

    public Object[] getArgumentos() {
        return argumentos;
    }
}
