package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.ADMIN;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF08 de ponta a ponta: compatibilidade (RN01/RN02), cópias (RN04), histórico (RN03) e versão (RN06). */
class SolicitacaoIntegracaoTest extends IntegracaoBase {

    private CenarioSolicitacao c;
    private int maria;
    private int enderecoMaria;
    private int ponto;
    private int papel;     // KG, limite 10
    private int vidro;     // KG, sem limite
    private int pilhas;    // UN, sem limite
    private int metal;     // KG, NÃO aceito pelo ponto

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        enderecoMaria = c.endereco(maria);
        ponto = c.ponto("Ponto A", true, "ATIVO");
        papel = c.material("Papel", "KG", "ATIVO");
        vidro = c.material("Vidro", "KG", "ATIVO");
        pilhas = c.material("Pilhas", "UN", "ATIVO");
        metal = c.material("Metal", "KG", "ATIVO");
        c.vincular(ponto, papel, "10");
        c.vincular(ponto, vidro, null);
        c.vincular(ponto, pilhas, null);
    }

    private org.springframework.test.web.servlet.ResultActions criar(int moradorId, String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes").header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(moradorId))
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private int criarValida() throws Exception {
        String resposta = criar(maria, corpo(ponto, enderecoMaria, item(papel, "5.5") + "," + item(vidro, "10")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return CenarioSolicitacao.id(resposta);
    }

    private void naoGravouNada() {
        assertEquals(0, c.contar("sol_solicitacao"));
        assertEquals(0, c.contar("iso_item_solicitacao"));
        assertEquals(0, c.contar("his_historico"));
    }

    // ---------- criação válida ----------

    @Test
    void rf08_solicitacaoValidaGeraProtocoloItensEHistoricoInicial() throws Exception {
        String resposta = criar(maria, corpo(ponto, enderecoMaria, item(papel, "5.5") + "," + item(vidro, "10")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.protocolo").value(org.hamcrest.Matchers.matchesPattern("SOL-\\d{8}-[A-Z2-9]{6}")))
                .andExpect(jsonPath("$.situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.versao").value(0))
                .andExpect(jsonPath("$.moradorId").value(maria))
                .andExpect(jsonPath("$.pontoId").value(ponto))
                .andExpect(jsonPath("$.enderecoCopia").value("Rua das Flores, 120 - Centro - Lagarto/SE - CEP 49400000"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].nomeMaterial").value("Papel"))
                .andExpect(jsonPath("$.itens[0].unidade").value("KG"))
                .andExpect(jsonPath("$.historico.length()").value(1))
                .andExpect(jsonPath("$.historico[0].situacaoNova").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[0].situacaoAnterior").doesNotExist())
                .andExpect(jsonPath("$.historico[0].responsavel.tipo").value("MORADOR"))
                .andExpect(jsonPath("$.historico[0].responsavel.id").value(maria))
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, c.contar("sol_solicitacao"));
        assertEquals(2, c.contar("iso_item_solicitacao"));
        assertEquals(1, c.contar("his_historico"));
        assertTrue(resposta.contains("criadaEm"));
    }

    @Test
    void rf08_doisPedidosGeramProtocolosDiferentes() throws Exception {
        criarValida();
        criarValida();

        assertEquals(2, jdbc.queryForObject("SELECT COUNT(DISTINCT sol_tx_protocolo) FROM coleta.sol_solicitacao", Integer.class));
    }

    @Test
    void rf08_limiteIgualAoCadastradoEhAceitoEPilhasInteirasTambem() throws Exception {
        criar(maria, corpo(ponto, enderecoMaria, item(papel, "10") + "," + item(pilhas, "3.0")))
                .andExpect(status().isCreated());
    }

    // ---------- RN01 e RN02: rejeição sem gravação parcial ----------

    @Test
    void rf08_materialNaoAceitoPeloPontoEhRejeitadoSemGravacaoParcial() throws Exception {
        criar(maria, corpo(ponto, enderecoMaria, item(papel, "1") + "," + item(metal, "2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("O ponto de coleta não aceita o material Metal."));

        naoGravouNada();
    }

    @Test
    void rf08_pontoInativoPontoSemRecolhimentoMaterialInativoERepetidoSaoRejeitados() throws Exception {
        int inativo = c.ponto("Inativo", true, "INATIVO");
        int semDomicilio = c.ponto("Sem domicílio", false, "ATIVO");
        c.vincular(inativo, papel, null);
        c.vincular(semDomicilio, papel, null);
        int isopor = c.material("Isopor", "KG", "INATIVO");
        c.vincular(ponto, isopor, null);

        criar(maria, corpo(inativo, enderecoMaria, item(papel, "1"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O ponto de coleta está inativo."));
        criar(maria, corpo(semDomicilio, enderecoMaria, item(papel, "1"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O ponto de coleta não oferece recolhimento domiciliar."));
        criar(maria, corpo(ponto, enderecoMaria, item(isopor, "1"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O material Isopor está inativo."));
        criar(maria, corpo(ponto, enderecoMaria, item(papel, "1") + "," + item(papel, "2"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));

        naoGravouNada();
    }

    @Test
    void rf08_limiteExcedidoEQuantidadeDecimalEmPecaSaoRejeitados() throws Exception {
        criar(maria, corpo(ponto, enderecoMaria, item(papel, "10.001"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A quantidade do material Papel excede o limite de 10 por solicitação."));
        criar(maria, corpo(ponto, enderecoMaria, item(pilhas, "2.5"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A quantidade do material Pilhas deve ser um número inteiro."));

        naoGravouNada();
    }

    @Test
    void i18n_violacaoDaRn01SaiEmInglesComAcceptLanguage() throws Exception {
        criar(maria, corpo(ponto, enderecoMaria, item(metal, "2")), "en")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("The collection point does not accept the material Metal."));
    }

    // ---------- 400, 404 e permissões ----------

    @Test
    void rf08_dadosInvalidosRetornam400() throws Exception {
        criar(maria, "{\"pontoId\": " + ponto + ", \"enderecoId\": " + enderecoMaria + ", \"itens\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens')].mensagem").value("informe pelo menos um item"));
        criar(maria, corpo(ponto, enderecoMaria, item(papel, "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEstimada')]").exists());
        criar(maria, "{\"itens\": [" + item(papel, "1") + "]}")
                .andExpect(status().isBadRequest());

        naoGravouNada();
    }

    @Test
    void rf08_pontoMaterialOuEnderecoInexistentesOuDeOutroMoradorRetornam404() throws Exception {
        int joao = c.morador("joao@exemplo.com");
        int enderecoJoao = c.endereco(joao);

        criar(maria, corpo(999999, enderecoMaria, item(papel, "1"))).andExpect(status().isNotFound());
        criar(maria, corpo(ponto, enderecoMaria, item(999999, "1"))).andExpect(status().isNotFound());
        criar(maria, corpo(ponto, enderecoJoao, item(papel, "1"))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));

        naoGravouNada();
    }

    @Test
    void rf08_soOMoradorAtivoCriaParaSiAdministradorEOperadorRecebem403() throws Exception {
        int operador = c.operador("op@exemplo.com", ponto, "ATIVO");
        String corpo = corpo(ponto, enderecoMaria, item(papel, "1"));

        mockMvc.perform(post("/solicitacoes").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        mockMvc.perform(post("/solicitacoes").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(operador))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
        jdbc.update("UPDATE coleta.mor_morador SET mor_tx_situacao = 'INATIVO' WHERE mor_nr_id = ?", maria);
        criar(maria, corpo).andExpect(status().isForbidden());

        naoGravouNada();
    }

    @Test
    void rf08_enderecoQueNaoCabeNaCopiaDe400CaracteresEhRecusadoEmVezDeTruncado() throws Exception {
        int enderecoLongo = c.endereco(maria, "L".repeat(150), "C".repeat(100));
        jdbc.update("UPDATE coleta.end_endereco SET end_tx_bairro = ?, end_tx_municipio = ? WHERE end_nr_id = ?",
                "B".repeat(100), "M".repeat(100), enderecoLongo);

        criar(maria, corpo(ponto, enderecoLongo, item(papel, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("excede 400 caracteres")));

        naoGravouNada();
    }

    // ---------- RN04: cópia preservada ----------

    @Test
    void rf08_mudarOEnderecoDepoisNaoAlteraACopiaDaSolicitacao() throws Exception {
        int id = criarValida();

        mockMvc.perform(put("/moradores/" + maria + "/enderecos/" + enderecoMaria).header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logradouro\": \"Rua Nova\", \"numero\": \"9\", \"bairro\": \"Outro\", \"municipio\": \"Aracaju\", "
                                + "\"uf\": \"SE\", \"cep\": \"49000000\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(jsonPath("$.enderecoCopia").value("Rua das Flores, 120 - Centro - Lagarto/SE - CEP 49400000"));
    }

    // ---------- consulta por id ----------

    @Test
    void rf08_consultaPorIdRespeitaOContextoDoAtor() throws Exception {
        int id = criarValida();
        int joao = c.morador("joao@exemplo.com");
        int outroPonto = c.ponto("Ponto B", true, "ATIVO");
        int opDoPonto = c.operador("op1@exemplo.com", ponto, "ATIVO");
        int opOutroPonto = c.operador("op2@exemplo.com", outroPonto, "ATIVO");
        int opInativo = c.operador("op3@exemplo.com", ponto, "INATIVO");

        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", ADMIN)).andExpect(status().isOk());
        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(opDoPonto)))
                .andExpect(status().isOk());
        // fora do contexto: 404, sem revelar que existe
        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(joao)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(opOutroPonto)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/solicitacoes/" + id).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(opInativo)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/solicitacoes/999999").header("X-Perfil", ADMIN)).andExpect(status().isNotFound());
        mockMvc.perform(get("/solicitacoes/" + id)).andExpect(status().isForbidden());
    }

    // ---------- correção (RN04) e versão (RN06) ----------

    private String correcao(int enderecoId, String itens, int versao) {
        return "{\"enderecoId\": %d, \"observacoes\": \"Corrigido\", \"itens\": [%s], \"versao\": %d}"
                .formatted(enderecoId, itens, versao);
    }

    private org.springframework.test.web.servlet.ResultActions corrigir(int id, int moradorId, String corpo) throws Exception {
        return mockMvc.perform(put("/solicitacoes/" + id).header("X-Perfil", "MORADOR")
                .header("X-Usuario-Id", String.valueOf(moradorId))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void rf08_corrigirTrocaItensEEnderecoESomaUmaVersao() throws Exception {
        int id = criarValida();
        int novoEndereco = c.endereco(maria, "Avenida Nova", null);

        corrigir(id, maria, correcao(novoEndereco, item(papel, "7") + "," + item(pilhas, "4"), 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.observacoes").value("Corrigido"))
                .andExpect(jsonPath("$.enderecoCopia").value("Avenida Nova, 120 - Centro - Lagarto/SE - CEP 49400000"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].nomeMaterial").value("Papel"))
                .andExpect(jsonPath("$.itens[0].quantidadeEstimada").value(7.0))
                .andExpect(jsonPath("$.itens[1].nomeMaterial").value("Pilhas"));

        assertEquals(2, c.contar("iso_item_solicitacao"));   // vidro saiu, pilhas entrou
        assertEquals(1, c.contar("his_historico"));            // correção não é mudança de situação
    }

    @Test
    void rf08_corrigirSoAQuantidadeDeUmItemTambemSomaExatamenteUmaVersao() throws Exception {
        int id = criarValida();
        String semMudarEnderecoNemObservacoes = "{\"enderecoId\": %d, \"observacoes\": \"Caixas na calçada\", \"itens\": [%s], \"versao\": 0}"
                .formatted(enderecoMaria, item(papel, "9") + "," + item(vidro, "10"));

        corrigir(id, maria, semMudarEnderecoNemObservacoes)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.itens[0].quantidadeEstimada").value(9.0));
        corrigir(id, maria, semMudarEnderecoNemObservacoes)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }

    @Test
    void rf08_versaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda() throws Exception {
        int id = criarValida();
        corrigir(id, maria, correcao(enderecoMaria, item(papel, "7"), 0)).andExpect(status().isOk());

        corrigir(id, maria, correcao(enderecoMaria, item(vidro, "3"), 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM coleta.iso_item_solicitacao WHERE mat_nr_id = ?",
                Integer.class, papel));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM coleta.iso_item_solicitacao WHERE mat_nr_id = ?",
                Integer.class, vidro));
    }

    @Test
    void rf08_correcaoRevalidaARn01ENaoGravaQuandoFalha() throws Exception {
        int id = criarValida();

        corrigir(id, maria, correcao(enderecoMaria, item(metal, "1"), 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));

        assertEquals(2, c.contar("iso_item_solicitacao"));
        assertEquals(0, jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao", Integer.class));
    }

    @Test
    void rf08_soCorrigeEmSolicitadaEApenasODono() throws Exception {
        int id = criarValida();
        int joao = c.morador("joao@exemplo.com");

        corrigir(id, joao, correcao(enderecoMaria, item(papel, "1"), 0)).andExpect(status().isNotFound());
        mockMvc.perform(put("/solicitacoes/" + id).header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(correcao(enderecoMaria, item(papel, "1"), 0)))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = 'AGENDADA' WHERE sol_nr_id = ?", id);
        corrigir(id, maria, correcao(enderecoMaria, item(papel, "1"), 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação só pode ser corrigida enquanto estiver SOLICITADA."));
    }

    @Test
    void rf08_duasCorrecoesSimultaneasNaMesmaVersaoSomenteUmaVence() throws Exception {
        int id = criarValida();
        int tentativas = 4;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            String quantidade = String.valueOf(i + 1);
            Callable<Integer> tentativa = () -> {
                largada.await();
                return corrigir(id, maria, correcao(enderecoMaria, item(papel, quantidade), 0))
                        .andReturn().getResponse().getStatus();
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

        assertEquals(1, sucessos, "só uma correção pode usar a versão 0");
        assertEquals(tentativas - 1, conflitos, "as demais recebem 409, nunca 500");
        assertEquals(1, jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao", Integer.class));
    }
}
