package br.edu.ifs.coletaseletiva.exception;

import br.edu.ifs.coletaseletiva.dto.ErroCampo;
import br.edu.ifs.coletaseletiva.dto.RespostaErro;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

/**
 * Converte exceções em respostas {@code { codigo, mensagem, erros }} (ADR 0018). As mensagens vêm
 * do {@link MessageSource}, no idioma da requisição (ADR 0023): aqui o texto é escolhido pela chave.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class TratadorGlobalExceptions {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExceptions.class);

    private final MessageSource messageSource;

    // =====================================================================
    // 400: entrada inválida (exceções lançadas pelo Spring)
    // =====================================================================

    /** Campo do corpo quebrou uma validação (@NotBlank, @Email, @Size...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> entradaInvalida(MethodArgumentNotValidException excecao) {
        // O texto de cada erro já vem no idioma certo: o Bean Validation resolve a chave
        // da anotação (por exemplo {morador.nome.obrigatorio}) no mesmo MessageSource.
        List<ErroCampo> erros = excecao.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO, traduzir("erro.geral.dados-invalidos"), erros);
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
                traduzir("erro.geral.json-ilegivel"), List.of());
    }

    /** Parâmetro da URL com tipo errado: /moradores/abc ou ?situacao=XYZ. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespostaErro> parametroInvalido(MethodArgumentTypeMismatchException excecao) {
        Class<?> tipo = excecao.getRequiredType();

        if (tipo != null && tipo.isEnum()) {
            List<ErroCampo> erros = List.of(new ErroCampo(excecao.getName(), mensagemValoresAceitos(tipo)));
            return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALOR_INVALIDO,
                    traduzir("erro.geral.parametro-invalido"), erros);
        }

        List<ErroCampo> erros = List.of(new ErroCampo(excecao.getName(), traduzir("erro.geral.valor-invalido-campo")));
        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO,
                traduzir("erro.geral.parametro-invalido"), erros);
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

        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO,
                traduzir("erro.geral.parametro-invalido"), erros);
    }

    /** Combinação de parâmetros inválida, detectada no service (ex.: período com fim antes do início). */
    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<RespostaErro> parametrosInvalidos(ParametroInvalidoException excecao) {
        List<ErroCampo> erros = List.of(new ErroCampo(excecao.getCampo(), traduzir(excecao)));
        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO,
                traduzir("erro.geral.parametro-invalido"), erros);
    }

    /** Parâmetro obrigatório da URL não foi enviado. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespostaErro> parametroAusente(MissingServletRequestParameterException excecao) {
        List<ErroCampo> erros = List.of(
                new ErroCampo(excecao.getParameterName(), traduzir("erro.geral.parametro-obrigatorio")));
        return montar(HttpStatus.BAD_REQUEST, CodigoErro.VALIDACAO,
                traduzir("erro.geral.parametro-ausente"), erros);
    }

    // =====================================================================
    // 403, 404, 409: exceções que você lança no service
    // =====================================================================

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<RespostaErro> acessoNegado(AcessoNegadoException excecao) {
        return montar(HttpStatus.FORBIDDEN, CodigoErro.ACESSO_NEGADO, traduzir(excecao), List.of());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<RespostaErro> naoEncontrado(NaoEncontradoException excecao) {
        return montar(HttpStatus.NOT_FOUND, CodigoErro.NAO_ENCONTRADO, traduzir(excecao), List.of());
    }

    /** URL que não existe (lançada pelo Spring). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespostaErro> caminhoInexistente(NoResourceFoundException excecao) {
        return montar(HttpStatus.NOT_FOUND, CodigoErro.NAO_ENCONTRADO,
                traduzir("erro.geral.recurso-nao-encontrado"), List.of());
    }

    @ExceptionHandler(ConflitoRegraException.class)
    public ResponseEntity<RespostaErro> conflitoRegra(ConflitoRegraException excecao) {
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_REGRA, traduzir(excecao), List.of());
    }

    @ExceptionHandler(ConflitoVersaoException.class)
    public ResponseEntity<RespostaErro> conflitoVersao(ConflitoVersaoException excecao) {
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_VERSAO, traduzir(excecao), List.of());
    }

    /** Duas transações disputaram a mesma linha com @Version (ADR 0009). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<RespostaErro> bloqueioOtimista(ObjectOptimisticLockingFailureException excecao) {
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_VERSAO,
                traduzir("erro.geral.registro-alterado"), List.of());
    }

    /** O banco barrou o dado (UNIQUE, FK, CHECK) e o service não tinha checado antes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaErro> violacaoDeIntegridade(DataIntegrityViolationException excecao) {
        log.warn("Violação de integridade no banco", excecao);
        return montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_REGRA,
                traduzir("erro.geral.violacao-integridade"), List.of());
    }

    // =====================================================================
    // 405 e 415: fora do catálogo do ADR 0018 (ver observação abaixo)
    // =====================================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespostaErro> metodoNaoSuportado(HttpRequestMethodNotSupportedException excecao) {
        return montar(excecao.getStatusCode(), CodigoErro.VALIDACAO,
                traduzir("erro.geral.metodo-nao-suportado"), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespostaErro> tipoNaoSuportado(HttpMediaTypeNotSupportedException excecao) {
        return montar(excecao.getStatusCode(), CodigoErro.VALIDACAO,
                traduzir("erro.geral.tipo-nao-suportado"), List.of());
    }

    // =====================================================================
    // 500: rede de segurança
    // =====================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> erroInesperado(Exception excecao) {
        log.error("Erro inesperado", excecao);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, CodigoErro.ERRO_INTERNO,
                traduzir("erro.geral.erro-interno"), List.of());
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

    /**
     * Procura a chave no MessageSource, no idioma que o DispatcherServlet escolheu para esta
     * requisição (Accept-Language). Se a chave não existir, devolve a própria chave.
     */
    private String traduzir(String chave, Object... argumentos) {
        return messageSource.getMessage(chave, argumentos, chave, LocaleContextHolder.getLocale());
    }

    private String traduzir(NegocioException excecao) {
        return traduzir(excecao.getChave(), excecao.getArgumentos());
    }

    private String mensagemValoresAceitos(Class<?> tipoEnum) {
        String valores = Arrays.stream(tipoEnum.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return traduzir("erro.geral.valor-invalido", valores);
    }
}
