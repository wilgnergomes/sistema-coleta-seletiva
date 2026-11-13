package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF13 de ponta a ponta: o operador do ponto inicia (AGENDADA -> EM_RECOLHIMENTO) e conclui (EM_RECOLHIMENTO ->
 * CONCLUIDA) com a quantidade efetiva de cada item. Cenário "Execução completa" do enunciado: agendar, iniciar e
 * concluir, vendo as quantidades efetivas e todas as mudanças no histórico.
 */
class ExecucaoIntegracaoTest extends IntegracaoBase {

    private CenarioSolicitacao c;
    private int maria;
    private int pontoA;
    private int operadorA;
    private int operadorB;
    private int papel;
    private int vidro;
    private int pilhas;
    private int solicitacao;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        pontoA = c.ponto("Ponto A", true, "ATIVO");
        int pontoB = c.ponto("Ponto B", true, "ATIVO");
        papel = c.material("Papel", "KG", "ATIVO");
        vidro = c.material("Vidro", "KG", "ATIVO");
        pilhas = c.material("Pilhas", "UN", "ATIVO");
        for (int ponto : new int[]{pontoA, pontoB}) {
            c.vincular(ponto, papel, null);
            c.vincular(ponto, vidro, null);
            c.vincular(ponto, pilhas, null);
        }
        operadorA = c.operador("op.a@exemplo.com", pontoA, "ATIVO");
        operadorB = c.operador("op.b@exemplo.com", pontoB, "ATIVO");

