package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regras do agendamento (RF10) sem Spring nem banco. O {@link Clock} é fixo, então a fronteira da regra
 * "data futura" é testada com exatidão: o instante atual é rejeitado e um segundo depois é aceito.
 */
@ExtendWith(MockitoExtension.class)
class TransicaoSolicitacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");
    private static final Ator OPERADOR = new Ator(Perfil.OPERADOR, 5);

    @Mock
    private SolicitacaoRepository repository;
    @Mock
    private HistoricoRepository historicoRepository;
    @Mock
    private OperadorService operadorService;

    private TransicaoSolicitacaoService service;
    private Operador operador;
    private Solicitacao solicitacao;

    @BeforeEach
    void preparar() {
        service = new TransicaoSolicitacaoService(repository, historicoRepository, operadorService,
                new SolicitacaoMapper(), Clock.fixed(AGORA, ZoneOffset.UTC));
        PontoColeta ponto = PontoColeta.builder().id(1).build();
        operador = Operador.builder().id(5).ponto(ponto).build();
        solicitacao = Solicitacao.builder().id(9).protocolo("SOL-20261110-AAAAAA")
                .morador(Morador.builder().id(3).build()).ponto(ponto).enderecoCopia("Rua A, 1")
                .situacao(SituacaoSolicitacao.SOLICITADA).criadaEm(OffsetDateTime.parse("2026-11-09T10:00:00Z"))
                .versao(0).build();
    }

    private void operadorEncontraASolicitacao() {
        when(operadorService.autorizarOperador(OPERADOR)).thenReturn(operador);
        when(repository.findComItensById(9)).thenReturn(Optional.of(solicitacao));
    }

    private static AgendamentoForm depoisDeAgora(long segundos, int versao) {
        return new AgendamentoForm(OffsetDateTime.ofInstant(AGORA.plusSeconds(segundos), ZoneOffset.UTC), versao);
    }

    @Test
    void rf10_agendamentoUmSegundoNoFuturoEAceitoEGravaHistoricoComOResponsavelDoAtor() {
        operadorEncontraASolicitacao();
        when(historicoRepository.findBySolicitacaoIdOrderById(9)).thenReturn(List.of());

        AgendamentoForm formulario = depoisDeAgora(1, 0);
        SolicitacaoDetalheDTO resposta = service.agendar(OPERADOR, 9, formulario);

        assertEquals(SituacaoSolicitacao.AGENDADA, resposta.situacao());
        assertEquals(formulario.dataHora(), resposta.agendadaPara());
        ArgumentCaptor<Historico> historico = ArgumentCaptor.forClass(Historico.class);
        verify(historicoRepository).save(historico.capture());
        assertEquals(SituacaoSolicitacao.SOLICITADA, historico.getValue().getSituacaoAnterior());
        assertEquals(SituacaoSolicitacao.AGENDADA, historico.getValue().getSituacaoNova());
        assertEquals(operador, historico.getValue().getOperador());
        assertNull(historico.getValue().getMorador());
        assertEquals(formulario.dataHora(), historico.getValue().getAgendamento());
        assertEquals(OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC).toInstant(), historico.getValue().getData().toInstant());
    }

    @Test
    void rf10_agendamentoNoInstanteAtualOuNoPassadoEhRejeitadoSemGravarNada() {
        operadorEncontraASolicitacao();

        ParametroInvalidoException igual = assertThrows(ParametroInvalidoException.class,
                () -> service.agendar(OPERADOR, 9, depoisDeAgora(0, 0)));
        assertEquals("dataHora", igual.getCampo());
        assertEquals("erro.solicitacao.agendamento-passado", igual.getChave());
        assertThrows(ParametroInvalidoException.class, () -> service.agendar(OPERADOR, 9, depoisDeAgora(-1, 0)));

        verify(repository, never()).saveAndFlush(any());
        verify(historicoRepository, never()).save(any());
        assertEquals(SituacaoSolicitacao.SOLICITADA, solicitacao.getSituacao());
        assertNull(solicitacao.getAgendadaPara());
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoSolicitacao.class, names = "SOLICITADA", mode = EnumSource.Mode.EXCLUDE)
    void rf10_soSolicitadaPodeSerAgendadaInclusiveOReagendamentoFicaParaORf14(SituacaoSolicitacao situacao) {
        solicitacao.setSituacao(situacao);
        operadorEncontraASolicitacao();

        ConflitoRegraException erro = assertThrows(ConflitoRegraException.class,
                () -> service.agendar(OPERADOR, 9, depoisDeAgora(60, 0)));

        assertEquals("erro.solicitacao.transicao-invalida", erro.getChave());
        verify(historicoRepository, never()).save(any());
        assertEquals(situacao, solicitacao.getSituacao());
    }

    @Test
    void rf10_versaoDiferenteDaAtualEhConflitoDeVersao() {
        operadorEncontraASolicitacao();

        assertThrows(ConflitoVersaoException.class, () -> service.agendar(OPERADOR, 9, depoisDeAgora(60, 3)));

        verify(historicoRepository, never()).save(any());
        assertEquals(SituacaoSolicitacao.SOLICITADA, solicitacao.getSituacao());
    }

    @Test
    void rf10_dataInvalidaTemPrioridadeSobreASituacao() {
        solicitacao.setSituacao(SituacaoSolicitacao.CANCELADA);
        operadorEncontraASolicitacao();

        assertThrows(ParametroInvalidoException.class, () -> service.agendar(OPERADOR, 9, depoisDeAgora(-60, 0)));
    }

    @Test
    void rf10_solicitacaoDeOutroPontoEhTratadaComoInexistente() {
        Operador outro = Operador.builder().id(6).ponto(PontoColeta.builder().id(2).build()).build();
        when(operadorService.autorizarOperador(OPERADOR)).thenReturn(outro);
        when(repository.findComItensById(9)).thenReturn(Optional.of(solicitacao));

        assertThrows(NaoEncontradoException.class, () -> service.agendar(OPERADOR, 9, depoisDeAgora(60, 0)));

        verify(historicoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = "OPERADOR", mode = EnumSource.Mode.EXCLUDE)
    void rf10_soOOperadorAgendaOutrosPerfisSaoNegadosSemConsultarNada(Perfil perfil) {
        assertThrows(AcessoNegadoException.class,
                () -> service.agendar(new Ator(perfil, 1), 9, depoisDeAgora(60, 0)));

        verifyNoInteractions(repository, historicoRepository, operadorService);
    }
}
