package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.mapper.PontoColetaMapper;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
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
public class PontoColetaService {

    private final PontoColetaRepository repository;
    private final PontoColetaMapper mapper;

    @Transactional
    public PontoColetaDTO criar(Ator ator, PontoColetaForm formulario) {
        exigirAdministrador(ator);
        return mapper.paraDTO(repository.save(mapper.paraEntidade(normalizar(formulario))));
    }

    /** Público, morador e operador: só pontos ATIVOS. Administrador: todos, com filtro por situação. */
    @Transactional(readOnly = true)
    public Pagina<PontoColetaDTO> listar(Optional<Ator> ator, SituacaoCadastro situacao, int page, int size) {
        Pageable paginacao = PageRequest.of(page, size, Sort.by("id"));
        SituacaoCadastro filtro = ehAdministrador(ator) ? situacao : SituacaoCadastro.ATIVO;

        Page<PontoColeta> pontos = (filtro == null)
                ? repository.findAll(paginacao)
                : repository.findBySituacao(filtro, paginacao);
        return Pagina.de(pontos.map(mapper::paraDTO));
    }

    @Transactional(readOnly = true)
    public PontoColetaDTO buscar(Optional<Ator> ator, Integer id) {
        PontoColeta ponto = buscarOuFalhar(id);
        if (ponto.getSituacao() == SituacaoCadastro.INATIVO && !ehAdministrador(ator)) {
            throw new NaoEncontradoException("erro.ponto.nao-encontrado");
        }
        return mapper.paraDTO(ponto);
    }

    @Transactional
    public PontoColetaDTO atualizar(Ator ator, Integer id, PontoColetaForm formulario) {
        exigirAdministrador(ator);
        PontoColeta ponto = buscarOuFalhar(id);
        // O bloqueio da inativação com solicitações abertas (RN05) entra no ciclo das solicitações.
        mapper.atualizar(ponto, normalizar(formulario));
        return mapper.paraDTO(repository.save(ponto));
    }

    // ---------- apoio ----------

    private PontoColeta buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("erro.ponto.nao-encontrado"));
    }

    /** Contato e horários em branco viram nulos (o banco recusa texto vazio); o endereço segue o ADR 0005. */
    private PontoColetaForm normalizar(PontoColetaForm f) {
        return new PontoColetaForm(f.nome(), nuloSeVazio(f.contato()), nuloSeVazio(f.horarios()),
                f.recolhimentoDomiciliar(), f.situacao(), NormalizacaoEndereco.normalizar(f.endereco()));
    }

    private String nuloSeVazio(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto;
    }

    private boolean ehAdministrador(Optional<Ator> ator) {
        return ator.map(a -> a.perfil() == Perfil.ADMINISTRADOR).orElse(false);
    }

    private void exigirAdministrador(Ator ator) {
        if (ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.ponto.somente-administrador");
        }
    }
}
