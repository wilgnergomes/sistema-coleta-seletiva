package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Integer> {

    /** Consulta derivada: compara o nome sem diferenciar maiúsculas (o banco tem índice único em LOWER(BTRIM(nome))). */
    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);

    /** Consulta derivada com paginação: findAll(pageable) já existe para o caso sem filtro. */
    Page<Material> findBySituacao(SituacaoCadastro situacao, Pageable pageable);

    /**
     * SQL nativo: item_solicitacao e material_aceito ainda não têm entidade (RF05 e RF08), então não há
     * JPQL possível. {h-schema} vira "coleta." (o schema padrão do Hibernate).
     */
    @Query(value = "SELECT COUNT(*) FROM {h-schema}iso_item_solicitacao WHERE mat_nr_id = :id", nativeQuery = true)
    long contarItensDeSolicitacao(@Param("id") Integer id);

    @Query(value = "SELECT COUNT(*) FROM {h-schema}mac_material_aceito WHERE mat_nr_id = :id", nativeQuery = true)
    long contarVinculosComPontos(@Param("id") Integer id);
}
