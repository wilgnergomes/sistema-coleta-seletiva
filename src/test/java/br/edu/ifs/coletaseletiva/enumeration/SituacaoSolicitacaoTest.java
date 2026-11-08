package br.edu.ifs.coletaseletiva.enumeration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao.AGENDADA;
import static br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao.CANCELADA;
import static br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao.CONCLUIDA;
import static br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao.EM_RECOLHIMENTO;
import static br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao.RECUSADA;
import static br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao.SOLICITADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transições da RN06 (tabela do enunciado), testadas sem Spring. Um teste parametrizado roda a mesma verificação
 * para as 36 combinações de origem e destino: cada uma é permitida só se a tabela abaixo a prevê.
 */
class SituacaoSolicitacaoTest {

    /** A tabela do enunciado, escrita à parte do código de produção para que o teste a confira. */
    private static final Map<SituacaoSolicitacao, Set<SituacaoSolicitacao>> PERMITIDAS = Map.of(
            SOLICITADA, EnumSet.of(AGENDADA, RECUSADA, CANCELADA),
            AGENDADA, EnumSet.of(AGENDADA /* reagendamento */, EM_RECOLHIMENTO, CANCELADA),
            EM_RECOLHIMENTO, EnumSet.of(CONCLUIDA, AGENDADA /* tentativa sem sucesso */),
            CONCLUIDA, EnumSet.noneOf(SituacaoSolicitacao.class),
            RECUSADA, EnumSet.noneOf(SituacaoSolicitacao.class),
            CANCELADA, EnumSet.noneOf(SituacaoSolicitacao.class));

    static Stream<SituacaoSolicitacao[]> combinacoes() {
        return Stream.of(SituacaoSolicitacao.values())
                .flatMap(origem -> Stream.of(SituacaoSolicitacao.values())
                        .map(destino -> new SituacaoSolicitacao[]{origem, destino}));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("combinacoes")
    void rf10_transicaoEPermitidaSomenteQuandoAPrevistaNaRn06(SituacaoSolicitacao origem, SituacaoSolicitacao destino) {
        assertEquals(PERMITIDAS.get(origem).contains(destino), origem.podeIrPara(destino),
                origem + " -> " + destino);
    }

    @Test
    void rf10_situacoesFinaisNaoTemNovaTransicao() {
        for (SituacaoSolicitacao situacao : EnumSet.of(CONCLUIDA, RECUSADA, CANCELADA)) {
            assertTrue(situacao.ehFinal(), situacao + " é final");
            assertTrue(situacao.proximas().isEmpty());
        }
        for (SituacaoSolicitacao situacao : EnumSet.of(SOLICITADA, AGENDADA, EM_RECOLHIMENTO)) {
            assertFalse(situacao.ehFinal(), situacao + " não é final");
        }
    }

    @Test
    void rf10_aTabelaCobreTodasAsSituacoes() {
        assertEquals(EnumSet.allOf(SituacaoSolicitacao.class), PERMITIDAS.keySet());
    }
}
