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
import br.edu.ifs.coletaseletiva.form.MotivoForm;
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
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private MoradorService moradorService;

    private TransicaoSolicitacaoService service;
    private Operador operador;
    private Solicitacao solicitacao;

    @BeforeEach
    void preparar() {
        service = new TransicaoSolicitacaoService(repository, historicoRepository, operadorService, moradorService,
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

    // ---------- RF11: recusa ----------

    @Test
    void rf11_recusaGravaRecusadaComOMotivoSemEspacosENoHistoricoDoOperador() {
        operadorEncontraASolicitacao();
        when(historicoRepository.findBySolicitacaoIdOrderById(9)).thenReturn(List.of());

        SolicitacaoDetalheDTO resposta = service.recusar(OPERADOR, 9, new MotivoForm("  Fora da área de atendimento  ", 0));

        assertEquals(SituacaoSolicitacao.RECUSADA, resposta.situacao());
        assertNull(resposta.agendadaPara());
        ArgumentCaptor<Historico> historico = ArgumentCaptor.forClass(Historico.class);
        verify(historicoRepository).save(historico.capture());
        assertEquals(SituacaoSolicitacao.SOLICITADA, historico.getValue().getSituacaoAnterior());
        assertEquals(SituacaoSolicitacao.RECUSADA, historico.getValue().getSituacaoNova());
        assertEquals("Fora da área de atendimento", historico.getValue().getMotivo());
        assertEquals(operador, historico.getValue().getOperador());
        assertNull(historico.getValue().getMorador());
        assertNull(historico.getValue().getAgendamento());
        assertEquals(AGORA, historico.getValue().getData().toInstant());
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoSolicitacao.class, names = "SOLICITADA", mode = EnumSource.Mode.EXCLUDE)
    void rf11_soSolicitadaPodeSerRecusada(SituacaoSolicitacao situacao) {
        solicitacao.setSituacao(situacao);
        operadorEncontraASolicitacao();

        ConflitoRegraException erro = assertThrows(ConflitoRegraException.class,
                () -> service.recusar(OPERADOR, 9, new MotivoForm("Sem condições", 0)));

        assertEquals("erro.solicitacao.transicao-invalida", erro.getChave());
        verify(historicoRepository, never()).save(any());
        assertEquals(situacao, solicitacao.getSituacao());
    }

    @Test
    void rf11_versaoDiferenteDaAtualEhConflitoDeVersaoENadaMuda() {
        operadorEncontraASolicitacao();

        assertThrows(ConflitoVersaoException.class, () -> service.recusar(OPERADOR, 9, new MotivoForm("Sem condições", 4)));

        verify(historicoRepository, never()).save(any());
        assertEquals(SituacaoSolicitacao.SOLICITADA, solicitacao.getSituacao());
    }

    @Test
    void rf11_solicitacaoDeOutroPontoEhTratadaComoInexistente() {
        Operador outro = Operador.builder().id(6).ponto(PontoColeta.builder().id(2).build()).build();
        when(operadorService.autorizarOperador(OPERADOR)).thenReturn(outro);
        when(repository.findComItensById(9)).thenReturn(Optional.of(solicitacao));

        assertThrows(NaoEncontradoException.class, () -> service.recusar(OPERADOR, 9, new MotivoForm("Sem condições", 0)));

        verify(historicoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = "OPERADOR", mode = EnumSource.Mode.EXCLUDE)
    void rf11_soOOperadorRecusaOutrosPerfisSaoNegadosSemConsultarNada(Perfil perfil) {
        assertThrows(AcessoNegadoException.class,
                () -> service.recusar(new Ator(perfil, 1), 9, new MotivoForm("Sem condições", 0)));

        verifyNoInteractions(repository, historicoRepository, operadorService);
    }

    // ---------- RF12: cancelamento ----------

    private static final Ator MORADOR = new Ator(Perfil.MORADOR, 3);

    @ParameterizedTest
    @EnumSource(value = SituacaoSolicitacao.class, names = {"SOLICITADA", "AGENDADA"})
    void rf12_operadorCancelaSolicitadaEAgendadaComResponsavelOperadorEPreservaAData(SituacaoSolicitacao situacao) {
        OffsetDateTime marcada = OffsetDateTime.parse("2026-11-20T09:00:00Z");
        solicitacao.setSituacao(situacao);
        if (situacao == SituacaoSolicitacao.AGENDADA) {
            solicitacao.setAgendadaPara(marcada);
        }
        operadorEncontraASolicitacao();
        when(historicoRepository.findBySolicitacaoIdOrderById(9)).thenReturn(List.of());

        SolicitacaoDetalheDTO resposta = service.cancelar(OPERADOR, 9, new MotivoForm("  Não estarei em casa  ", 0));

        assertEquals(SituacaoSolicitacao.CANCELADA, resposta.situacao());
        assertEquals(situacao == SituacaoSolicitacao.AGENDADA ? marcada : null, resposta.agendadaPara());
        ArgumentCaptor<Historico> historico = ArgumentCaptor.forClass(Historico.class);
        verify(historicoRepository).save(historico.capture());
        assertEquals(situacao, historico.getValue().getSituacaoAnterior());
        assertEquals(SituacaoSolicitacao.CANCELADA, historico.getValue().getSituacaoNova());
        assertEquals("Não estarei em casa", historico.getValue().getMotivo());
        assertEquals(operador, historico.getValue().getOperador());
        assertNull(historico.getValue().getMorador());
        assertNull(historico.getValue().getAgendamento());
    }

    @Test
    void rf12_moradorDonoCancelaComResponsavelMoradorENaoOperador() {
        when(repository.findComItensById(9)).thenReturn(Optional.of(solicitacao));
        when(historicoRepository.findBySolicitacaoIdOrderById(9)).thenReturn(List.of());

        SolicitacaoDetalheDTO resposta = service.cancelar(MORADOR, 9, new MotivoForm("Mudança de planos", 0));

        assertEquals(SituacaoSolicitacao.CANCELADA, resposta.situacao());
        verify(moradorService).autorizarAcesso(MORADOR, 3);
        ArgumentCaptor<Historico> historico = ArgumentCaptor.forClass(Historico.class);
        verify(historicoRepository).save(historico.capture());
        assertEquals(solicitacao.getMorador(), historico.getValue().getMorador());
        assertNull(historico.getValue().getOperador());
        assertEquals("Mudança de planos", historico.getValue().getMotivo());
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoSolicitacao.class, names = {"EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA"})
    void rf12_recolhimentoIniciadoConcluidoOuJaEncerradoNaoPodeSerCancelado(SituacaoSolicitacao situacao) {
        solicitacao.setSituacao(situacao);
        operadorEncontraASolicitacao();

        ConflitoRegraException erro = assertThrows(ConflitoRegraException.class,
                () -> service.cancelar(OPERADOR, 9, new MotivoForm("Sem condições", 0)));

        assertEquals("erro.solicitacao.transicao-invalida", erro.getChave());
        verify(historicoRepository, never()).save(any());
        assertEquals(situacao, solicitacao.getSituacao());
    }

    @Test
    void rf12_versaoDiferenteDaAtualEhConflitoDeVersaoENadaMuda() {
        operadorEncontraASolicitacao();

        assertThrows(ConflitoVersaoException.class, () -> service.cancelar(OPERADOR, 9, new MotivoForm("Sem condições", 2)));

        verify(historicoRepository, never()).save(any());
        assertEquals(SituacaoSolicitacao.SOLICITADA, solicitacao.getSituacao());
    }

    @Test
    void rf12_administradorNaoCancelaESemConsultarNada() {
        assertThrows(AcessoNegadoException.class,
                () -> service.cancelar(new Ator(Perfil.ADMINISTRADOR, null), 9, new MotivoForm("Sem condições", 0)));

        verifyNoInteractions(repository, historicoRepository, operadorService, moradorService);
    }

    @Test
    void rf12_moradorQueNaoEODonoOuOperadorDeOutroPontoRecebemNaoEncontrado() {
        when(repository.findComItensById(9)).thenReturn(Optional.of(solicitacao));

        assertThrows(NaoEncontradoException.class,
                () -> service.cancelar(new Ator(Perfil.MORADOR, 4), 9, new MotivoForm("Sem condições", 0)));

        Operador outro = Operador.builder().id(6).ponto(PontoColeta.builder().id(2).build()).build();
        when(operadorService.autorizarOperador(OPERADOR)).thenReturn(outro);
        assertThrows(NaoEncontradoException.class, () -> service.cancelar(OPERADOR, 9, new MotivoForm("Sem condições", 0)));

        verify(historicoRepository, never()).save(any());
    }

    @Test
    void rf12_moradorInativoOuInexistenteEhNegadoSemConsultarASolicitacao() {
        doThrow(new AcessoNegadoException("erro.morador.usuario-inexistente-ou-inativo"))
                .when(moradorService).autorizarAcesso(MORADOR, 3);

        assertThrows(AcessoNegadoException.class, () -> service.cancelar(MORADOR, 9, new MotivoForm("Sem condições", 0)));

        verifyNoInteractions(repository, historicoRepository);
    }
}
