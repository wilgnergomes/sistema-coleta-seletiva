package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.entity.ItemSolicitacao;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.ConflitoVersaoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.form.AgendamentoForm;
import br.edu.ifs.coletaseletiva.form.ConclusaoForm;
import br.edu.ifs.coletaseletiva.form.ItemConclusaoForm;
import br.edu.ifs.coletaseletiva.form.MotivoForm;
import br.edu.ifs.coletaseletiva.form.VersaoForm;
import br.edu.ifs.coletaseletiva.mapper.SolicitacaoMapper;
import br.edu.ifs.coletaseletiva.repository.HistoricoRepository;
import br.edu.ifs.coletaseletiva.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mudanças de situação da solicitação (RN06). Cada ação é uma única transação: altera a solicitação e grava o
 * histórico, ou não faz nada (ADR 0009). O responsável do histórico sai do ator, nunca do corpo (ADR 0011).
 *
 * <p>A lógica comum (contexto do operador, versão, transição permitida e histórico) fica aqui para que o RF10 e os
 * RF11 a RF14 não repitam regras.
 */
@Service
@RequiredArgsConstructor
public class TransicaoSolicitacaoService {

    private final SolicitacaoRepository repository;
    private final HistoricoRepository historicoRepository;
    private final OperadorService operadorService;
    private final MoradorService moradorService;
    private final SolicitacaoMapper mapper;
    private final Clock relogio;

    /**
     * RF10: o operador ativo do ponto aceita a solicitação e marca a data. Só SOLICITADA -> AGENDADA; o reagendamento
     * (AGENDADA -> AGENDADA) é do RF14. Não revalida a RN01: o ponto pode até ter sido inativado depois do pedido.
     */
    @Transactional
    public SolicitacaoDetalheDTO agendar(Ator ator, Integer id, AgendamentoForm formulario) {
        Operador operador = operadorDoAtor(ator);
        Solicitacao solicitacao = solicitacaoDoPonto(operador, id);

        OffsetDateTime agora = OffsetDateTime.now(relogio);
        if (!formulario.dataHora().isAfter(agora)) {
            throw new ParametroInvalidoException("dataHora", "erro.solicitacao.agendamento-passado");
        }
        exigirSituacao(solicitacao, SituacaoSolicitacao.SOLICITADA, SituacaoSolicitacao.AGENDADA);
        exigirVersao(solicitacao, formulario.versao());

        solicitacao.setAgendadaPara(formulario.dataHora());
        return aplicar(solicitacao, SituacaoSolicitacao.AGENDADA, Responsavel.de(operador), agora, null,
                formulario.dataHora());
    }

    /**
     * RF11: o operador ativo do ponto recusa a solicitação, com justificativa. Só SOLICITADA -> RECUSADA; depois disso
     * a solicitação é final (RN06). O motivo chega validado e sem espaços nas pontas (MotivoForm).
     */
    @Transactional
    public SolicitacaoDetalheDTO recusar(Ator ator, Integer id, MotivoForm formulario) {
        Operador operador = operadorDoAtor(ator);
        Solicitacao solicitacao = solicitacaoDoPonto(operador, id);

        exigirSituacao(solicitacao, SituacaoSolicitacao.SOLICITADA, SituacaoSolicitacao.RECUSADA);
        exigirVersao(solicitacao, formulario.versao());

        return aplicar(solicitacao, SituacaoSolicitacao.RECUSADA, Responsavel.de(operador), OffsetDateTime.now(relogio),
                formulario.motivo(), null);
    }

    /**
     * RF12: o morador dono (ativo) ou o operador ativo do ponto cancela a solicitação, com justificativa. Só
     * SOLICITADA -> CANCELADA e AGENDADA -> CANCELADA; recolhimento iniciado ou concluído, e as situações finais,
     * são rejeitados (409). Se estava AGENDADA, {@code agendadaPara} é preservado como registro da data marcada.
     * O responsável do histórico é o ator que cancelou (ADR 0011).
     */
    @Transactional
    public SolicitacaoDetalheDTO cancelar(Ator ator, Integer id, MotivoForm formulario) {
        Contexto contexto = contextoDoCancelamento(ator, id);
        Solicitacao solicitacao = contexto.solicitacao();

        if (solicitacao.getSituacao() != SituacaoSolicitacao.SOLICITADA
                && solicitacao.getSituacao() != SituacaoSolicitacao.AGENDADA) {
            throw transicaoInvalida(solicitacao.getSituacao(), SituacaoSolicitacao.CANCELADA);
        }
        exigirVersao(solicitacao, formulario.versao());

        return aplicar(solicitacao, SituacaoSolicitacao.CANCELADA, contexto.responsavel(),
                OffsetDateTime.now(relogio), formulario.motivo(), null);
    }

