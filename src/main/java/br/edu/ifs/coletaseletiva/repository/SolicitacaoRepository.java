package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Integer>, JpaSpecificationExecutor<Solicitacao> {

    boolean existsByProtocolo(String protocolo);

    /** Traz a solicitação com os itens e seus materiais em uma só consulta. */
    @EntityGraph(attributePaths = {"itens", "itens.material"})
    Optional<Solicitacao> findComItensById(Integer id);
}
