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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF11 de ponta a ponta: o operador do ponto recusa uma solicitação ainda em SOLICITADA, com justificativa
 * obrigatória, e a recusa aparece no histórico que o morador consulta. RECUSADA é situação final (RN06).
 */
class RecusaIntegracaoTest extends IntegracaoBase {

    private static final String MOTIVO = "Fora da área de atendimento";

    private CenarioSolicitacao c;
    private int maria;
    private int pontoA;
    private int operadorA;
    private int operadorB;
    private int solicitacao;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        pontoA = c.ponto("Ponto A", true, "ATIVO");
        int pontoB = c.ponto("Ponto B", true, "ATIVO");
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

    private static String recusa(String motivo, int versao) {
        String texto = motivo == null ? "null" : "\"" + motivo + "\"";
        return "{\"motivo\": " + texto + ", \"versao\": " + versao + "}";
    }

    private ResultActions recusar(String perfil, Integer usuarioId, int id, String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/" + id + "/recusa").header("X-Perfil", perfil)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", String.valueOf(usuarioId));
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions recusarComoOperadorA(String corpo) throws Exception {
        return recusar("OPERADOR", operadorA, solicitacao, corpo);
    }

    private ResultActions agendarComoOperadorA(int versao) throws Exception {
        String dataHora = OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString();
        return mockMvc.perform(post("/solicitacoes/" + solicitacao + "/agendamento").header("X-Perfil", "OPERADOR")
                .header("X-Usuario-Id", String.valueOf(operadorA)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataHora\": \"" + dataHora + "\", \"versao\": " + versao + "}"));
    }

