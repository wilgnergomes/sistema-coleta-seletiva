package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/** JpaSpecificationExecutor acrescenta findAll(Specification, Pageable), usado na pesquisa (RF06). */
public interface PontoColetaRepository extends JpaRepository<PontoColeta, Integer>, JpaSpecificationExecutor<PontoColeta> {

    /**
     * Lê o ponto e <b>trava a linha</b> ({@code SELECT ... FOR UPDATE}) até o fim da transação (ADR 0024). A criação e a
     * correção de solicitações, a inativação do ponto e a remoção de um vínculo chamam isto antes de qualquer outra
     * linha: quem chega depois espera e, ao receber o bloqueio, enxerga o que a outra transação confirmou.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PontoColeta p where p.id = :id")
    Optional<PontoColeta> findComBloqueioById(@Param("id") Integer id);
}
