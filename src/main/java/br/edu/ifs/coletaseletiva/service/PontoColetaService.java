package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.FiltroPontoColeta;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.repository.PontoColetaSpecifications;
import br.edu.ifs.coletaseletiva.mapper.MaterialAceitoMapper;
import br.edu.ifs.coletaseletiva.mapper.PontoColetaMapper;
import br.edu.ifs.coletaseletiva.repository.MaterialAceitoRepository;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PontoColetaService {

    private final PontoColetaRepository repository;
    private final MaterialAceitoRepository vinculoRepository;
    private final PontoColetaMapper mapper;
    private final MaterialAceitoMapper vinculoMapper;

    @Transactional
    public PontoColetaDTO criar(Ator ator, PontoColetaForm formulario) {
        exigirAdministrador(ator);
        PontoColeta salvo = repository.save(mapper.paraEntidade(normalizar(formulario)));
        return mapper.paraDTO(salvo, materiaisDo(salvo.getId()));
    }

    /**
     * Pesquisa (RF06 e listagem administrativa do RF04). Público, morador e operador: só pontos ATIVOS.
     * Administrador: todos, com filtro opcional por situação. Os demais filtros valem para todos.
     */
    @Transactional(readOnly = true)
    public Pagina<PontoColetaDTO> listar(Optional<Ator> ator, FiltroPontoColeta filtro, int page, int size) {
        Pageable paginacao = PageRequest.of(page, size, Sort.by("id"));
        SituacaoCadastro situacao = ehAdministrador(ator) ? filtro.situacao() : SituacaoCadastro.ATIVO;
        FiltroPontoColeta efetivo = new FiltroPontoColeta(
                filtro.nome(), filtro.municipio(), filtro.bairro(), filtro.materialId(), situacao);

        Page<PontoColeta> pontos = repository.findAll(PontoColetaSpecifications.de(efetivo), paginacao);

        // Uma única consulta traz os materiais de todos os pontos da página (evita o "N+1").
        Map<Integer, List<MaterialAceitoDTO>> materiaisPorPonto = materiaisDosPontos(pontos.getContent());
        return Pagina.de(pontos.map(p -> mapper.paraDTO(p, materiaisPorPonto.getOrDefault(p.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public PontoColetaDTO buscar(Optional<Ator> ator, Integer id) {
        PontoColeta ponto = buscarOuFalhar(id);
        if (ponto.getSituacao() == SituacaoCadastro.INATIVO && !ehAdministrador(ator)) {
            throw new NaoEncontradoException("erro.ponto.nao-encontrado");
        }
        return mapper.paraDTO(ponto, materiaisDo(id));
    }

    @Transactional
    public PontoColetaDTO atualizar(Ator ator, Integer id, PontoColetaForm formulario) {
        exigirAdministrador(ator);
        PontoColeta ponto = buscarOuFalhar(id);
        // O bloqueio da inativação com solicitações abertas (RN05) entra no ciclo das solicitações.
        mapper.atualizar(ponto, normalizar(formulario));
        return mapper.paraDTO(repository.save(ponto), materiaisDo(id));
    }

    // ---------- apoio ----------

    private PontoColeta buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("erro.ponto.nao-encontrado"));
    }

    private List<MaterialAceitoDTO> materiaisDo(Integer pontoId) {
        return vinculoRepository.findByPontoIdOrderByMaterialId(pontoId).stream()
                .map(vinculoMapper::paraDTO)
                .toList();
    }

    private Map<Integer, List<MaterialAceitoDTO>> materiaisDosPontos(List<PontoColeta> pontos) {
        if (pontos.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = pontos.stream().map(PontoColeta::getId).toList();
        return vinculoRepository.findByPontoIdInOrderByMaterialId(ids).stream()
                .collect(Collectors.groupingBy(v -> v.getId().getPontoId(),
                        Collectors.mapping(vinculoMapper::paraDTO, Collectors.toList())));
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
