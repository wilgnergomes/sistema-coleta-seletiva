package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Integer>, JpaSpecificationExecutor<Solicitacao> {

    boolean existsByProtocolo(String protocolo);

    /** Traz a solicitação com os itens e seus materiais em uma só consulta. */
    @EntityGraph(attributePaths = {"itens", "itens.material"})
    Optional<Solicitacao> findComItensById(Integer id);

    /** Quantas solicitações do ponto estão em alguma das situações (RN05: as abertas bloqueiam a inativação). */
    long countByPontoIdAndSituacaoIn(Integer pontoId, Collection<SituacaoSolicitacao> situacoes);

    /**
     * Quantas solicitações do ponto, em alguma das situações, têm item do material (RN05: as abertas com o material
     * bloqueiam a remoção do vínculo). Conta cada solicitação uma vez.
     */
    @Query("""
            select count(distinct s.id) from Solicitacao s join s.itens i
            where s.ponto.id = :pontoId and s.situacao in :situacoes and i.material.id = :materialId""")
    long contarPorPontoESituacaoComMaterial(@Param("pontoId") Integer pontoId,
                                            @Param("situacoes") Collection<SituacaoSolicitacao> situacoes,
                                            @Param("materialId") Integer materialId);
}
