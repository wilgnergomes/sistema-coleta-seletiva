package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.MaterialAceito;
import br.edu.ifs.coletaseletiva.entity.MaterialAceitoId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MaterialAceitoRepository extends JpaRepository<MaterialAceito, MaterialAceitoId> {

    /** O EntityGraph traz o material junto na mesma consulta (JOIN), evitando uma consulta por vínculo. */
    @EntityGraph(attributePaths = "material")
    List<MaterialAceito> findByPontoIdOrderByMaterialId(Integer pontoId);

    /** Versão em lote: os vínculos de vários pontos em uma única consulta (WHERE poc_nr_id IN (...)). */
    @EntityGraph(attributePaths = "material")
    List<MaterialAceito> findByPontoIdInOrderByMaterialId(Collection<Integer> pontoIds);
}