    /**
     * RF13: o operador ativo do ponto inicia o recolhimento. Só AGENDADA -> EM_RECOLHIMENTO, e pode ser antes da data
     * marcada: o enunciado não exige que o horário tenha chegado.
     */
    @Transactional
    public SolicitacaoDetalheDTO iniciar(Ator ator, Integer id, VersaoForm formulario) {
        Operador operador = operadorDoAtor(ator);
        Solicitacao solicitacao = solicitacaoDoPonto(operador, id);

        exigirSituacao(solicitacao, SituacaoSolicitacao.AGENDADA, SituacaoSolicitacao.EM_RECOLHIMENTO);
        exigirVersao(solicitacao, formulario.versao());

        return aplicar(solicitacao, SituacaoSolicitacao.EM_RECOLHIMENTO, Responsavel.de(operador),
                OffsetDateTime.now(relogio), null, null);
    }

    /**
     * RF13: o operador ativo do ponto conclui o recolhimento com a quantidade efetiva de cada item (zero para o item
     * não recolhido, mas pelo menos uma positiva). Só EM_RECOLHIMENTO -> CONCLUIDA. Situação, quantidades, data da
     * conclusão (do servidor, pelo relógio) e histórico mudam na mesma transação: se qualquer parte falhar, nada fica.
     */
    @Transactional
    public SolicitacaoDetalheDTO concluir(Ator ator, Integer id, ConclusaoForm formulario) {
        Operador operador = operadorDoAtor(ator);
        Solicitacao solicitacao = solicitacaoDoPonto(operador, id);

        // Regra só sobre o corpo (como a data passada do agendamento): 400 no campo itens.
        if (formulario.itens().stream().noneMatch(i -> i.quantidadeEfetiva().signum() > 0)) {
            throw new ParametroInvalidoException("itens", "erro.solicitacao.conclusao-sem-quantidade-positiva");
        }
        exigirSituacao(solicitacao, SituacaoSolicitacao.EM_RECOLHIMENTO, SituacaoSolicitacao.CONCLUIDA);
        exigirVersao(solicitacao, formulario.versao());
        registrarQuantidadesEfetivas(solicitacao, formulario.itens());

        OffsetDateTime agora = OffsetDateTime.now(relogio);
        solicitacao.setConcluidaEm(agora);
        return aplicar(solicitacao, SituacaoSolicitacao.CONCLUIDA, Responsavel.de(operador), agora, null, null);
    }

    // =====================================================================
    // apoio compartilhado pelas ações de situação
    // =====================================================================

    /** Só o operador existente e ativo age (o administrador não pode ser responsável do histórico, ADR 0011). */
    private Operador operadorDoAtor(Ator ator) {
        if (ator.perfil() != Perfil.OPERADOR) {
            throw new AcessoNegadoException("erro.solicitacao.somente-operador");
        }
        return operadorService.autorizarOperador(ator);
    }

    /** A solicitação de outro ponto "não existe" para este operador (404), como no GET (política C-02). */
    private Solicitacao solicitacaoDoPonto(Operador operador, Integer id) {
        return repository.findComItensById(id)
                .filter(s -> s.getPonto().getId().equals(operador.getPonto().getId()))
                .orElseThrow(() -> new NaoEncontradoException("erro.solicitacao.nao-encontrada"));
    }

    /**
     * Quem pode cancelar e o que enxerga: o operador, as solicitações do seu ponto; o morador, as suas. Fora do
     * contexto a solicitação "não existe" (404); o administrador não participa (403, ADR 0011).
     */
    private Contexto contextoDoCancelamento(Ator ator, Integer id) {
        return switch (ator.perfil()) {
            case OPERADOR -> {
                Operador operador = operadorService.autorizarOperador(ator);
                yield new Contexto(solicitacaoDoPonto(operador, id), Responsavel.de(operador));
            }
            case MORADOR -> {
                moradorService.autorizarAcesso(ator, ator.usuarioId());
                Solicitacao solicitacao = repository.findComItensById(id)
                        .filter(s -> s.getMorador().getId().equals(ator.usuarioId()))
                        .orElseThrow(() -> new NaoEncontradoException("erro.solicitacao.nao-encontrada"));
                yield new Contexto(solicitacao, Responsavel.de(solicitacao.getMorador()));
            }
            case ADMINISTRADOR -> throw new AcessoNegadoException("erro.solicitacao.cancelamento-sem-permissao");
        };
    }

    /**
     * A ação só vale a partir de uma situação de origem. A tabela da RN06 (no enum) diz o que o domínio permite; aqui
     * cada ação escolhe a transição que expõe (por exemplo, o agendamento do RF10 não aceita AGENDADA -> AGENDADA).
     */
    private void exigirSituacao(Solicitacao solicitacao, SituacaoSolicitacao origem, SituacaoSolicitacao destino) {
        if (solicitacao.getSituacao() != origem) {
            throw transicaoInvalida(solicitacao.getSituacao(), destino);
        }
    }

