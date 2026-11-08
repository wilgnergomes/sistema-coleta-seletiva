package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.ConflitoVersaoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.form.AgendamentoForm;
import br.edu.ifs.coletaseletiva.mapper.SolicitacaoMapper;
import br.edu.ifs.coletaseletiva.repository.HistoricoRepository;
import br.edu.ifs.coletaseletiva.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

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
        if (solicitacao.getSituacao() != SituacaoSolicitacao.SOLICITADA) {
            throw transicaoInvalida(solicitacao.getSituacao(), SituacaoSolicitacao.AGENDADA);
        }
        exigirVersao(solicitacao, formulario.versao());

        solicitacao.setAgendadaPara(formulario.dataHora());
        return aplicar(solicitacao, SituacaoSolicitacao.AGENDADA, operador, agora, null, formulario.dataHora());
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
    private SolicitacaoDetalheDTO aplicar(Solicitacao solicitacao, SituacaoSolicitacao destino, Operador operador,
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
                .operador(operador)
                .motivo(motivo)
                .agendamento(agendamento)
                .build());
        return mapper.paraDetalhe(solicitacao, historicoRepository.findBySolicitacaoIdOrderById(solicitacao.getId()));
    }
}