        String resposta = mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(pontoA, endereco,
                                item(papel, "5.5") + "," + item(vidro, "10") + "," + item(pilhas, "4"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        solicitacao = CenarioSolicitacao.id(resposta);
    }

    // ---------- ajudantes ----------

    private ResultActions acao(String perfil, Integer usuarioId, int id, String caminho, String corpo, String... idioma)
            throws Exception {
        var requisicao = post("/solicitacoes/" + id + "/" + caminho).header("X-Perfil", perfil)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", String.valueOf(usuarioId));
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions comoOperadorA(String caminho, String corpo) throws Exception {
        return acao("OPERADOR", operadorA, solicitacao, caminho, corpo);
    }

    private static String versao(int versao) {
        return "{\"versao\": " + versao + "}";
    }

    private static String efetiva(int materialId, String quantidade) {
        return "{\"materialId\": " + materialId + ", \"quantidadeEfetiva\": " + quantidade + "}";
    }

    private static String conclusao(int versao, String... itens) {
        return "{\"itens\": [" + String.join(", ", itens) + "], \"versao\": " + versao + "}";
    }

    /** Conclusão válida de todos os itens: papel 5, vidro 0 (não recolhido) e pilhas 3. */
    private String conclusaoValida(int versao) {
        return conclusao(versao, efetiva(papel, "5"), efetiva(vidro, "0"), efetiva(pilhas, "3"));
    }

    private String dataMarcada() {
        return OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /** SOLICITADA -> AGENDADA, pela própria API. A versão passa a 1. */
    private void agendar() throws Exception {
        comoOperadorA("agendamento", "{\"dataHora\": \"" + dataMarcada() + "\", \"versao\": 0}").andExpect(status().isOk());
    }

    /** SOLICITADA -> AGENDADA -> EM_RECOLHIMENTO, pela própria API. A versão passa a 2. */
    private void emRecolhimento() throws Exception {
        agendar();
        comoOperadorA("inicio", versao(1)).andExpect(status().isOk());
    }

    private String situacaoNoBanco() {
        return jdbc.queryForObject("SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                String.class, solicitacao);
    }

    private int versaoNoBanco() {
        return jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                Integer.class, solicitacao);
    }

    private BigDecimal efetivaNoBanco(int materialId) {
        return jdbc.queryForObject(
                "SELECT iso_nr_quantidade_efetiva FROM coleta.iso_item_solicitacao WHERE sol_nr_id = ? AND mat_nr_id = ?",
                BigDecimal.class, solicitacao, materialId);
    }

    private void nenhumaEfetivaGravada() {
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.iso_item_solicitacao WHERE iso_nr_quantidade_efetiva IS NOT NULL", Integer.class));
        assertNull(jdbc.queryForObject(
                "SELECT sol_dt_conclusao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao));
    }

    // ---------- cenário "Execução completa" ----------

    @Test
    void rf13_agendarIniciarEConcluirGravaEfetivasDataEHistoricoCompleto() throws Exception {
        agendar();

        // O início vale antes da data marcada: o enunciado não exige que o horário tenha chegado.
        comoOperadorA("inicio", versao(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_RECOLHIMENTO"))
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.historico[2].situacaoAnterior").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[2].situacaoNova").value("EM_RECOLHIMENTO"))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("OPERADOR"));

        comoOperadorA("conclusao", conclusaoValida(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.versao").value(3))
                .andExpect(jsonPath("$.concluidaEm").exists())
                .andExpect(jsonPath("$.itens.length()").value(3))
                .andExpect(jsonPath("$.historico.length()").value(4))
                .andExpect(jsonPath("$.historico[3].situacaoAnterior").value("EM_RECOLHIMENTO"))
                .andExpect(jsonPath("$.historico[3].situacaoNova").value("CONCLUIDA"))
                .andExpect(jsonPath("$.historico[3].responsavel.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[3].responsavel.id").value(operadorA));

        assertEquals("CONCLUIDA", situacaoNoBanco());
        assertEquals(3, versaoNoBanco());
        assertEquals(0, new BigDecimal("5").compareTo(efetivaNoBanco(papel)));
        assertEquals(0, BigDecimal.ZERO.compareTo(efetivaNoBanco(vidro)), "zero para o item não recolhido");
        assertEquals(0, new BigDecimal("3").compareTo(efetivaNoBanco(pilhas)));
        assertNotNull(jdbc.queryForObject(
                "SELECT sol_dt_conclusao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao));
        assertEquals(4, c.contar("his_historico"));
        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE ope_nr_id = ? AND mor_nr_id IS NULL", Integer.class, operadorA));
        // As quantidades estimadas continuam como foram pedidas (a efetiva não as substitui).
        assertEquals(0, new BigDecimal("5.5").compareTo(jdbc.queryForObject(
                "SELECT iso_nr_quantidade_estimada FROM coleta.iso_item_solicitacao WHERE sol_nr_id = ? AND mat_nr_id = ?",
                BigDecimal.class, solicitacao, papel)));
    }

    @Test
    void rf13_oMoradorVeAsQuantidadesEfetivasEATodaAHistoriaNoDetalhe() throws Exception {
        emRecolhimento();
        comoOperadorA("conclusao", conclusaoValida(2)).andExpect(status().isOk());

        mockMvc.perform(get("/solicitacoes/" + solicitacao).header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluidaEm").exists())
                .andExpect(jsonPath("$.itens[?(@.materialId==" + papel + ")].quantidadeEfetiva").isNotEmpty())
                .andExpect(jsonPath("$.historico.length()").value(4));
    }

    @Test
    void rf13_operadorAtivoIniciaEConcluiMesmoComOPontoInativo() throws Exception {
        agendar();
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", pontoA);

        comoOperadorA("inicio", versao(1)).andExpect(status().isOk());
        comoOperadorA("conclusao", conclusaoValida(2)).andExpect(status().isOk());
    }

    // ---------- início: só AGENDADA ----------

    @Test
    void rf13_inicioForaDeAgendadaRetorna409ENadaMuda() throws Exception {
        for (String situacao : List.of("SOLICITADA", "EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            comoOperadorA("inicio", versao(0)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                    .andExpect(jsonPath("$.mensagem").value(
                            "A solicitação está " + situacao + " e não pode passar para EM_RECOLHIMENTO."));
        }

        assertEquals(1, c.contar("his_historico"));
    }

    @Test
    void rf13_inicioComVersaoDesatualizadaOuCorpoInvalidoNaoMudaNada() throws Exception {
        agendar();

        comoOperadorA("inicio", versao(0)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
        comoOperadorA("inicio", "{}").andExpect(status().isBadRequest());
        comoOperadorA("inicio", versao(-1)).andExpect(status().isBadRequest());

        assertEquals("AGENDADA", situacaoNoBanco());
        assertEquals(2, c.contar("his_historico"));
    }

    // ---------- conclusão: quantidades ----------

    @Test
    void rf13_todasAsQuantidadesZeroRetornam400NoCampoItensENadaMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "0"), efetiva(vidro, "0.000"), efetiva(pilhas, "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("itens"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Informe pelo menos uma quantidade efetiva maior que zero."));
        acao("OPERADOR", operadorA, solicitacao, "conclusao",
                conclusao(2, efetiva(papel, "0"), efetiva(vidro, "0"), efetiva(pilhas, "0")), "en")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Provide at least one actual quantity greater than zero."));

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
        nenhumaEfetivaGravada();
    }

    @Test
    void rf13_formatoInvalidoRetorna400EmCadaCampoENadaMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("conclusao", "{\"versao\": 2}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens')]").exists());
        comoOperadorA("conclusao", "{\"itens\": [], \"versao\": 2}").andExpect(status().isBadRequest());
        comoOperadorA("conclusao", "{\"itens\": [null], \"versao\": 2}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0]')].mensagem").value("o item não pode ser nulo"));
        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "-1"), efetiva(vidro, "1"), efetiva(pilhas, "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEfetiva')].mensagem")
                        .value("deve ser maior ou igual a zero"));
        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "1.2345"), efetiva(vidro, "1"), efetiva(pilhas, "1")))
                .andExpect(status().isBadRequest());
        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "1000000000"), efetiva(vidro, "1"), efetiva(pilhas, "1")))
                .andExpect(status().isBadRequest());
        comoOperadorA("conclusao", "{\"itens\": [{\"materialId\": " + papel + "}], \"versao\": 2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEfetiva')]").exists());
        comoOperadorA("conclusao", "{\"itens\": [{\"quantidadeEfetiva\": 1}], \"versao\": 2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].materialId')]").exists());
        comoOperadorA("conclusao", "{\"itens\": [" + efetiva(papel, "1") + "]}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        nenhumaEfetivaGravada();
    }

    @Test
    void rf13_fracaoParaItemEmPecaRetorna409UsandoAUnidadeCopiadaMesmoSeOCadastroMudar() throws Exception {
        emRecolhimento();
        // O cadastro atual do material muda para KG, mas a cópia no item continua UN (ADR 0008): a fonte é a cópia.
        jdbc.update("UPDATE coleta.mat_material SET mat_tx_unidade = 'KG' WHERE mat_nr_id = ?", pilhas);

        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "5"), efetiva(vidro, "1"), efetiva(pilhas, "2.5")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A quantidade do material Pilhas deve ser um número inteiro."));
        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "5.250"), efetiva(vidro, "0"), efetiva(pilhas, "3.000")))
                .andExpect(status().isOk());

        assertEquals(0, new BigDecimal("3").compareTo(efetivaNoBanco(pilhas)));
    }

    @Test
    void rf13_itemRepetidoDesconhecidoOuFaltanteRetorna409ComCausaEspecificaENadaMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "1"), efetiva(papel, "2"), efetiva(vidro, "1"), efetiva(pilhas, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O material Papel aparece mais de uma vez na solicitação."));
        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "1"), efetiva(vidro, "1"), efetiva(pilhas, "1"), efetiva(999999, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O material 999999 não é item desta solicitação."));
        comoOperadorA("conclusao", conclusao(2, efetiva(papel, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("Informe a quantidade efetiva de todos os itens. Faltam: Vidro, Pilhas."));

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
        nenhumaEfetivaGravada();
    }

    // ---------- conclusão: situação e versão ----------

    @Test
    void rf13_conclusaoForaDeEmRecolhimentoRetorna409() throws Exception {
        for (String situacao : List.of("SOLICITADA", "AGENDADA", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            comoOperadorA("conclusao", conclusaoValida(0)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value(
                            "A solicitação está " + situacao + " e não pode passar para CONCLUIDA."));
        }

        nenhumaEfetivaGravada();
        assertEquals(1, c.contar("his_historico"));
    }

    @Test
    void rf13_conclusaoComVersaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("conclusao", conclusaoValida(1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        nenhumaEfetivaGravada();
    }

    @Test
    void rf13_concluidaEFinalNaoPodeSerIniciadaConcluidaCanceladaRecusadaNemAgendada() throws Exception {
        emRecolhimento();
        comoOperadorA("conclusao", conclusaoValida(2)).andExpect(status().isOk());

        comoOperadorA("inicio", versao(3)).andExpect(status().isConflict());
        comoOperadorA("conclusao", conclusaoValida(3)).andExpect(status().isConflict());
        comoOperadorA("cancelamento", "{\"motivo\": \"Tarde demais\", \"versao\": 3}").andExpect(status().isConflict());
        comoOperadorA("recusa", "{\"motivo\": \"Tarde demais\", \"versao\": 3}").andExpect(status().isConflict());
        comoOperadorA("agendamento", "{\"dataHora\": \"" + dataMarcada() + "\", \"versao\": 3}")
                .andExpect(status().isConflict());

        assertEquals("CONCLUIDA", situacaoNoBanco());
        assertEquals(4, c.contar("his_historico"));
    }

    @Test
    void rf13_solicitacaoCanceladaNaoPodeSerIniciadaNemConcluida() throws Exception {
        agendar();
        acao("MORADOR", maria, solicitacao, "cancelamento", "{\"motivo\": \"Não estarei em casa\", \"versao\": 1}")
                .andExpect(status().isOk());

        comoOperadorA("inicio", versao(2)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está CANCELADA e não pode passar para EM_RECOLHIMENTO."));
        comoOperadorA("conclusao", conclusaoValida(2)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A solicitação está CANCELADA e não pode passar para CONCLUIDA."));

        assertEquals("CANCELADA", situacaoNoBanco());
        nenhumaEfetivaGravada();
        assertEquals(3, c.contar("his_historico"), "criação, agendamento e cancelamento");
    }

    // ---------- quem pode (403) e o que enxerga (404) ----------

    @Test
    void rf13_operadorDeOutroPontoRecebe404NoInicioENaConclusaoENadaMuda() throws Exception {
        emRecolhimento();

        acao("OPERADOR", operadorB, solicitacao, "inicio", versao(2)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        acao("OPERADOR", operadorB, solicitacao, "conclusao", conclusaoValida(2)).andExpect(status().isNotFound());
        acao("OPERADOR", operadorA, 999999, "conclusao", conclusaoValida(2)).andExpect(status().isNotFound());

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        nenhumaEfetivaGravada();
    }

    @Test
    void rf13_moradorAdministradorSemAtorEOperadorInexistenteOuInativoRecebem403() throws Exception {
        emRecolhimento();

        for (String caminho : List.of("inicio", "conclusao")) {
            String corpo = caminho.equals("inicio") ? versao(2) : conclusaoValida(2);
            acao("MORADOR", maria, solicitacao, caminho, corpo).andExpect(status().isForbidden());
            acao("ADMINISTRADOR", null, solicitacao, caminho, corpo).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mockMvc.perform(post("/solicitacoes/" + solicitacao + "/" + caminho).contentType(MediaType.APPLICATION_JSON)
                    .content(corpo)).andExpect(status().isForbidden());
            acao("OPERADOR", 999999, solicitacao, caminho, corpo).andExpect(status().isForbidden());
        }
        jdbc.update("UPDATE coleta.ope_operador SET ope_tx_situacao = 'INATIVO' WHERE ope_nr_id = ?", operadorA);
        comoOperadorA("conclusao", conclusaoValida(2)).andExpect(status().isForbidden());

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        nenhumaEfetivaGravada();
    }

    // ---------- concorrência (RN06, ADR 0009) ----------

    private List<Integer> dispararSimultaneos(List<Callable<Integer>> tentativas, CountDownLatch largada) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tentativas.size());
        List<Future<Integer>> futuros = new ArrayList<>();
        for (Callable<Integer> tentativa : tentativas) {
            futuros.add(pool.submit(tentativa));
        }
        largada.countDown();
        List<Integer> codigos = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            codigos.add(futuro.get());
        }
        pool.shutdown();
        return codigos;
    }

    @Test
    void rf13_iniciosSimultaneosNaMesmaVersaoSomenteUmVence() throws Exception {
        agendar();
        CountDownLatch largada = new CountDownLatch(1);
        List<Callable<Integer>> tentativas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tentativas.add(() -> {
                largada.await();
                return comoOperadorA("inicio", versao(1)).andReturn().getResponse().getStatus();
            });
        }

        List<Integer> codigos = dispararSimultaneos(tentativas, largada);

        assertEquals(1, codigos.stream().filter(codigo -> codigo == 200).count(), "só um início usa a versão 1");
        assertEquals(3, codigos.stream().filter(codigo -> codigo == 409).count(), "os demais recebem 409, nunca 500");
        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(2, versaoNoBanco());
        assertEquals(3, c.contar("his_historico"), "criação, agendamento e um único início");
    }

    @Test
    void rf13_conclusoesSimultaneasNaMesmaVersaoSomenteUmaVenceESuasQuantidadesFicam() throws Exception {
        emRecolhimento();
        CountDownLatch largada = new CountDownLatch(1);
        List<Callable<Integer>> tentativas = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String quantidade = String.valueOf(i);
            tentativas.add(() -> {
                largada.await();
                return comoOperadorA("conclusao",
                        conclusao(2, efetiva(papel, quantidade), efetiva(vidro, quantidade), efetiva(pilhas, quantidade)))
                        .andReturn().getResponse().getStatus();
            });
        }

        List<Integer> codigos = dispararSimultaneos(tentativas, largada);

        assertEquals(1, codigos.stream().filter(codigo -> codigo == 200).count(), "só uma conclusão usa a versão 2");
        assertEquals(3, codigos.stream().filter(codigo -> codigo == 409).count(), "as demais recebem 409, nunca 500");
        assertEquals("CONCLUIDA", situacaoNoBanco());
        assertEquals(3, versaoNoBanco());
        assertEquals(4, c.contar("his_historico"), "criação, agendamento, início e uma única conclusão");
        // As quantidades são todas da mesma conclusão vencedora: nenhuma mistura entre tentativas.
        BigDecimal vencedora = efetivaNoBanco(papel);
        assertTrue(vencedora.signum() > 0);
        assertEquals(0, vencedora.compareTo(efetivaNoBanco(vidro)));
        assertEquals(0, vencedora.compareTo(efetivaNoBanco(pilhas)));
    }
}
