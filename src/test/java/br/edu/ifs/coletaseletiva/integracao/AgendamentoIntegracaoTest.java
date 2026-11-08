package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF10 de ponta a ponta: o operador do ponto agenda (SOLICITADA -> AGENDADA), com histórico e versão (RN06),
 * isolamento entre pontos (RF07) e rejeição de data passada (cenário "Agendamento inválido" do enunciado).
 */
class AgendamentoIntegracaoTest extends IntegracaoBase {

    private CenarioSolicitacao c;
    private int maria;
    private int pontoA;
    private int pontoB;
    private int operadorA;
    private int operadorB;
    private int solicitacao;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        pontoA = c.ponto("Ponto A", true, "ATIVO");
        pontoB = c.ponto("Ponto B", true, "ATIVO");
        int papel = c.material("Papel", "KG", "ATIVO");
        c.vincular(pontoA, papel, null);
        c.vincular(pontoB, papel, null);
        operadorA = c.operador("op.a@exemplo.com", pontoA, "ATIVO");
        operadorB = c.operador("op.b@exemplo.com", pontoB, "ATIVO");

        String resposta = mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(pontoA, endereco, item(papel, "2"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        solicitacao = CenarioSolicitacao.id(resposta);
    }

    private static String futuro() {
        return OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static String passado() {
        return OffsetDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static String agendamento(String dataHora, int versao) {
        return "{\"dataHora\": \"" + dataHora + "\", \"versao\": " + versao + "}";
    }

    private ResultActions agendar(String perfil, Integer usuarioId, int id, String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/" + id + "/agendamento").header("X-Perfil", perfil)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", String.valueOf(usuarioId));
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions agendarComoOperadorA(String corpo) throws Exception {
        return agendar("OPERADOR", operadorA, solicitacao, corpo);
    }

    /** Nada mudou: ainda SOLICITADA, sem agendamento, versão 0 e só o histórico inicial. */
    private void nadaMudou() {
        assertEquals("SOLICITADA", jdbc.queryForObject(
                "SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", String.class, solicitacao));
        assertNull(jdbc.queryForObject(
                "SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao));
        assertEquals(0, jdbc.queryForObject(
                "SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Integer.class, solicitacao));
        assertEquals(1, c.contar("his_historico"));
    }

    // ---------- agendamento válido ----------

    @Test
    void rf10_agendamentoValidoMudaParaAgendadaGravaDataVersaoEHistoricoComOOperador() throws Exception {
        agendarComoOperadorA(agendamento(futuro(), 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.agendadaPara").exists())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[1].situacaoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[1].responsavel.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[1].responsavel.id").value(operadorA))
                .andExpect(jsonPath("$.historico[1].agendamento").exists());

        assertEquals("AGENDADA", jdbc.queryForObject(
                "SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", String.class, solicitacao));
        assertNotNull(jdbc.queryForObject(
                "SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao));
        assertEquals(1, jdbc.queryForObject(
                "SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Integer.class, solicitacao));
        assertEquals(2, c.contar("his_historico"));
        // O responsável é exatamente um (ADR 0011): o operador, e não o morador.
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE ope_nr_id = ? AND mor_nr_id IS NULL AND his_dt_agendamento IS NOT NULL",
                Integer.class, operadorA));
    }

    @Test
    void rf10_oMoradorDonoVeOAgendamentoEOHistoricoNaConsulta() throws Exception {
        agendarComoOperadorA(agendamento(futuro(), 0)).andExpect(status().isOk());

        mockMvc.perform(get("/solicitacoes/" + solicitacao).header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.agendadaPara").exists())
                .andExpect(jsonPath("$.historico.length()").value(2));
    }

    @Test
    void rf10_operadorAtivoAgendaMesmoComOPontoInativoESemRevalidarARn01() throws Exception {
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", pontoA);
        jdbc.update("UPDATE coleta.mat_material SET mat_tx_situacao = 'INATIVO'");

        agendarComoOperadorA(agendamento(futuro(), 0))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("AGENDADA"));
    }

    // ---------- cenário "Agendamento inválido" ----------

    @Test
    void rf10_dataPassadaRetorna400NoCampoDataHoraEMantemASolicitacaoEmSolicitada() throws Exception {
        agendarComoOperadorA(agendamento(passado(), 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("dataHora"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A data e a hora do agendamento devem ser futuras."));
        agendar("OPERADOR", operadorA, solicitacao, agendamento(passado(), 0), "en")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("The scheduling date and time must be in the future."));

        nadaMudou();
    }

    @Test
    void rf10_corpoInvalidoRetorna400SemAlterarNada() throws Exception {
        agendarComoOperadorA("{\"versao\": 0}").andExpect(status().isBadRequest());
        agendarComoOperadorA("{\"dataHora\": \"" + futuro() + "\"}").andExpect(status().isBadRequest());
        agendarComoOperadorA("{\"dataHora\": \"em breve\", \"versao\": 0}").andExpect(status().isBadRequest());

        nadaMudou();
    }

    // ---------- transição e versão (RN06) ----------

    @Test
    void rf10_foraDeSolicitadaRetorna409EOReagendamentoFicaParaORf14() throws Exception {
        for (String situacao : List.of("AGENDADA", "EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            agendarComoOperadorA(agendamento(futuro(), 0))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                    .andExpect(jsonPath("$.mensagem").value(
                            "A solicitação está " + situacao + " e não pode passar para AGENDADA."));
        }

        assertEquals(1, c.contar("his_historico"));
    }

    @Test
    void rf10_versaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda() throws Exception {
        agendarComoOperadorA(agendamento(futuro(), 7))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        nadaMudou();
    }

    // ---------- quem pode (403) e o que enxerga (404): política da matriz ----------

    @Test
    void rf10_operadorDeOutroPontoRecebe404ENadaMuda() throws Exception {
        agendar("OPERADOR", operadorB, solicitacao, agendamento(futuro(), 0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));

        nadaMudou();
    }

    @Test
    void rf10_solicitacaoInexistenteRetorna404() throws Exception {
        agendar("OPERADOR", operadorA, 999999, agendamento(futuro(), 0)).andExpect(status().isNotFound());
    }

    @Test
    void rf10_moradorAdministradorSemAtorEOperadorInexistenteOuInativoRecebem403() throws Exception {
        agendar("MORADOR", maria, solicitacao, agendamento(futuro(), 0)).andExpect(status().isForbidden());
        agendar("ADMINISTRADOR", null, solicitacao, agendamento(futuro(), 0))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/agendamento").contentType(MediaType.APPLICATION_JSON)
                .content(agendamento(futuro(), 0))).andExpect(status().isForbidden());
        agendar("OPERADOR", 999999, solicitacao, agendamento(futuro(), 0)).andExpect(status().isForbidden());

        jdbc.update("UPDATE coleta.ope_operador SET ope_tx_situacao = 'INATIVO' WHERE ope_nr_id = ?", operadorA);
        agendarComoOperadorA(agendamento(futuro(), 0)).andExpect(status().isForbidden());

        nadaMudou();
    }

    // ---------- concorrência (RN06, ADR 0009) ----------

    @Test
    void rf10_agendamentosSimultaneosNaMesmaVersaoSomenteUmVence() throws Exception {
        int tentativas = 4;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            String dataHora = OffsetDateTime.now().plusDays(2 + i).truncatedTo(ChronoUnit.SECONDS).toString();
            Callable<Integer> tentativa = () -> {
                largada.await();
                return agendarComoOperadorA(agendamento(dataHora, 0)).andReturn().getResponse().getStatus();
            };
            resultados.add(pool.submit(tentativa));
        }
        largada.countDown();

        int sucessos = 0;
        int conflitos = 0;
        for (Future<Integer> resultado : resultados) {
            int codigo = resultado.get();
            if (codigo == 200) {
                sucessos++;
            } else if (codigo == 409) {
                conflitos++;
            }
        }
        pool.shutdown();

        assertEquals(1, sucessos, "só um agendamento pode usar a versão 0");
        assertEquals(tentativas - 1, conflitos, "os demais recebem 409, nunca 500");
        assertEquals(1, jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao", Integer.class));
        assertEquals(2, c.contar("his_historico"), "o histórico tem a criação e um único agendamento");
    }
}
