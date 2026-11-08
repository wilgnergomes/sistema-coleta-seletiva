package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PontoColetaRepository extends JpaRepository<PontoColeta, Integer> {

    Page<PontoColeta> findBySituacao(SituacaoCadastro situacao, Pageable pageable);
}
