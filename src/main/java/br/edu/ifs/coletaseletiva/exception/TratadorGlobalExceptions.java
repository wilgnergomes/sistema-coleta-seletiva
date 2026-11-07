package br.edu.ifs.coletaseletiva.exception;

import br.edu.ifs.coletaseletiva.dto.ErroCampo;
import br.edu.ifs.coletaseletiva.dto.RespostaErro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TratadorGlobalExceptions {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExceptions.class);

    // =====================================================================
    // 400: entrada inválida (exceções lançadas pelo Spring)
    // =====================================================================

    /** Campo do corpo quebrou uma validação (@NotBlank, @Email, @Size...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> entradaInvalida(MethodArgumentNotValidException excecao) {
        List<ErroCampo> erros = excecao.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO, "Dados inválidos.", erros);
    }

    /** JSON malformado, corpo ausente ou valor de enum inexistente no corpo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> jsonIlegivel(HttpMessageNotReadableException excecao) {
        if (excecao.getCause() instanceof InvalidFormatException causa
                && causa.getTargetType() != null
                && causa.getTargetType().isEnum()) {
            return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALOR_INVALIDO,
                    mensagemValoresAceitos(causa.getTargetType()), List.of());
        }
        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO,
                "Dados inválidos. Verifique se o JSON está completo e bem formado.", List.of());
    }

    /** Parâmetro da URL com tipo errado: /moradores/abc ou ?situacao=XYZ. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespostaErro> parametroInvalido(MethodArgumentTypeMismatchException excecao) {
        Class<?> tipo = excecao.getRequiredType();

        if (tipo != null && tipo.isEnum()) {
            List<ErroCampo> erros = List.of(new ErroCampo(excecao.getName(), mensagemValoresAceitos(tipo)));
            return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALOR_INVALIDO,
                    "Parâmetro inválido.", erros);
        }

        List<ErroCampo> erros = List.of(new ErroCampo(excecao.getName(), "valor inválido"));
        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO, "Parâmetro inválido.", erros);
    }

    /** Validação de @RequestParam/@PathVariable falhou (ex.: size acima de 100). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RespostaErro> validacaoDeParametro(HandlerMethodValidationException excecao) {
        List<ErroCampo> erros = excecao.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(erro -> new ErroCampo(
                                resultado.getMethodParameter().getParameterName(),
                                erro.getDefaultMessage())))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO, "Parâmetro inválido.", erros);
    }

    /** Parâmetro obrigatório da URL não foi enviado. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespostaErro> parametroAusente(MissingServletRequestParameterException excecao) {
        List<ErroCampo> erros = List.of(new ErroCampo(excecao.getParameterName(), "o parâmetro é obrigatório"));
        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO, "Parâmetro ausente.", erros);
    }

    // =====================================================================
    // 403, 404, 409: exceções que você lança no service
    // =====================================================================

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<RespostaErro> acessoNegado(AcessoNegadoException excecao) {
        return montar(HttpStatus.FORBIDDEN, CodigoErro.ACESSO_NEGADO, excecao.getMessage(), List.of());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<RespostaErro> naoEncontrado(NaoEncontradoException excecao) {
        return montar(HttpStatus.NOT_FOUND, CodigoErro.NAO_ENCONTRADO, excecao.getMessage(), List.of());
    }

    /** URL que não existe (lançada pelo Spring). */
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

    /** Duas transações disputaram a mesma linha com @Version (ADR 0009). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<RespostaErro> bloqueioOtimista(ObjectOptimisticLockingFailureException excecao) {
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_VERSAO,
                "O registro foi alterado por outra operação. Recarregue e tente novamente.", List.of());
    }

    /** O banco barrou o dado (UNIQUE, FK, CHECK) e o service não tinha checado antes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaErro> violacaoDeIntegridade(DataIntegrityViolationException excecao) {
        log.warn("Violação de integridade no banco", excecao);
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_REGRA,
                "Os dados informados violam uma regra de integridade.", List.of());
    }

    // =====================================================================
    // 405 e 415: fora do catálogo do ADR 0018 (ver observação abaixo)
    // =====================================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespostaErro> metodoNaoSuportado(HttpRequestMethodNotSupportedException excecao) {
        return montar(excecao.getStatusCode(), CodigoErro.VALIDACAO,
                "Método HTTP não suportado para este recurso.", List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespostaErro> tipoNaoSuportado(HttpMediaTypeNotSupportedException excecao) {
        return montar(excecao.getStatusCode(), CodigoErro.VALIDACAO,
                "Tipo de conteúdo não suportado. Use application/json.", List.of());
    }

    // =====================================================================
    // 500: rede de segurança
    // =====================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> erroInesperado(Exception excecao) {
        log.error("Erro inesperado", excecao);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, CodigoErro.ERRO_INTERNO,
                "Erro interno. Tente novamente mais tarde.", List.of());
    }

    // =====================================================================
    // apoio
    // =====================================================================

    private ResponseEntity<RespostaErro> montar(HttpStatusCode status, CodigoErro codigo,
                                                String mensagem, List<ErroCampo> erros) {
        return ResponseEntity
                .status(status)
                .body(new RespostaErro(codigo, mensagem, erros));
    }

    private String mensagemValoresAceitos(Class<?> tipoEnum) {
        String valores = Arrays.stream(tipoEnum.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return "Valor inválido. Valores aceitos: " + valores + ".";
    }
}