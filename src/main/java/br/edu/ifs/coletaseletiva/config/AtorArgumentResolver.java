package br.edu.ifs.coletaseletiva.config;

import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Locale;
import java.util.Optional;

/**
 * Monta o {@link Ator} a partir dos cabeçalhos (ADR 0019). Um parâmetro {@code Ator} exige os
 * cabeçalhos; um parâmetro {@code Optional<Ator>} (consultas públicas) aceita a ausência deles.
 */
public class AtorArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String CABECALHO_PERFIL = "X-Perfil";
    private static final String CABECALHO_USUARIO = "X-Usuario-Id";

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return Ator.class.equals(parametro.nestedIfOptional().getNestedParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parametro,
                                  ModelAndViewContainer container,
                                  NativeWebRequest requisicao,
                                  WebDataBinderFactory fabrica) {

        if (Optional.class.equals(parametro.getParameterType())) {
            String textoPerfil = requisicao.getHeader(CABECALHO_PERFIL);
            if (textoPerfil == null || textoPerfil.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(lerAtor(requisicao));
        }
        return lerAtor(requisicao);
    }

    private Ator lerAtor(NativeWebRequest requisicao) {
        String textoPerfil = requisicao.getHeader(CABECALHO_PERFIL);
        if (textoPerfil == null || textoPerfil.isBlank()) {
            throw new AcessoNegadoException("erro.ator.cabecalho-ausente", CABECALHO_PERFIL);
        }

        Perfil perfil;
        try {
            perfil = Perfil.valueOf(textoPerfil.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AcessoNegadoException("erro.ator.perfil-invalido");
        }

        if (perfil == Perfil.ADMINISTRADOR) {
            return new Ator(perfil, null);
        }

        String textoId = requisicao.getHeader(CABECALHO_USUARIO);
        if (textoId == null || textoId.isBlank()) {
            throw new AcessoNegadoException("erro.ator.cabecalho-ausente", CABECALHO_USUARIO);
        }

        try {
            return new Ator(perfil, Integer.valueOf(textoId.trim()));
        } catch (NumberFormatException e) {
            throw new AcessoNegadoException("erro.ator.usuario-id-invalido", CABECALHO_USUARIO);
        }
    }
}
