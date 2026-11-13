package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.config.FusoNegocio;
import br.edu.ifs.coletaseletiva.dto.QuantidadeRecolhidaDTO;
import br.edu.ifs.coletaseletiva.dto.ResumoGerencialDTO;
import br.edu.ifs.coletaseletiva.dto.SituacaoTotalDTO;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.repository.ResumoGerencialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regras do resumo gerencial (RF15) sem Spring nem banco: acesso, período no fuso de negócio e as seis situações. */
@ExtendWith(MockitoExtension.class)
class ResumoGerencialServiceTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);

    @Mock
    private ResumoGerencialRepository repository;

    private ResumoGerencialService service;

    @BeforeEach
    void preparar() {
        service = new ResumoGerencialService(repository, new FusoNegocio("America/Sao_Paulo"));
    }

    @Test
    void rf15_devolveSempreAsSeisSituacoesNaOrdemDoEnumComZeroQuandoNaoHaOcorrencia() {
        Map<SituacaoSolicitacao, Long> contagem = new EnumMap<>(SituacaoSolicitacao.class);
        contagem.put(SituacaoSolicitacao.CONCLUIDA, 4L);
        contagem.put(SituacaoSolicitacao.SOLICITADA, 2L);
        when(repository.contarPorSituacao(any(), any())).thenReturn(contagem);
        when(repository.somarQuantidadesRecolhidas(any(), any())).thenReturn(List.of());

        ResumoGerencialDTO resumo = service.resumir(ADMINISTRADOR, null, null);

        assertEquals(List.of(
                new SituacaoTotalDTO(SituacaoSolicitacao.SOLICITADA, 2),
                new SituacaoTotalDTO(SituacaoSolicitacao.AGENDADA, 0),
                new SituacaoTotalDTO(SituacaoSolicitacao.EM_RECOLHIMENTO, 0),
                new SituacaoTotalDTO(SituacaoSolicitacao.CONCLUIDA, 4),
                new SituacaoTotalDTO(SituacaoSolicitacao.RECUSADA, 0),
                new SituacaoTotalDTO(SituacaoSolicitacao.CANCELADA, 0)), resumo.solicitacoesPorSituacao());
        assertEquals(List.of(), resumo.quantidadesRecolhidas());
    }

    @Test
    void rf15_repassaAsQuantidadesPorMaterialEUnidadeSemAlterar() {
        List<QuantidadeRecolhidaDTO> linhas = List.of(
                new QuantidadeRecolhidaDTO(1, "Papel", Unidade.KG, new BigDecimal("28.500")),
                new QuantidadeRecolhidaDTO(3, "Pilhas", Unidade.UN, new BigDecimal("4.000")));
        when(repository.contarPorSituacao(any(), any())).thenReturn(Map.of());
        when(repository.somarQuantidadesRecolhidas(any(), any())).thenReturn(linhas);

        assertEquals(linhas, service.resumir(ADMINISTRADOR, null, null).quantidadesRecolhidas());
    }

    @Test
    void rf15_semPeriodoNaoFiltraNada() {
        when(repository.contarPorSituacao(any(), any())).thenReturn(Map.of());
        when(repository.somarQuantidadesRecolhidas(any(), any())).thenReturn(List.of());

        service.resumir(ADMINISTRADOR, null, null);

        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).contarPorSituacao(inicio.capture(), fim.capture());
        assertNull(inicio.getValue());
        assertNull(fim.getValue());
    }

    @Test
    void rf15_oPeriodoViraIntervaloSemiabertoNoFusoDeNegocioParaOsDoisIndicadores() {
        when(repository.contarPorSituacao(any(), any())).thenReturn(Map.of());
        when(repository.somarQuantidadesRecolhidas(any(), any())).thenReturn(List.of());

        service.resumir(ADMINISTRADOR, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1));

        // 01/11 00h00 em São Paulo (UTC-3) é 03h00 UTC; o fim é o início do dia seguinte.
        OffsetDateTime inicioEsperado = OffsetDateTime.parse("2026-11-01T03:00:00Z");
        OffsetDateTime fimEsperado = OffsetDateTime.parse("2026-11-02T03:00:00Z");
        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).contarPorSituacao(inicio.capture(), fim.capture());
        assertEquals(inicioEsperado.toInstant(), inicio.getValue().toInstant());
        assertEquals(fimEsperado.toInstant(), fim.getValue().toInstant());
        verify(repository).somarQuantidadesRecolhidas(inicio.getValue(), fim.getValue());
    }

    @Test
    void rf15_soInicioOuSoFimFiltraApenasUmLado() {
        when(repository.contarPorSituacao(any(), any())).thenReturn(Map.of());
        when(repository.somarQuantidadesRecolhidas(any(), any())).thenReturn(List.of());

        service.resumir(ADMINISTRADOR, LocalDate.of(2026, 11, 1), null);
        service.resumir(ADMINISTRADOR, null, LocalDate.of(2026, 10, 31));

        // OffsetDateTime.equals também compara o offset; o que importa aqui é o instante.
        Instant limite = Instant.parse("2026-11-01T03:00:00Z");
        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository, times(2)).contarPorSituacao(inicio.capture(), fim.capture());
        assertEquals(limite, inicio.getAllValues().get(0).toInstant());
        assertNull(fim.getAllValues().get(0));
        assertNull(inicio.getAllValues().get(1));
        assertEquals(limite, fim.getAllValues().get(1).toInstant());
    }

    @Test
    void rf15_periodoComFimAntesDoInicioEh400NoCampoAteSemConsultarOBanco() {
        ParametroInvalidoException erro = assertThrows(ParametroInvalidoException.class,
                () -> service.resumir(ADMINISTRADOR, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)));

        assertEquals("ate", erro.getCampo());
        assertEquals("erro.solicitacao.periodo-invalido", erro.getChave());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = "ADMINISTRADOR", mode = EnumSource.Mode.EXCLUDE)
    void rf15_soOAdministradorConsultaOsDemaisPerfisSaoNegadosSemConsultarOBanco(Perfil perfil) {
        AcessoNegadoException erro = assertThrows(AcessoNegadoException.class,
                () -> service.resumir(new Ator(perfil, 1), null, null));

        assertEquals("erro.resumo.somente-administrador", erro.getChave());
        verifyNoInteractions(repository);
    }
}
