package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.config.FusoNegocio;
import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.form.FiltroSolicitacao;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Predicados reutilizáveis da consulta de solicitações (RF09). Os filtros do usuário ({@link #de}) e a
 * restrição do contexto do ator ({@link #doMorador}, {@link #doPonto}) são combinados com E: o que o ator
 * não pode ver nunca chega a ser lido do banco.
 */
public final class SolicitacaoSpecifications {

    private SolicitacaoSpecifications() {
    }

    public static Specification<Solicitacao> de(FiltroSolicitacao filtro, FusoNegocio fuso) {
        Specification<Solicitacao> especificacao = qualquer();
        if (filtro.situacao() != null) {
            especificacao = especificacao.and(situacaoIgual(filtro.situacao()));
        }
        if (filtro.pontoId() != null) {
            especificacao = especificacao.and(doPonto(filtro.pontoId()));
        }
        if (filtro.de() != null) {
            especificacao = especificacao.and(criadaDesde(fuso.inicioDoDia(filtro.de())));
        }
        if (filtro.ate() != null) {
            // Intervalo semiaberto: o dia final inteiro entra, porque o limite é o começo do dia seguinte.
            especificacao = especificacao.and(criadaAntesDe(fuso.inicioDoDia(filtro.ate().plusDays(1))));
        }
        return especificacao;
    }

    /** Sem restrição (administrador). */
    public static Specification<Solicitacao> qualquer() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Solicitacao> situacaoIgual(SituacaoSolicitacao situacao) {
        return (root, query, cb) -> cb.equal(root.get("situacao"), situacao);
    }

    public static Specification<Solicitacao> doPonto(Integer pontoId) {
        return (root, query, cb) -> cb.equal(root.get("ponto").get("id"), pontoId);
    }

    public static Specification<Solicitacao> doMorador(Integer moradorId) {
        return (root, query, cb) -> cb.equal(root.get("morador").get("id"), moradorId);
    }

    /** Inclusivo: uma solicitação criada exatamente no início do período entra. */
    static Specification<Solicitacao> criadaDesde(OffsetDateTime inicio) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("criadaEm"), inicio);
    }

    /** Exclusivo: uma solicitação criada exatamente no limite pertence ao período seguinte. */
    static Specification<Solicitacao> criadaAntesDe(OffsetDateTime limite) {
        return (root, query, cb) -> cb.lessThan(root.<OffsetDateTime>get("criadaEm"), limite);
    }
}
