package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnderecoRepository extends JpaRepository<Endereco, Integer> {

    /** Consulta derivada do nome: SELECT ... WHERE morador.id = ? ORDER BY id. */
    List<Endereco> findByMoradorIdOrderById(Integer moradorId);

    /** Só devolve o endereço se ele pertencer ao morador: é isto que impede o acesso cruzado. */
    Optional<Endereco> findByIdAndMoradorId(Integer id, Integer moradorId);
}
