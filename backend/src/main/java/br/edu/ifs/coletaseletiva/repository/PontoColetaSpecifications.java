package br.edu.ifs.coletaseletiva.repository;

import br.edu.ifs.coletaseletiva.entity.MaterialAceito;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.FiltroPontoColeta;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Cada método devolve um predicado (uma condição do WHERE) reutilizável; {@link #de} junta só os que
 * foram informados. É assim que a pesquisa combina filtros opcionais sem montar SQL à mão.
 */
public final class PontoColetaSpecifications {

    private PontoColetaSpecifications() {
    }

    public static Specification<PontoColeta> de(FiltroPontoColeta filtro) {
        Specification<PontoColeta> especificacao = (root, query, cb) -> cb.conjunction();
        if (filtro.nome() != null) {
            especificacao = especificacao.and(nomeContem(filtro.nome()));
        }
        if (filtro.municipio() != null) {
            especificacao = especificacao.and(municipioIgual(filtro.municipio()));
        }
        if (filtro.bairro() != null) {
            especificacao = especificacao.and(bairroIgual(filtro.bairro()));
        }
        if (filtro.materialId() != null) {
            especificacao = especificacao.and(aceitaMaterial(filtro.materialId()));
        }
        if (filtro.situacao() != null) {
            especificacao = especificacao.and(situacaoIgual(filtro.situacao()));
        }
        return especificacao;
    }

    /** Trecho do nome, sem diferenciar maiúsculas. Os caracteres % e _ digitados valem como texto comum. */
    static Specification<PontoColeta> nomeContem(String trecho) {
        String padrao = "%" + escaparCuringas(minusculo(trecho)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), padrao, '\\');
    }

    static Specification<PontoColeta> municipioIgual(String municipio) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("endereco").get("municipio")), minusculo(municipio));
    }

    static Specification<PontoColeta> bairroIgual(String bairro) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("endereco").get("bairro")), minusculo(bairro));
    }

    static Specification<PontoColeta> situacaoIgual(SituacaoCadastro situacao) {
        return (root, query, cb) -> cb.equal(root.get("situacao"), situacao);
    }

    /**
     * "Existe um vínculo deste ponto com o material": uma subconsulta EXISTS. Ao contrário de um JOIN,
     * ela não repete o ponto nem atrapalha a contagem da paginação.
     */
    static Specification<PontoColeta> aceitaMaterial(Integer materialId) {
        return (root, query, cb) -> {
            Subquery<Integer> subconsulta = query.subquery(Integer.class);
            Root<MaterialAceito> vinculo = subconsulta.from(MaterialAceito.class);
            subconsulta.select(cb.literal(1)).where(
                    cb.equal(vinculo.get("ponto"), root),
                    cb.equal(vinculo.get("material").get("id"), materialId));
            return cb.exists(subconsulta);
        };
    }

    private static String minusculo(String texto) {
        return texto.toLowerCase(Locale.ROOT);
    }

    private static String escaparCuringas(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
