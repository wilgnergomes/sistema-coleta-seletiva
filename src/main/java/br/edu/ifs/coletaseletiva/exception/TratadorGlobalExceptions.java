package br.edu.ifs.coletaseletiva.exception;

import br.edu.ifs.coletaseletiva.dto.ErroCampo;
import br.edu.ifs.coletaseletiva.dto.RespostaErro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.List;

@RestControllerAdvice
public class TratadorGlobalExceptions {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExceptions.class);

    // ---------- 400: erros de entrada (exceções lançadas pelo Spring) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> entradaInvalida(MethodArgumentNotValidException excecao) {
        List<ErroCampo> erros = excecao.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO, "Dados inválidos.", erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> jsonIlegivel(HttpMessageNotReadableException excecao) {
        boolean enumInvalido = excecao.getCause() instanceof InvalidFormatException causa
                && causa.getTargetType() != null
                && causa.getTargetType().isEnum();

        CodigoErro codigo = enumInvalido ? CodigoErro.VALOR_INVALIDO : CodigoErro.VALIDACAO;
        return montar(HttpStatus.BAD_REQUEST, codigo, "Dados inválidos.", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespostaErro> parametroInvalido(MethodArgumentTypeMismatchException excecao) {
        boolean enumInvalido = excecao.getRequiredType() != null
                && excecao.getRequiredType().isEnum();

        CodigoErro codigo = enumInvalido ? CodigoErro.VALOR_INVALIDO : CodigoErro.VALIDACAO;
        List<ErroCampo> erros = List.of(new ErroCampo(excecao.getName(), "valor inválido"));
        return montar(HttpStatus.BAD_REQUEST, codigo, "Parâmetro inválido.", erros);
    }

    // ---------- 403, 404, 409: exceções que você lança no service ----------

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<RespostaErro> acessoNegado(AcessoNegadoException excecao) {
        return montar(HttpStatus.FORBIDDEN, CodigoErro.ACESSO_NEGADO, excecao.getMessage(), List.of());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<RespostaErro> naoEncontrado(NaoEncontradoException excecao) {
        return montar(HttpStatus.NOT_FOUND, CodigoErro.NAO_ENCONTRADO, excecao.getMessage(), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespostaErro> caminhoInexistente(NoResourceFoundException excecao) {
        return montar(HttpStatus.NOT_FOUND, CodigoErro.NAO_ENCONTRADO, "Recurso não encontrado.", List.of());
    }

    @ExceptionHandler(ConflitoRegraException.class)
    public ResponseEntity<RespostaErro> conflitoRegra(ConflitoRegraException excecao) {
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_REGRA, excecao.getMessage(), List.of());
    }

    @ExceptionHandler(ConflitoVersaoException.class)
    public ResponseEntity<RespostaErro> conflitoVersao(ConflitoVersaoException excecao) {
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_VERSAO, excecao.getMessage(), List.of());
    }

    // ---------- 500: qualquer erro inesperado ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> erroInesperado(Exception excecao) {
        log.error("Erro inesperado", excecao);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, CodigoErro.ERRO_INTERNO,
                "Erro interno. Tente novamente mais tarde.", List.of());
    }

    // ---------- método auxiliar: evita repetir as mesmas linhas ----------

    private ResponseEntity<RespostaErro> montar(HttpStatus status, CodigoErro codigo,
                                                String mensagem, List<ErroCampo> erros) {
        return ResponseEntity
                .status(status)
                .body(new RespostaErro(codigo, mensagem, erros));
    }
}