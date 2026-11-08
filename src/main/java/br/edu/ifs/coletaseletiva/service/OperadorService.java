package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.OperadorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.OperadorForm;
import br.edu.ifs.coletaseletiva.mapper.OperadorMapper;
import br.edu.ifs.coletaseletiva.repository.OperadorRepository;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Administração dos responsáveis (RF07): todas as operações são do administrador. */
@Service
@RequiredArgsConstructor
public class OperadorService {

    private static final String EMAIL_REPETIDO = "erro.operador.email-repetido";

    private final OperadorRepository repository;
    private final PontoColetaRepository pontoRepository;
    private final OperadorMapper mapper;

    @Transactional
    public OperadorDTO criar(Ator ator, OperadorForm formulario) {
        exigirAdministrador(ator);
        OperadorForm dados = normalizar(formulario);
        PontoColeta ponto = buscarPonto(dados.pontoId());

        if (repository.existsByEmail(dados.email())) {
            throw new ConflitoRegraException(EMAIL_REPETIDO);
        }
        return mapper.paraDTO(repository.save(mapper.paraEntidade(dados, ponto)));
    }

    /** Sem filtro lista todos; com {@code pontoId} lista os operadores daquele ponto (inexistente = lista vazia). */
    @Transactional(readOnly = true)
    public Pagina<OperadorDTO> listar(Ator ator, Integer pontoId, int page, int size) {
        exigirAdministrador(ator);
        Pageable paginacao = PageRequest.of(page, size, Sort.by("id"));

        Page<Operador> operadores = (pontoId == null)
                ? repository.findAll(paginacao)
                : repository.findByPontoId(pontoId, paginacao);
        return Pagina.de(operadores.map(mapper::paraDTO));
    }

    @Transactional(readOnly = true)
    public OperadorDTO buscar(Ator ator, Integer id) {
        exigirAdministrador(ator);
        return mapper.paraDTO(buscarOuFalhar(id));
    }

    /** Atualiza dados, muda o ponto ou inativa. Mover de ponto e inativar não têm restrição neste requisito. */
    @Transactional
    public OperadorDTO atualizar(Ator ator, Integer id, OperadorForm formulario) {
        exigirAdministrador(ator);
        Operador operador = buscarOuFalhar(id);
        OperadorForm dados = normalizar(formulario);
        PontoColeta ponto = buscarPonto(dados.pontoId());

        if (repository.existsByEmailAndIdNot(dados.email(), id)) {
            throw new ConflitoRegraException(EMAIL_REPETIDO);
        }
        mapper.atualizar(operador, dados, ponto);
        return mapper.paraDTO(repository.save(operador));
    }

    // ---------- apoio ----------

    private Operador buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("erro.operador.nao-encontrado"));
    }

    private PontoColeta buscarPonto(Integer pontoId) {
        return pontoRepository.findById(pontoId)
                .orElseThrow(() -> new NaoEncontradoException("erro.ponto.nao-encontrado"));
    }

    /** O form já removeu espaços; aqui o e-mail vai para minúsculas (ck_ope_email_normalizado). */
    private OperadorForm normalizar(OperadorForm f) {
        return new OperadorForm(f.nome(), f.email().toLowerCase(Locale.ROOT), f.pontoId(), f.situacao());
    }

    private void exigirAdministrador(Ator ator) {
        if (ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.operador.somente-administrador");
        }
    }
}
