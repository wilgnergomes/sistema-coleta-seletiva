package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.dto.QuantidadeRecolhidaDTO;
import br.edu.ifs.coletaseletiva.entity.ItemSolicitacao;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas do resumo gerencial (RF15). O banco faz o {@code COUNT}, o {@code SUM} e o {@code GROUP BY}:
 * devolvem-se poucas linhas já agrupadas, em vez de carregar todas as solicitações em memória. O período é um
 * intervalo semiaberto {@code [inicio, fim)} sobre a data de cada indicador (ADR 0014); um limite nulo não filtra.
 */
@Repository
@RequiredArgsConstructor
public class ResumoGerencialRepository {

    private final EntityManager entityManager;

    /** Número de solicitações de cada situação, pela data de criação. Situações sem ocorrência não aparecem. */
    public Map<SituacaoSolicitacao, Long> contarPorSituacao(OffsetDateTime inicio, OffsetDateTime fim) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
        Root<Solicitacao> solicitacao = consulta.from(Solicitacao.class);

        consulta.multiselect(solicitacao.get("situacao"), cb.count(solicitacao));
        consulta.where(periodo(cb, solicitacao.get("criadaEm"), inicio, fim).toArray(new Predicate[0]));
        consulta.groupBy(solicitacao.get("situacao"));

        Map<SituacaoSolicitacao, Long> totais = new EnumMap<>(SituacaoSolicitacao.class);
        for (Tuple linha : entityManager.createQuery(consulta).getResultList()) {
            totais.put(linha.get(0, SituacaoSolicitacao.class), linha.get(1, Long.class));
        }
        return totais;
    }

    /**
     * Soma da quantidade <b>efetiva</b> por material e unidade copiada, só de solicitações CONCLUIDA e pela data de
     * conclusão. Item sem quantidade efetiva não entra; item com zero soma zero. A estimada nunca é usada, e a unidade
     * entra no agrupamento: kg, litros e unidades jamais são somados entre si.
     */
    public List<QuantidadeRecolhidaDTO> somarQuantidadesRecolhidas(OffsetDateTime inicio, OffsetDateTime fim) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
        Root<ItemSolicitacao> item = consulta.from(ItemSolicitacao.class);
        Join<ItemSolicitacao, Solicitacao> solicitacao = item.join("solicitacao");
        Join<ItemSolicitacao, Material> material = item.join("material");

        Expression<BigDecimal> total = cb.sum(item.<BigDecimal>get("quantidadeEfetiva"));
        consulta.multiselect(material.get("id"), material.get("nome"), item.get("unidadeCopia"), total);

        List<Predicate> filtros = new ArrayList<>();
        filtros.add(cb.equal(solicitacao.get("situacao"), SituacaoSolicitacao.CONCLUIDA));
        filtros.add(cb.isNotNull(item.get("quantidadeEfetiva")));
        filtros.addAll(periodo(cb, solicitacao.get("concluidaEm"), inicio, fim));
        consulta.where(filtros.toArray(new Predicate[0]));

        consulta.groupBy(material.get("id"), material.get("nome"), item.get("unidadeCopia"));
        consulta.orderBy(cb.asc(material.get("nome")), cb.asc(item.get("unidadeCopia")));

        return entityManager.createQuery(consulta).getResultList().stream()
                .map(linha -> new QuantidadeRecolhidaDTO(
                        linha.get(0, Integer.class),
                        linha.get(1, String.class),
                        linha.get(2, Unidade.class),
                        linha.get(3, BigDecimal.class)))
                .toList();
    }

    /** Início inclusivo e fim exclusivo; o que for nulo não filtra. */
    private List<Predicate> periodo(CriteriaBuilder cb, Expression<OffsetDateTime> data,
                                    OffsetDateTime inicio, OffsetDateTime fim) {
        List<Predicate> filtros = new ArrayList<>();
        if (inicio != null) {
            filtros.add(cb.greaterThanOrEqualTo(data, inicio));
        }
        if (fim != null) {
            filtros.add(cb.lessThan(data, fim));
        }
        return filtros;
    }
}
