package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.Operador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperadorRepository extends JpaRepository<Operador, Integer> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    /** Consulta derivada com paginação: WHERE ponto.id = ? */
    Page<Operador> findByPontoId(Integer pontoId, Pageable pageable);
}