    private String situacaoNoBanco() {
        return jdbc.queryForObject("SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                String.class, solicitacao);
    }

    /** Nada mudou: ainda SOLICITADA, versão 0 e só o histórico inicial. */
    private void nadaMudou() {
        assertEquals("SOLICITADA", situacaoNoBanco());
        assertEquals(0, jdbc.queryForObject(
                "SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Integer.class, solicitacao));
        assertEquals(1, c.contar("his_historico"));
    }

    // ---------- recusa válida e o histórico do morador ----------

    @Test
    void rf11_recusaValidaMudaParaRecusadaEGravaMotivoEOperadorNoHistorico() throws Exception {
        recusarComoOperadorA(recusa("  " + MOTIVO + "  ", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("RECUSADA"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.agendadaPara").doesNotExist())
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[1].situacaoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("RECUSADA"))
                .andExpect(jsonPath("$.historico[1].motivo").value(MOTIVO))
                .andExpect(jsonPath("$.historico[1].responsavel.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[1].responsavel.id").value(operadorA));

        assertEquals("RECUSADA", situacaoNoBanco());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE ope_nr_id = ? AND mor_nr_id IS NULL AND his_tx_motivo = ?",
                Integer.class, operadorA, MOTIVO));
    }

    @Test
    void rf11_oMoradorDonoVeARecusaEOMotivoNoDetalheENaListagemPorSituacao() throws Exception {
        recusarComoOperadorA(recusa(MOTIVO, 0)).andExpect(status().isOk());

        mockMvc.perform(get("/solicitacoes/" + solicitacao).header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("RECUSADA"))
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("RECUSADA"))
                .andExpect(jsonPath("$.historico[1].motivo").value(MOTIVO));
        mockMvc.perform(get("/solicitacoes").param("situacao", "RECUSADA").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(solicitacao));
    }

    @Test
    void rf11_operadorAtivoRecusaMesmoComOPontoInativo() throws Exception {
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", pontoA);

        recusarComoOperadorA(recusa(MOTIVO, 0)).andExpect(status().isOk());
    }

    // ---------- justificativa obrigatória (400) ----------

    @Test
    void rf11_recusaSemMotivoOuComMotivoInvalidoRetorna400NoCampoMotivoENadaMuda() throws Exception {
        recusarComoOperadorA(recusa(null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("o motivo é obrigatório"));
        recusarComoOperadorA(recusa("", 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        recusarComoOperadorA(recusa("     ", 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        recusarComoOperadorA("{\"versao\": 0}").andExpect(status().isBadRequest());
        recusarComoOperadorA(recusa("a".repeat(501), 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("deve ter no máximo 500 caracteres"));
        recusarComoOperadorA("{\"motivo\": \"Sem condições\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        recusar("OPERADOR", operadorA, solicitacao, recusa("     ", 0), "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("the reason is required"));

        nadaMudou();
    }

    @Test
    void rf11_motivoComExatamente500CaracteresEAceito() throws Exception {
        recusarComoOperadorA(recusa("a".repeat(500), 0)).andExpect(status().isOk());

        assertEquals(500, jdbc.queryForObject("SELECT LENGTH(his_tx_motivo) FROM coleta.his_historico WHERE his_tx_motivo IS NOT NULL",
                Integer.class));
    }

    // ---------- transição, versão e estado final (409) ----------

    @Test
    void rf11_foraDeSolicitadaRetorna409InclusiveDepoisDeAgendar() throws Exception {
        agendarComoOperadorA(0).andExpect(status().isOk());

        recusarComoOperadorA(recusa(MOTIVO, 1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A solicitação está AGENDADA e não pode passar para RECUSADA."));
        for (String situacao : List.of("EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            recusarComoOperadorA(recusa(MOTIVO, 1)).andExpect(status().isConflict());
        }

        assertEquals(2, c.contar("his_historico"), "só a criação e o agendamento");
    }

    @Test
    void rf11_versaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda() throws Exception {
        recusarComoOperadorA(recusa(MOTIVO, 9)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        nadaMudou();
    }

    @Test
    void rf11_recusadaEFinalNaoPodeSerRecusadaNemAgendada() throws Exception {
        recusarComoOperadorA(recusa(MOTIVO, 0)).andExpect(status().isOk());

        recusarComoOperadorA(recusa(MOTIVO, 1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está RECUSADA e não pode passar para RECUSADA."));
        agendarComoOperadorA(1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está RECUSADA e não pode passar para AGENDADA."));

        assertEquals("RECUSADA", situacaoNoBanco());
        assertEquals(2, c.contar("his_historico"));
        assertNull(jdbc.queryForObject("SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                Object.class, solicitacao));
    }

    // ---------- quem pode (403) e o que enxerga (404) ----------

    @Test
    void rf11_operadorDeOutroPontoRecebe404ENadaMuda() throws Exception {
        recusar("OPERADOR", operadorB, solicitacao, recusa(MOTIVO, 0)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        recusar("OPERADOR", operadorA, 999999, recusa(MOTIVO, 0)).andExpect(status().isNotFound());

        nadaMudou();
    }

    @Test
    void rf11_moradorAdministradorSemAtorEOperadorInexistenteOuInativoRecebem403() throws Exception {
        recusar("MORADOR", maria, solicitacao, recusa(MOTIVO, 0)).andExpect(status().isForbidden());
        recusar("ADMINISTRADOR", null, solicitacao, recusa(MOTIVO, 0)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/recusa").contentType(MediaType.APPLICATION_JSON)
                .content(recusa(MOTIVO, 0))).andExpect(status().isForbidden());
        recusar("OPERADOR", 999999, solicitacao, recusa(MOTIVO, 0)).andExpect(status().isForbidden());

        jdbc.update("UPDATE coleta.ope_operador SET ope_tx_situacao = 'INATIVO' WHERE ope_nr_id = ?", operadorA);
        recusarComoOperadorA(recusa(MOTIVO, 0)).andExpect(status().isForbidden());

        nadaMudou();
    }

    // ---------- concorrência (RN06, ADR 0009) ----------

    @Test
    void rf11_recusasEAgendamentosSimultaneosNaMesmaVersaoSomenteUmaAcaoVence() throws Exception {
        int tentativas = 4;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            boolean recusa = i % 2 == 0;
            Callable<Integer> tentativa = () -> {
                largada.await();
                ResultActions resposta = recusa ? recusarComoOperadorA(recusa(MOTIVO, 0)) : agendarComoOperadorA(0);
                return resposta.andReturn().getResponse().getStatus();
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

        assertEquals(1, sucessos, "só uma ação pode usar a versão 0");
        assertEquals(tentativas - 1, conflitos, "as demais recebem 409, nunca 500");
        assertEquals(1, jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao", Integer.class));
        assertEquals(2, c.contar("his_historico"), "o histórico tem a criação e uma única mudança");
    }
}
