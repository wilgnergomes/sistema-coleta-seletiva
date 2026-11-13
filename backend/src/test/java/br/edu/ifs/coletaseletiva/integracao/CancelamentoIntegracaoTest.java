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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF12 de ponta a ponta: o morador dono ou o operador do ponto cancelam em SOLICITADA ou AGENDADA, com motivo
 * obrigatório. O responsável do histórico é o ator que cancelou. Recolhimento iniciado ou concluído é rejeitado.
 */
class CancelamentoIntegracaoTest extends IntegracaoBase {

    private static final String MOTIVO = "Não estarei em casa";

    private CenarioSolicitacao c;
    private int maria;
    private int joao;
    private int pontoA;
    private int operadorA;
    private int operadorB;
    private int solicitacao;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        joao = c.morador("joao@exemplo.com");
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

    private static String cancelamento(String motivo, int versao) {
        String texto = motivo == null ? "null" : "\"" + motivo + "\"";
        return "{\"motivo\": " + texto + ", \"versao\": " + versao + "}";
    }

    private ResultActions cancelar(String perfil, Integer usuarioId, int id, String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/" + id + "/cancelamento").header("X-Perfil", perfil)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", String.valueOf(usuarioId));
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions cancelarComoMaria(String corpo) throws Exception {
        return cancelar("MORADOR", maria, solicitacao, corpo);
    }

    private ResultActions cancelarComoOperadorA(String corpo) throws Exception {
        return cancelar("OPERADOR", operadorA, solicitacao, corpo);
    }

