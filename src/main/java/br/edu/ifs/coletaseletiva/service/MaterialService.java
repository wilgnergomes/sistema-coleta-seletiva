package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MaterialForm;
import br.edu.ifs.coletaseletiva.mapper.MaterialMapper;
import br.edu.ifs.coletaseletiva.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository repository;
    private final MaterialMapper mapper;

    @Transactional
    public MaterialDTO criar(Ator ator, MaterialForm formulario) {
        exigirAdministrador(ator);
        if (repository.existsByNomeIgnoreCase(formulario.nome())) {
            throw new ConflitoRegraException("erro.material.nome-repetido");
        }
        return mapper.paraDTO(repository.save(mapper.paraEntidade(formulario)));
    }

    /**
     * Consulta pública (sem ator, morador ou operador): só materiais ATIVOS.
     * Administrador: todos, com filtro opcional por situação.
     */
    @Transactional(readOnly = true)
    public Pagina<MaterialDTO> listar(Optional<Ator> ator, SituacaoCadastro situacao, int page, int size) {
        Pageable paginacao = PageRequest.of(page, size, Sort.by("id"));
        SituacaoCadastro filtro = ehAdministrador(ator) ? situacao : SituacaoCadastro.ATIVO;

        Page<Material> materiais = (filtro == null)
                ? repository.findAll(paginacao)
                : repository.findBySituacao(filtro, paginacao);
        return Pagina.de(materiais.map(mapper::paraDTO));
    }

    @Transactional(readOnly = true)
    public MaterialDTO buscar(Optional<Ator> ator, Integer id) {
        Material material = buscarOuFalhar(id);
        // Quem não é administrador não deve nem saber que o material inativo existe.
        if (material.getSituacao() == SituacaoCadastro.INATIVO && !ehAdministrador(ator)) {
            throw new NaoEncontradoException("erro.material.nao-encontrado");
        }
        return mapper.paraDTO(material);
    }

    @Transactional
    public MaterialDTO atualizar(Ator ator, Integer id, MaterialForm formulario) {
        exigirAdministrador(ator);
        Material material = buscarOuFalhar(id);

        if (repository.existsByNomeIgnoreCaseAndIdNot(formulario.nome(), id)) {
            throw new ConflitoRegraException("erro.material.nome-repetido");
        }
        // ADR 0013: a unidade só muda se o material não tiver item de solicitação nem vínculo com ponto.
        if (formulario.unidade() != material.getUnidade() && temDependencias(id)) {
            throw new ConflitoRegraException("erro.material.unidade-bloqueada");
        }

        mapper.atualizar(material, formulario);
        return mapper.paraDTO(repository.save(material));
    }

    // ---------- apoio ----------

    private Material buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("erro.material.nao-encontrado"));
    }

    private boolean temDependencias(Integer id) {
        return repository.contarItensDeSolicitacao(id) > 0 || repository.contarVinculosComPontos(id) > 0;
    }

    private boolean ehAdministrador(Optional<Ator> ator) {
        return ator.map(a -> a.perfil() == Perfil.ADMINISTRADOR).orElse(false);
    }

    private void exigirAdministrador(Ator ator) {
        if (ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.material.somente-administrador");
        }
    }
}