    /**
     * A conclusão informa cada item da solicitação, exatamente uma vez (RF13). Valida tudo contra os itens gravados antes
     * de alterar qualquer um. A regra "inteiro para peça" (RN02) usa a unidade copiada no item (ADR 0008), a fonte
     * histórica, e não a unidade atual do cadastro do material.
     */
    private void registrarQuantidadesEfetivas(Solicitacao solicitacao, List<ItemConclusaoForm> enviados) {
        Map<Integer, ItemSolicitacao> itens = solicitacao.getItens().stream()
                .collect(Collectors.toMap(i -> i.getMaterial().getId(), Function.identity()));
        Set<Integer> informados = new HashSet<>();

        for (ItemConclusaoForm enviado : enviados) {
            ItemSolicitacao item = itens.get(enviado.materialId());
            if (item == null) {
                // O id vai como texto: um Integer seria formatado pela mensagem com separador de milhar ("999.999").
                throw new ConflitoRegraException("erro.solicitacao.conclusao-item-desconhecido",
                        String.valueOf(enviado.materialId()));
            }
            String nome = item.getMaterial().getNome();
            if (!informados.add(enviado.materialId())) {
                throw new ConflitoRegraException("erro.solicitacao.material-repetido", nome);
            }
            if (item.getUnidadeCopia() == Unidade.UN && enviado.quantidadeEfetiva().stripTrailingZeros().scale() > 0) {
                throw new ConflitoRegraException("erro.solicitacao.quantidade-inteira", nome);
            }
        }

        String faltantes = itens.values().stream()
                .filter(i -> !informados.contains(i.getMaterial().getId()))
                .sorted(Comparator.comparing(i -> i.getMaterial().getId()))
                .map(i -> i.getMaterial().getNome())
                .collect(Collectors.joining(", "));
        if (!faltantes.isEmpty()) {
            throw new ConflitoRegraException("erro.solicitacao.conclusao-item-faltante", faltantes);
        }

        enviados.forEach(e -> itens.get(e.materialId()).setQuantidadeEfetiva(e.quantidadeEfetiva()));
    }

    private void exigirVersao(Solicitacao solicitacao, Integer versaoDoCliente) {
        if (!versaoDoCliente.equals(solicitacao.getVersao())) {
            throw new ConflitoVersaoException("erro.solicitacao.versao-desatualizada");
        }
    }

    private ConflitoRegraException transicaoInvalida(SituacaoSolicitacao atual, SituacaoSolicitacao destino) {
        return new ConflitoRegraException("erro.solicitacao.transicao-invalida", atual, destino);
    }

    /**
     * Muda a situação e grava o histórico na mesma transação. A situação mudou, então o {@code @Version} sobe
     * sozinho no UPDATE; se outra transação gravou antes, o Hibernate lança o bloqueio otimista (409).
     */
    private SolicitacaoDetalheDTO aplicar(Solicitacao solicitacao, SituacaoSolicitacao destino, Responsavel responsavel,
                                          OffsetDateTime agora, String motivo, OffsetDateTime agendamento) {
        SituacaoSolicitacao anterior = solicitacao.getSituacao();
        if (!anterior.podeIrPara(destino)) {
            throw transicaoInvalida(anterior, destino);
        }
        solicitacao.setSituacao(destino);
        repository.saveAndFlush(solicitacao);
        historicoRepository.save(Historico.builder()
                .solicitacao(solicitacao)
                .situacaoAnterior(anterior)
                .situacaoNova(destino)
                .data(agora)
                .operador(responsavel.operador())
                .morador(responsavel.morador())
                .motivo(motivo)
                .agendamento(agendamento)
                .build());
        return mapper.paraDetalhe(solicitacao, historicoRepository.findBySolicitacaoIdOrderById(solicitacao.getId()));
    }

    /** Solicitação já dentro do contexto do ator e o responsável que o histórico vai registrar. */
    private record Contexto(Solicitacao solicitacao, Responsavel responsavel) {
    }

    /**
     * Quem agiu: exatamente um entre operador e morador, como o {@code CHECK} do banco exige (ADR 0011). Evita passar
     * dois parâmetros opcionais onde um deve ser nulo.
     */
    private record Responsavel(Operador operador, Morador morador) {

        Responsavel {
            if ((operador == null) == (morador == null)) {
                throw new IllegalArgumentException("o responsável é exatamente um: operador ou morador");
            }
        }

        static Responsavel de(Operador operador) {
            return new Responsavel(operador, null);
        }

        static Responsavel de(Morador morador) {
            return new Responsavel(null, morador);
        }
    }
}
