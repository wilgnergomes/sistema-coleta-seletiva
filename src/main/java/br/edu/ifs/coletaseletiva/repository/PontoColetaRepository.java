package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** JpaSpecificationExecutor acrescenta findAll(Specification, Pageable), usado na pesquisa (RF06). */
public interface PontoColetaRepository extends JpaRepository<PontoColeta, Integer>, JpaSpecificationExecutor<PontoColeta> {
}
