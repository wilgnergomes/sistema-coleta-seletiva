package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.config.FusoNegocio;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoResumoDTO;
import br.edu.ifs.coletaseletiva.entity.Endereco;
import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.entity.ItemSolicitacao;
import br.edu.ifs.coletaseletiva.entity.ItemSolicitacaoId;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.entity.MaterialAceito;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.ConflitoVersaoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.form.FiltroSolicitacao;
import br.edu.ifs.coletaseletiva.form.ItemForm;
import br.edu.ifs.coletaseletiva.form.SolicitacaoCorrecaoForm;
import br.edu.ifs.coletaseletiva.form.SolicitacaoForm;
import br.edu.ifs.coletaseletiva.mapper.SolicitacaoMapper;
import br.edu.ifs.coletaseletiva.repository.EnderecoRepository;
import br.edu.ifs.coletaseletiva.repository.HistoricoRepository;
import br.edu.ifs.coletaseletiva.repository.MaterialAceitoRepository;
import br.edu.ifs.coletaseletiva.repository.MaterialRepository;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
import br.edu.ifs.coletaseletiva.repository.SolicitacaoRepository;
import br.edu.ifs.coletaseletiva.repository.SolicitacaoSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Criação, consulta e correção de solicitações (RF08). Cada operação de escrita é uma única transação:
 * ou grava solicitação, itens e histórico, ou não grava nada (RN06, ADR 0009).
 */
