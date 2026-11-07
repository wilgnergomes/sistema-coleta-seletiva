package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MoradorAtualizacaoForm;
import br.edu.ifs.coletaseletiva.form.MoradorCriacaoForm;
import br.edu.ifs.coletaseletiva.mapper.MoradorMapper;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MoradorService {

    private static final String EMAIL_REPETIDO = "erro.morador.email-repetido";

    private final MoradorRepository repository;
    private final MoradorMapper mapper;

    @Transactional
    public MoradorDTO criar(MoradorCriacaoForm formulario) {
        MoradorCriacaoForm dados = normalizar(formulario);

        if (repository.existsByEmail(dados.email())) {
            throw new ConflitoRegraException(EMAIL_REPETIDO);
        }

        Morador salvo = repository.save(mapper.paraEntidade(dados));
        return mapper.paraDTO(salvo);
    }

    @Transactional(readOnly = true)
    public Pagina<MoradorDTO> listar(Ator ator, int page, int size) {
        if (ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.morador.listar-somente-administrador");
        }

        Pageable paginacao = PageRequest.of(page, size, Sort.by("id"));
        return Pagina.de(repository.findAll(paginacao).map(mapper::paraDTO));
    }

    @Transactional(readOnly = true)
    public MoradorDTO buscar(Ator ator, Integer id) {
        autorizarAcesso(ator, id);
        return mapper.paraDTO(buscarOuFalhar(id));
    }

    @Transactional
    public MoradorDTO atualizar(Ator ator, Integer id, MoradorAtualizacaoForm formulario) {
        autorizarAcesso(ator, id);
        Morador morador = buscarOuFalhar(id);
        MoradorAtualizacaoForm dados = normalizar(formulario);

        boolean mudaSituacao = dados.situacao() != null
                && dados.situacao() != morador.getSituacao();
        if (mudaSituacao && ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.morador.situacao-somente-administrador");
        }

        if (repository.existsByEmailAndIdNot(dados.email(), id)) {
            throw new ConflitoRegraException(EMAIL_REPETIDO);
        }

        mapper.atualizar(morador, dados);
        return mapper.paraDTO(repository.save(morador));
    }

    // ---------- apoio ----------

    private Morador buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("erro.morador.nao-encontrado"));
    }

    /**
     * Administrador acessa qualquer morador; morador só o próprio; operador não acessa.
     * Público para o RF02 reutilizar a regra (endereços pertencem ao morador).
     */
    public void autorizarAcesso(Ator ator, Integer id) {
        switch (ator.perfil()) {
            case ADMINISTRADOR -> { }
            case MORADOR -> {
                if (!id.equals(ator.usuarioId())) {
                    throw new AcessoNegadoException("erro.morador.acesso-proprio-cadastro");
                }
                repository.findById(ator.usuarioId())
                        .filter(m -> m.getSituacao() == SituacaoCadastro.ATIVO)
                        .orElseThrow(() -> new AcessoNegadoException("erro.morador.usuario-inexistente-ou-inativo"));
            }
            case OPERADOR -> throw new AcessoNegadoException("erro.morador.operador-sem-acesso");
        }
    }

    private MoradorCriacaoForm normalizar(MoradorCriacaoForm f) {
        return new MoradorCriacaoForm(normalizarNome(f.nome()), normalizarEmail(f.email()), normalizarFone(f.fone()));
    }

    private MoradorAtualizacaoForm normalizar(MoradorAtualizacaoForm f) {
        return new MoradorAtualizacaoForm(normalizarNome(f.nome()), normalizarEmail(f.email()),
                normalizarFone(f.fone()), f.situacao());
    }

    private String normalizarNome(String nome) {
        return nome.trim();
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarFone(String fone) {
        return (fone == null || fone.isBlank()) ? null : fone.trim();
    }
}