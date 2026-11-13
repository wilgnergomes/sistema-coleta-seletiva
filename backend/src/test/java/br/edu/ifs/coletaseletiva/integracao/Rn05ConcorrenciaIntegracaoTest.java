package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.ADMIN;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * RN05 sob concorrência (Issue #57, ADR 0024): a criação e a correção de solicitações disputam a mesma regra com a
 * inativação do ponto e com a remoção do vínculo. Todas coordenam pelo bloqueio da linha do ponto
 * ({@code SELECT ... FOR UPDATE}), adquirido antes de qualquer outra linha.
 *
 * <p>Os testes determinísticos não dependem de sorte de horário: uma transação externa <b>segura</b> o bloqueio do
 * ponto; a operação testada começa em outra thread e fica <b>esperando</b> (o teste confirma pela visão
 * {@code pg_stat_activity}); só então a transação externa faz o que "a outra operação" teria feito e confirma. A
 * operação que esperava precisa enxergar o resultado confirmado e respeitar a regra. Cada par de operações é testado
 * nas duas ordens possíveis.
 */
class Rn05ConcorrenciaIntegracaoTest extends IntegracaoBase {

    @Autowired
    private PlatformTransactionManager transacoes;

    private final AtomicInteger sequencia = new AtomicInteger();

    private CenarioSolicitacao c;
    private int maria;
    private int endereco;
    private int ponto;
    private int papel;
    private int vidro;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        endereco = c.endereco(maria);
        ponto = c.ponto("Ponto A", true, "ATIVO");
        papel = c.material("Papel", "KG", "ATIVO");
        vidro = c.material("Vidro", "KG", "ATIVO");
        c.vincular(ponto, papel, null);
        c.vincular(ponto, vidro, null);
    }

    // ---------- infraestrutura dos testes determinísticos ----------

    /**
     * Segura o bloqueio do ponto numa transação externa, dispara a operação em outra thread, espera até ela ficar
     * bloqueada e então executa {@code duranteOBloqueio} (dentro da transação externa) e confirma.
     */
    private MockHttpServletResponse disputandoComOPontoTravado(Runnable duranteOBloqueio,
                                                               Callable<ResultActions> operacao) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        AtomicReference<Future<MockHttpServletResponse>> resposta = new AtomicReference<>();
        try {
            new TransactionTemplate(transacoes).executeWithoutResult(status -> {
                jdbc.queryForList("SELECT poc_nr_id FROM coleta.poc_ponto_coleta WHERE poc_nr_id = ? FOR UPDATE", ponto);
                resposta.set(pool.submit(() -> operacao.call().andReturn().getResponse()));
                aguardarOperacaoBloqueada(resposta.get());
                duranteOBloqueio.run();
            });
            return resposta.get().get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    /** A operação testada precisa estar parada esperando um bloqueio; se terminar sozinha, ela não coordenou pelo ponto. */
    private void aguardarOperacaoBloqueada(Future<MockHttpServletResponse> operacao) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < limite) {
            if (operacao.isDone()) {
                fail("a operação terminou sem esperar o bloqueio da linha do ponto");
            }
            Integer esperando = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock'",
                    Integer.class);
            if (esperando != null && esperando > 0) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrompido");
            }
        }
        fail("a operação não ficou esperando o bloqueio da linha do ponto");
    }

    /** O que uma criação confirmada teria deixado no banco: uma solicitação aberta com um item. */
    private void inserirSolicitacaoAberta(String situacao, int materialId) {
        int id = jdbc.queryForObject("""
                INSERT INTO coleta.sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao)
                VALUES (?, ?, ?, 'Rua A, 1 - Centro - Lagarto/SE - CEP 49400000', ?) RETURNING sol_nr_id""",
                Integer.class, "SOL-20261108-T" + sequencia.incrementAndGet(), maria, ponto, situacao);
        jdbc.update("""
                INSERT INTO coleta.iso_item_solicitacao (sol_nr_id, mat_nr_id, iso_tx_copia_unidade, iso_nr_quantidade_estimada)
                VALUES (?, ?, 'KG', 1)""", id, materialId);
    }

    private ResultActions criarSolicitacao(int materialId) throws Exception {
        return mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria))
                .contentType(MediaType.APPLICATION_JSON).content(corpo(ponto, endereco, item(materialId, "1"))));
    }

    private ResultActions corrigirSolicitacao(int solicitacao, String itens) throws Exception {
        return mockMvc.perform(put("/solicitacoes/" + solicitacao).header("X-Perfil", "MORADOR")
                .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enderecoId\": " + endereco + ", \"versao\": 0, \"itens\": [" + itens + "]}"));
    }

    private ResultActions removerVinculo(int materialId) throws Exception {
        return mockMvc.perform(delete("/pontos-coleta/" + ponto + "/materiais/" + materialId).header("X-Perfil", ADMIN));
    }

    private ResultActions inativarPonto() throws Exception {
        return mockMvc.perform(put("/pontos-coleta/" + ponto).header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpoDoPonto("INATIVO")));
    }

    private static String corpoDoPonto(String situacao) {
        return "{\"nome\": \"Ponto A\", \"recolhimentoDomiciliar\": true, \"situacao\": \"" + situacao + "\", \"endereco\": "
                + "{\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"Centro\", \"municipio\": \"Lagarto\", "
                + "\"uf\": \"SE\", \"cep\": \"49400000\"}}";
    }

    private int solicitacoesNoBanco() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta.sol_solicitacao", Integer.class);
    }

    private String situacaoDoPonto() {
        return jdbc.queryForObject("SELECT poc_tx_situacao FROM coleta.poc_ponto_coleta WHERE poc_nr_id = ?", String.class, ponto);
    }

    private int vinculos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE poc_nr_id = ?", Integer.class, ponto);
    }

    // ---------- ordem 1: a criação confirma primeiro; quem altera o ponto espera e enxerga a nova solicitação ----------

    @Test
    void rn05_removerVinculoQueEsperavaEnxergaASolicitacaoAbertaConfirmadaPorUmaCriacaoEResponde409() throws Exception {
        MockHttpServletResponse resposta = disputandoComOPontoTravado(
                () -> inserirSolicitacaoAberta("SOLICITADA", papel), () -> removerVinculo(papel));

        assertEquals(409, resposta.getStatus(), resposta.getContentAsString());
        assertTrue(resposta.getContentAsString(utf8()).contains("CONFLITO_REGRA"));
        assertEquals(2, vinculos(), "o vínculo continua, porque a regra foi respeitada");
    }

    @Test
    void rn05_inativarPontoQueEsperavaEnxergaASolicitacaoAbertaConfirmadaPorUmaCriacaoEResponde409() throws Exception {
        MockHttpServletResponse resposta = disputandoComOPontoTravado(
                () -> inserirSolicitacaoAberta("SOLICITADA", papel), this::inativarPonto);

        assertEquals(409, resposta.getStatus(), resposta.getContentAsString());
        assertEquals("ATIVO", situacaoDoPonto(), "o ponto continua ativo");
    }

    // ---------- ordem 2: quem altera o ponto confirma primeiro; a criação espera e respeita o novo estado ----------

    @Test
    void rn05_criarSolicitacaoQueEsperavaRespeitaOPontoInativadoEResponde409SemGravarNada() throws Exception {
        MockHttpServletResponse resposta = disputandoComOPontoTravado(
                () -> jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", ponto),
                () -> criarSolicitacao(papel));

        assertEquals(409, resposta.getStatus(), resposta.getContentAsString());
        assertTrue(resposta.getContentAsString(utf8()).contains("O ponto de coleta está inativo."));
        assertEquals(0, solicitacoesNoBanco());
        assertEquals(0, c.contar("iso_item_solicitacao"));
        assertEquals(0, c.contar("his_historico"));
    }

    @Test
    void rn05_criarSolicitacaoQueEsperavaRespeitaOVinculoRemovidoEResponde409SemGravarNada() throws Exception {
        MockHttpServletResponse resposta = disputandoComOPontoTravado(
                () -> jdbc.update("DELETE FROM coleta.mac_material_aceito WHERE poc_nr_id = ? AND mat_nr_id = ?", ponto, papel),
                () -> criarSolicitacao(papel));

        assertEquals(409, resposta.getStatus(), resposta.getContentAsString());
        assertTrue(resposta.getContentAsString(utf8()).contains("O ponto de coleta não aceita o material Papel."));
        assertEquals(0, solicitacoesNoBanco());
    }

    // ---------- a correção valida o estado do ponto lido DEPOIS do bloqueio (não o de antes da espera) ----------

    @Test
    void rn05_corrigirSolicitacaoQueEsperavaUsaOEstadoAtualizadoDoPontoInativadoEResponde409SemAlterarNada() throws Exception {
        int solicitacao = CenarioSolicitacao.id(criarSolicitacao(papel).andReturn().getResponse().getContentAsString());

        MockHttpServletResponse resposta = disputandoComOPontoTravado(
                () -> jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", ponto),
                () -> corrigirSolicitacao(solicitacao, item(papel, "9")));

        assertEquals(409, resposta.getStatus(), resposta.getContentAsString());
        assertTrue(resposta.getContentAsString(utf8()).contains("O ponto de coleta está inativo."));
        assertEquals(0, jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                Integer.class, solicitacao));
        assertEquals(0, new java.math.BigDecimal("1").compareTo(jdbc.queryForObject(
                "SELECT iso_nr_quantidade_estimada FROM coleta.iso_item_solicitacao WHERE sol_nr_id = ?",
                java.math.BigDecimal.class, solicitacao)));
    }

    @Test
    void rn05_corrigirSolicitacaoQueEsperavaNaoAcrescentaItemCujoVinculoFoiRemovido() throws Exception {
        int solicitacao = CenarioSolicitacao.id(criarSolicitacao(vidro).andReturn().getResponse().getContentAsString());

        MockHttpServletResponse resposta = disputandoComOPontoTravado(
                () -> jdbc.update("DELETE FROM coleta.mac_material_aceito WHERE poc_nr_id = ? AND mat_nr_id = ?", ponto, papel),
                () -> corrigirSolicitacao(solicitacao, item(vidro, "1") + "," + item(papel, "2")));

        assertEquals(409, resposta.getStatus(), resposta.getContentAsString());
        assertTrue(resposta.getContentAsString(utf8()).contains("O ponto de coleta não aceita o material Papel."));
        assertEquals(1, c.contar("iso_item_solicitacao"), "nenhum item novo foi gravado");
    }

    // ---------- estresse: várias threads de verdade, com os invariantes da RN05 ----------

    /** Reinicia o cenário entre as rodadas: sem solicitações, ponto ativo e os dois vínculos. */
    private void reiniciar() throws Exception {
        jdbc.update("DELETE FROM coleta.iso_item_solicitacao");
        jdbc.update("DELETE FROM coleta.his_historico");
        jdbc.update("DELETE FROM coleta.sol_solicitacao");
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'ATIVO' WHERE poc_nr_id = ?", ponto);
        c.vincular(ponto, papel, null);
        c.vincular(ponto, vidro, null);
    }

    private List<Integer> dispararJuntas(List<Callable<Integer>> operacoes) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(operacoes.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        for (Callable<Integer> operacao : operacoes) {
            futuros.add(pool.submit(() -> {
                largada.await();
                return operacao.call();
            }));
        }
        largada.countDown();
        List<Integer> codigos = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            codigos.add(futuro.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return codigos;
    }

    private static void semErroDeServidor(List<Integer> codigos) {
        assertTrue(codigos.stream().allMatch(codigo -> codigo < 500), "nenhum 500 (nem deadlock): " + codigos);
    }

    @Test
    void rn05_estresseCriacoesContraInativacaoNuncaDeixamPontoInativoComSolicitacaoAberta() throws Exception {
        for (int rodada = 0; rodada < 8; rodada++) {
            reiniciar();
            List<Callable<Integer>> operacoes = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                operacoes.add(() -> criarSolicitacao(papel).andReturn().getResponse().getStatus());
            }
            operacoes.add(() -> inativarPonto().andReturn().getResponse().getStatus());

            semErroDeServidor(dispararJuntas(operacoes));

            int abertas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM coleta.sol_solicitacao
                    WHERE poc_nr_id = ? AND sol_tx_situacao IN ('SOLICITADA', 'AGENDADA', 'EM_RECOLHIMENTO')""", Integer.class, ponto);
            assertTrue(!"INATIVO".equals(situacaoDoPonto()) || abertas == 0,
                    "rodada " + rodada + ": ponto INATIVO com " + abertas + " solicitação(ões) aberta(s)");
        }
    }

    @Test
    void rn05_estresseCriacoesECorrecoesContraRemocaoDeVinculoNuncaDeixamSolicitacaoAbertaComMaterialSemVinculo() throws Exception {
        for (int rodada = 0; rodada < 8; rodada++) {
            reiniciar();
            int existente = CenarioSolicitacao.id(criarSolicitacao(vidro).andReturn().getResponse().getContentAsString());
            List<Callable<Integer>> operacoes = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                operacoes.add(() -> criarSolicitacao(papel).andReturn().getResponse().getStatus());
            }
            operacoes.add(() -> corrigirSolicitacao(existente, item(vidro, "1") + "," + item(papel, "2")).andReturn().getResponse().getStatus());
            operacoes.add(() -> removerVinculo(papel).andReturn().getResponse().getStatus());

            semErroDeServidor(dispararJuntas(operacoes));

            int abertasComPapel = jdbc.queryForObject("""
                    SELECT COUNT(DISTINCT s.sol_nr_id) FROM coleta.sol_solicitacao s
                    JOIN coleta.iso_item_solicitacao i ON i.sol_nr_id = s.sol_nr_id
                    WHERE s.poc_nr_id = ? AND i.mat_nr_id = ?
                      AND s.sol_tx_situacao IN ('SOLICITADA', 'AGENDADA', 'EM_RECOLHIMENTO')""", Integer.class, ponto, papel);
            int vinculoDoPapel = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE poc_nr_id = ? AND mat_nr_id = ?", Integer.class, ponto, papel);
            assertTrue(vinculoDoPapel == 1 || abertasComPapel == 0,
                    "rodada " + rodada + ": vínculo removido com " + abertasComPapel + " solicitação(ões) aberta(s) com o material");
        }
    }

    @Test
    void rn05_estresseMisturandoAsQuatroOperacoesNaoCausaDeadlockNemErroDeServidor() throws Exception {
        for (int rodada = 0; rodada < 6; rodada++) {
            reiniciar();
            int existente = CenarioSolicitacao.id(criarSolicitacao(vidro).andReturn().getResponse().getContentAsString());
            List<Callable<Integer>> operacoes = new ArrayList<>();
            operacoes.add(() -> criarSolicitacao(papel).andReturn().getResponse().getStatus());
            operacoes.add(() -> criarSolicitacao(vidro).andReturn().getResponse().getStatus());
            operacoes.add(() -> corrigirSolicitacao(existente, item(vidro, "3")).andReturn().getResponse().getStatus());
            operacoes.add(() -> corrigirSolicitacao(existente, item(vidro, "4") + "," + item(papel, "1")).andReturn().getResponse().getStatus());
            operacoes.add(() -> inativarPonto().andReturn().getResponse().getStatus());
            operacoes.add(() -> removerVinculo(papel).andReturn().getResponse().getStatus());
            operacoes.add(() -> removerVinculo(vidro).andReturn().getResponse().getStatus());

            // O limite de 30 s em cada futuro faz o teste falhar se alguma operação ficar presa (deadlock ou espera infinita).
            semErroDeServidor(dispararJuntas(operacoes));
        }
    }

    private static java.nio.charset.Charset utf8() {
        return java.nio.charset.StandardCharsets.UTF_8;
    }
}