    private String dataMarcada() {
        return OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private void agendarComoOperadorA(int versao) throws Exception {
        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/agendamento").header("X-Perfil", "OPERADOR")
                        .header("X-Usuario-Id", String.valueOf(operadorA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataHora\": \"" + dataMarcada() + "\", \"versao\": " + versao + "}"))
                .andExpect(status().isOk());
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

    // ---------- cancelamento válido: o responsável é o ator ----------

    @Test
    void rf12_moradorDonoCancelaSolicitadaEOHistoricoGuardaOMoradorComoResponsavel() throws Exception {
        cancelarComoMaria(cancelamento("  " + MOTIVO + "  ", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[1].situacaoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("CANCELADA"))
                .andExpect(jsonPath("$.historico[1].motivo").value(MOTIVO))
                .andExpect(jsonPath("$.historico[1].responsavel.tipo").value("MORADOR"))
                .andExpect(jsonPath("$.historico[1].responsavel.id").value(maria));

        assertEquals("CANCELADA", situacaoNoBanco());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE mor_nr_id = ? AND ope_nr_id IS NULL AND his_tx_motivo = ?"
                        + " AND his_tx_situacao_anterior = 'SOLICITADA'", Integer.class, maria, MOTIVO));
    }

    @Test
    void rf12_operadorDoPontoCancelaAgendadaOHistoricoGuardaOOperadorEAPreservaADataMarcada() throws Exception {
        agendarComoOperadorA(0);
        Object dataAntes = jdbc.queryForObject(
                "SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao);
        assertNotNull(dataAntes);

        cancelarComoOperadorA(cancelamento(MOTIVO, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA"))
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.agendadaPara").exists())
                .andExpect(jsonPath("$.historico.length()").value(3))
                .andExpect(jsonPath("$.historico[2].situacaoAnterior").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[2].situacaoNova").value("CANCELADA"))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[2].responsavel.id").value(operadorA));

        assertEquals(dataAntes, jdbc.queryForObject(
                "SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao),
                "a data marcada é preservada como registro");
    }

    @Test
    void rf12_moradorTambemCancelaDepoisDeAgendadaEVeOMotivoNoProprioDetalhe() throws Exception {
        agendarComoOperadorA(0);

        cancelarComoMaria(cancelamento(MOTIVO, 1)).andExpect(status().isOk());

        mockMvc.perform(get("/solicitacoes/" + solicitacao).header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA"))
                .andExpect(jsonPath("$.historico[2].motivo").value(MOTIVO))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("MORADOR"));
    }

    @Test
    void rf12_operadorAtivoCancelaMesmoComOPontoInativo() throws Exception {
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", pontoA);

        cancelarComoOperadorA(cancelamento(MOTIVO, 0)).andExpect(status().isOk());
    }

    // ---------- motivo obrigatório (400) ----------

    @Test
    void rf12_motivoAusenteVazioSoComEspacosOuLongoDemaisRetorna400ENadaMuda() throws Exception {
        cancelarComoMaria(cancelamento(null, 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("o motivo é obrigatório"));
        cancelarComoMaria(cancelamento("", 0)).andExpect(status().isBadRequest());
        cancelarComoOperadorA(cancelamento("     ", 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        cancelarComoMaria(cancelamento("a".repeat(501), 0)).andExpect(status().isBadRequest());
        cancelarComoMaria("{\"motivo\": \"Mudança\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        cancelar("MORADOR", maria, solicitacao, cancelamento("     ", 0), "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("the reason is required"));

        nadaMudou();
    }

    // ---------- situações que não podem ser canceladas (409) ----------

    @Test
    void rf12_recolhimentoIniciadoConcluidoOuEncerradoRetorna409ENadaMuda() throws Exception {
        for (String situacao : List.of("EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            cancelarComoMaria(cancelamento(MOTIVO, 0)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                    .andExpect(jsonPath("$.mensagem").value(
                            "A solicitação está " + situacao + " e não pode passar para CANCELADA."));
            cancelarComoOperadorA(cancelamento(MOTIVO, 0)).andExpect(status().isConflict());
        }

        assertEquals(1, c.contar("his_historico"));
    }

    @Test
    void rf12_versaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda() throws Exception {
        cancelarComoMaria(cancelamento(MOTIVO, 7)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        nadaMudou();
    }

    @Test
    void rf12_canceladaEFinalNaoPodeSerCanceladaAgendadaNemRecusada() throws Exception {
        agendarComoOperadorA(0);
        cancelarComoMaria(cancelamento(MOTIVO, 1)).andExpect(status().isOk());

        cancelarComoMaria(cancelamento(MOTIVO, 2)).andExpect(status().isConflict());
        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/agendamento").header("X-Perfil", "OPERADOR")
                        .header("X-Usuario-Id", String.valueOf(operadorA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataHora\": \"" + dataMarcada() + "\", \"versao\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está CANCELADA e não pode passar para AGENDADA."));
        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/recusa").header("X-Perfil", "OPERADOR")
                        .header("X-Usuario-Id", String.valueOf(operadorA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Tarde demais\", \"versao\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está CANCELADA e não pode passar para RECUSADA."));

        assertEquals("CANCELADA", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"), "criação, agendamento e cancelamento");
    }

    // ---------- quem pode (403) e o que enxerga (404) ----------

    @Test
    void rf12_outroMoradorEOperadorDeOutroPontoRecebem404ENadaMuda() throws Exception {
        cancelar("MORADOR", joao, solicitacao, cancelamento(MOTIVO, 0)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        cancelar("OPERADOR", operadorB, solicitacao, cancelamento(MOTIVO, 0)).andExpect(status().isNotFound());
        cancelar("MORADOR", maria, 999999, cancelamento(MOTIVO, 0)).andExpect(status().isNotFound());
        cancelar("OPERADOR", operadorA, 999999, cancelamento(MOTIVO, 0)).andExpect(status().isNotFound());

        nadaMudou();
    }

    @Test
    void rf12_administradorSemAtorEUsuariosInexistentesOuInativosRecebem403() throws Exception {
        cancelar("ADMINISTRADOR", null, solicitacao, cancelamento(MOTIVO, 0)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/cancelamento").contentType(MediaType.APPLICATION_JSON)
                .content(cancelamento(MOTIVO, 0))).andExpect(status().isForbidden());
        cancelar("MORADOR", 999999, solicitacao, cancelamento(MOTIVO, 0)).andExpect(status().isForbidden());
        cancelar("OPERADOR", 999999, solicitacao, cancelamento(MOTIVO, 0)).andExpect(status().isForbidden());

        jdbc.update("UPDATE coleta.mor_morador SET mor_tx_situacao = 'INATIVO' WHERE mor_nr_id = ?", maria);
        cancelarComoMaria(cancelamento(MOTIVO, 0)).andExpect(status().isForbidden());
        jdbc.update("UPDATE coleta.ope_operador SET ope_tx_situacao = 'INATIVO' WHERE ope_nr_id = ?", operadorA);
        cancelarComoOperadorA(cancelamento(MOTIVO, 0)).andExpect(status().isForbidden());

        nadaMudou();
    }

    // ---------- concorrência (RN06, ADR 0009) ----------

    @Test
    void rf12_cancelamentosEAgendamentoSimultaneosNaMesmaVersaoSomenteUmaAcaoVence() throws Exception {
        int tentativas = 4;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            int tipo = i % 3;
            Callable<Integer> tentativa = () -> {
                largada.await();
                ResultActions resposta = switch (tipo) {
                    case 0 -> cancelarComoMaria(cancelamento(MOTIVO, 0));
                    case 1 -> cancelarComoOperadorA(cancelamento(MOTIVO, 0));
                    default -> mockMvc.perform(post("/solicitacoes/" + solicitacao + "/agendamento")
                            .header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(operadorA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"dataHora\": \"" + dataMarcada() + "\", \"versao\": 0}"));
                };
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