@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private static final int LIMITE_ENDERECO_COPIA = 400;
    private static final String ALFABETO_PROTOCOLO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TENTATIVAS_PROTOCOLO = 5;
    private static final DateTimeFormatter DATA_PROTOCOLO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SolicitacaoRepository repository;
    private final HistoricoRepository historicoRepository;
    private final MoradorRepository moradorRepository;
    private final PontoColetaRepository pontoRepository;
    private final EnderecoRepository enderecoRepository;
    private final MaterialRepository materialRepository;
    private final MaterialAceitoRepository vinculoRepository;
    private final MoradorService moradorService;
    private final OperadorService operadorService;
    private final SolicitacaoMapper mapper;
    private final FusoNegocio fusoNegocio;
    private final EntityManager entityManager;

    private final SecureRandom aleatorio = new SecureRandom();

    // =====================================================================
    // criar
    // =====================================================================

    @Transactional
    public SolicitacaoDetalheDTO criar(Ator ator, SolicitacaoForm formulario) {
        Morador morador = moradorAutenticado(ator);
        // ADR 0024: o ponto é lido com bloqueio, antes de qualquer outra linha. Uma inativação do ponto ou a remoção de um
        // vínculo (RN05) não pode confirmar entre a validação da RN01 e a gravação da solicitação.
        PontoColeta ponto = pontoRepository.findComBloqueioById(formulario.pontoId())
                .orElseThrow(() -> new NaoEncontradoException("erro.ponto.nao-encontrado"));
        Endereco endereco = enderecoDoMorador(formulario.enderecoId(), morador);

        exigirPontoDisponivel(ponto);
        List<ItemSolicitacao> itens = validarEMontarItens(ponto, formulario.itens());

        Solicitacao solicitacao = Solicitacao.builder()
                .protocolo(gerarProtocolo())
                .morador(morador)
                .ponto(ponto)
                .enderecoCopia(copiarEndereco(endereco))
                .situacao(SituacaoSolicitacao.SOLICITADA)
                .criadaEm(OffsetDateTime.now())
                .observacoes(vazioParaNulo(formulario.observacoes()))
                .build();
        itens.forEach(item -> anexar(solicitacao, item));

        // saveAndFlush envia os INSERTs agora; se o histórico falhar logo depois, a transação inteira é desfeita.
        repository.saveAndFlush(solicitacao);
        historicoRepository.save(historicoInicial(solicitacao, morador));

        return detalhe(solicitacao);
    }

    // =====================================================================
    // consultar
    // =====================================================================

    /** Dono, operador do ponto ou administrador. Fora do contexto do ator, a solicitação "não existe" (404). */
    @Transactional(readOnly = true)
    public SolicitacaoDetalheDTO buscar(Ator ator, Integer id) {
        Solicitacao solicitacao = repository.findComItensById(id)
                .orElseThrow(() -> new NaoEncontradoException("erro.solicitacao.nao-encontrada"));

        boolean visivel = switch (ator.perfil()) {
            case ADMINISTRADOR -> true;
            case MORADOR -> {
                moradorService.autorizarAcesso(ator, ator.usuarioId());
                yield solicitacao.getMorador().getId().equals(ator.usuarioId());
            }
            case OPERADOR -> {
                Operador operador = operadorService.autorizarOperador(ator);
                yield solicitacao.getPonto().getId().equals(operador.getPonto().getId());
            }
        };
        if (!visivel) {
            throw new NaoEncontradoException("erro.solicitacao.nao-encontrada");
        }
        return detalhe(solicitacao);
    }

    /**
     * Consulta paginada (RF09). O contexto do ator é uma condição fixa do WHERE: o morador só enxerga as
     * suas, o operador ativo só as do ponto ao qual está vinculado no banco (ADR 0019) e o administrador
     * todas. Os filtros do usuário se somam a essa condição, nunca a substituem.
     */
    @Transactional(readOnly = true)
    public Pagina<SolicitacaoResumoDTO> listar(Ator ator, FiltroSolicitacao filtro, int page, int size) {
        Specification<Solicitacao> contexto = switch (ator.perfil()) {
            case ADMINISTRADOR -> SolicitacaoSpecifications.qualquer();
            case MORADOR -> {
                moradorService.autorizarAcesso(ator, ator.usuarioId());
                yield SolicitacaoSpecifications.doMorador(ator.usuarioId());
            }
            case OPERADOR -> SolicitacaoSpecifications.doPonto(
                    operadorService.autorizarOperador(ator).getPonto().getId());
        };
        if (filtro.de() != null && filtro.ate() != null && filtro.ate().isBefore(filtro.de())) {
            throw new ParametroInvalidoException("ate", "erro.solicitacao.periodo-invalido");
        }

        // Mais recentes primeiro; o id desempata solicitações criadas no mesmo instante e deixa a página estável.
        PageRequest paginacao = PageRequest.of(page, size, Sort.by(Sort.Order.desc("criadaEm"), Sort.Order.desc("id")));
        Page<Solicitacao> pagina = repository.findAll(
                SolicitacaoSpecifications.de(filtro, fusoNegocio).and(contexto), paginacao);
        return Pagina.de(pagina.map(mapper::paraResumo));
    }

    // =====================================================================
    // corrigir (RN04)
    // =====================================================================

    /** Só o morador dono, só em SOLICITADA e só com a versão atual. Refaz as cópias e revalida a RN01. */
    @Transactional
    public SolicitacaoDetalheDTO corrigir(Ator ator, Integer id, SolicitacaoCorrecaoForm formulario) {
        Morador morador = moradorAutenticado(ator);
        Solicitacao solicitacao = repository.findComItensById(id)
                .filter(s -> s.getMorador().getId().equals(morador.getId()))
                .orElseThrow(() -> new NaoEncontradoException("erro.solicitacao.nao-encontrada"));

        if (solicitacao.getSituacao() != SituacaoSolicitacao.SOLICITADA) {
            throw new ConflitoRegraException("erro.solicitacao.correcao-bloqueada");
        }
        if (!formulario.versao().equals(solicitacao.getVersao())) {
            throw new ConflitoVersaoException("erro.solicitacao.versao-desatualizada");
        }

        // ADR 0024: a solicitação foi lida sem bloqueio, mas o ponto só é lido (e validado) depois de travado. Assim a
        // RN01 usa o estado do ponto confirmado até agora, e não um estado antigo carregado antes da espera.
        PontoColeta ponto = pontoRepository.findComBloqueioById(solicitacao.getPonto().getId())
                .orElseThrow(() -> new NaoEncontradoException("erro.ponto.nao-encontrado"));
        Endereco endereco = enderecoDoMorador(formulario.enderecoId(), morador);
        exigirPontoDisponivel(ponto);
        List<ItemSolicitacao> novos = validarEMontarItens(ponto, formulario.itens());

        String novaCopia = copiarEndereco(endereco);
        String novasObservacoes = vazioParaNulo(formulario.observacoes());
        boolean dadosDaSolicitacaoMudaram = !novaCopia.equals(solicitacao.getEnderecoCopia())
                || !Objects.equals(novasObservacoes, solicitacao.getObservacoes());

        solicitacao.setEnderecoCopia(novaCopia);
        solicitacao.setObservacoes(novasObservacoes);
        sincronizarItens(solicitacao, novos);

        // A versão sobe 1 a cada correção (ADR 0009). Se a própria solicitação mudou, o Hibernate já soma 1 no UPDATE.
        // Se só os itens mudaram (coleção inversa, que não mexe na versão), o lock força a soma. Não pode ser os dois:
        // somar os dois faria a versão subir 2.
        if (!dadosDaSolicitacaoMudaram) {
            entityManager.lock(solicitacao, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }

        repository.saveAndFlush(solicitacao);
        // No caminho do lock, o Hibernate conclui a soma só no commit: a resposta já informa a versão nova.
        Integer versaoNova = dadosDaSolicitacaoMudaram ? solicitacao.getVersao() : solicitacao.getVersao() + 1;
        return mapper.paraDetalhe(solicitacao,
                historicoRepository.findBySolicitacaoIdOrderById(solicitacao.getId()), versaoNova);
    }

    // =====================================================================
    // regras (RN01 e RN02)
    // =====================================================================

    private void exigirPontoDisponivel(PontoColeta ponto) {
        if (ponto.getSituacao() != SituacaoCadastro.ATIVO) {
            throw new ConflitoRegraException("erro.solicitacao.ponto-inativo");
        }
        if (!ponto.isRecolhimentoDomiciliar()) {
            throw new ConflitoRegraException("erro.solicitacao.ponto-sem-recolhimento");
        }
    }

    /**
     * Valida cada item contra o material e o vínculo com o ponto e já monta as entidades com a unidade
     * copiada (ADR 0008). Qualquer violação interrompe tudo: nada é gravado.
     */
    private List<ItemSolicitacao> validarEMontarItens(PontoColeta ponto, List<ItemForm> itens) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (ItemForm item : itens) {
            if (!ids.add(item.materialId())) {
                throw new ConflitoRegraException("erro.solicitacao.material-repetido", item.materialId());
            }
        }

        Map<Integer, Material> materiais = materialRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Integer, MaterialAceito> vinculos = vinculoRepository.findByPontoIdOrderByMaterialId(ponto.getId()).stream()
                .collect(Collectors.toMap(v -> v.getId().getMaterialId(), Function.identity()));

        Map<Integer, ItemSolicitacao> resultado = new LinkedHashMap<>();
        for (ItemForm item : itens) {
            Material material = materiais.get(item.materialId());
            if (material == null) {
                throw new NaoEncontradoException("erro.material.nao-encontrado");
            }
            if (material.getSituacao() != SituacaoCadastro.ATIVO) {
                throw new ConflitoRegraException("erro.solicitacao.material-inativo", material.getNome());
            }
            MaterialAceito vinculo = vinculos.get(material.getId());
            if (vinculo == null) {
                throw new ConflitoRegraException("erro.solicitacao.material-nao-aceito", material.getNome());
            }
            if (material.getUnidade() == Unidade.UN && item.quantidadeEstimada().stripTrailingZeros().scale() > 0) {
                throw new ConflitoRegraException("erro.solicitacao.quantidade-inteira", material.getNome());
            }
            if (vinculo.getLimite() != null && item.quantidadeEstimada().compareTo(vinculo.getLimite()) > 0) {
                throw new ConflitoRegraException("erro.solicitacao.limite-excedido", material.getNome(),
                        vinculo.getLimite().stripTrailingZeros().toPlainString());
            }

            ItemSolicitacao novo = new ItemSolicitacao();
            novo.setMaterial(material);
            novo.setUnidadeCopia(material.getUnidade());
            novo.setQuantidadeEstimada(item.quantidadeEstimada());
            resultado.put(material.getId(), novo);
        }
        return List.copyOf(resultado.values());
    }

    // =====================================================================
    // apoio
    // =====================================================================

    /** Só o morador cria e corrige, e só para si: o morador vem do ator, nunca do corpo (ADR 0019). */
    private Morador moradorAutenticado(Ator ator) {
        if (ator.perfil() != Perfil.MORADOR) {
            throw new AcessoNegadoException("erro.solicitacao.somente-morador");
        }
        moradorService.autorizarAcesso(ator, ator.usuarioId());
        return moradorRepository.findById(ator.usuarioId())
                .orElseThrow(() -> new AcessoNegadoException("erro.morador.usuario-inexistente-ou-inativo"));
    }

    /** Endereço de outro morador é tratado como inexistente (404), sem revelar que ele existe. */
    private Endereco enderecoDoMorador(Integer enderecoId, Morador morador) {
        return enderecoRepository.findByIdAndMoradorId(enderecoId, morador.getId())
                .orElseThrow(() -> new NaoEncontradoException("erro.endereco.nao-encontrado"));
    }

    /**
     * Texto único do endereço (ADR 0008). A coluna guarda até 400 caracteres e o cadastro admite mais no pior caso:
     * em vez de cortar o texto em silêncio e perder dados do histórico, o pedido é recusado.
     */
    private String copiarEndereco(Endereco e) {
        String complemento = (e.getComplemento() == null || e.getComplemento().isBlank())
                ? "" : " (" + e.getComplemento() + ")";
        String texto = e.getLogradouro() + ", " + e.getNumero() + complemento + " - " + e.getBairro()
                + " - " + e.getMunicipio() + "/" + e.getUf() + " - CEP " + e.getCep();
        if (texto.length() > LIMITE_ENDERECO_COPIA) {
            throw new ConflitoRegraException("erro.solicitacao.endereco-muito-longo", LIMITE_ENDERECO_COPIA);
        }
        return texto;
    }

    /** SOL-AAAAMMDD-XXXXXX: data e 6 caracteres aleatórios; tenta de novo se já existir. */
    private String gerarProtocolo() {
        String data = OffsetDateTime.now().format(DATA_PROTOCOLO);
        for (int tentativa = 0; tentativa < TENTATIVAS_PROTOCOLO; tentativa++) {
            StringBuilder sufixo = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sufixo.append(ALFABETO_PROTOCOLO.charAt(aleatorio.nextInt(ALFABETO_PROTOCOLO.length())));
            }
            String protocolo = "SOL-" + data + "-" + sufixo;
            if (!repository.existsByProtocolo(protocolo)) {
                return protocolo;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um protocolo único");
    }

    private Historico historicoInicial(Solicitacao solicitacao, Morador morador) {
        return Historico.builder()
                .solicitacao(solicitacao)
                .situacaoAnterior(null)
                .situacaoNova(SituacaoSolicitacao.SOLICITADA)
                .data(solicitacao.getCriadaEm())
                .morador(morador)
                .build();
    }

    private void anexar(Solicitacao solicitacao, ItemSolicitacao item) {
        item.setSolicitacao(solicitacao);
        item.setId(new ItemSolicitacaoId(solicitacao.getId(), item.getMaterial().getId()));
        solicitacao.getItens().add(item);
    }

    /**
     * Atualiza no lugar os itens que continuam, retira os que saíram e acrescenta os novos. Recriar um item com a
     * mesma chave na mesma transação faria o INSERT vir antes do DELETE e violaria a chave primária.
     */
    private void sincronizarItens(Solicitacao solicitacao, List<ItemSolicitacao> novos) {
        Map<Integer, ItemSolicitacao> atuais = solicitacao.getItens().stream()
                .collect(Collectors.toMap(i -> i.getMaterial().getId(), Function.identity()));
        Set<Integer> idsNovos = new HashSet<>();
        novos.forEach(n -> idsNovos.add(n.getMaterial().getId()));

        solicitacao.getItens().removeIf(i -> !idsNovos.contains(i.getMaterial().getId()));
        for (ItemSolicitacao novo : novos) {
            ItemSolicitacao existente = atuais.get(novo.getMaterial().getId());
            if (existente != null) {
                existente.setQuantidadeEstimada(novo.getQuantidadeEstimada());
                existente.setUnidadeCopia(novo.getUnidadeCopia());
            } else {
                anexar(solicitacao, novo);
            }
        }
    }

    private SolicitacaoDetalheDTO detalhe(Solicitacao solicitacao) {
        return mapper.paraDetalhe(solicitacao, historicoRepository.findBySolicitacaoIdOrderById(solicitacao.getId()));
    }

    private String vazioParaNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto;
    }
}
