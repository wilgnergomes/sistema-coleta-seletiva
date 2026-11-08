package br.edu.ifs.coletaseletiva.exception;

/** Combinação inválida de parâmetros da URL (400): aponta o campo e usa uma chave de mensagem. */
public class ParametroInvalidoException extends NegocioException {

    private final String campo;

    public ParametroInvalidoException(String campo, String chave, Object... argumentos) {
        super(chave, argumentos);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
