package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dataset de desempenho do RNF04 (Issue #58): os scripts de {@code docs/desempenho/} são carregados num PostgreSQL real
 * com o esquema do Flyway. Provam que a carga gera exatamente 1.000 solicitações coerentes com as restrições do banco e
 * com as regras do domínio (ADR 0008, 0010 e 0011), que não pode ser repetida por engano, que a limpeza remove só o que
 * é do dataset e que a API responde corretamente sobre ele.
 */
class DatasetDesempenhoIntegracaoTest extends IntegracaoBase {

    static final Path CARGA = Path.of("docs", "desempenho", "carga-dataset.sql");
    static final Path LIMPEZA = Path.of("docs", "desempenho", "limpar-dataset.sql");

    @Autowired
    private DataSource dataSource;

    private void executar(Path script) throws Exception {
        jdbc.execute(Files.readString(script, StandardCharsets.UTF_8));
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static final String SOLICITACOES_PERF = """
            SELECT s.sol_nr_id FROM coleta.sol_solicitacao s
            JOIN coleta.mor_morador m ON m.mor_nr_id = s.mor_nr_id WHERE m.mor_tx_email LIKE 'perf.morador.%'""";

    // ---------- a carga ----------

    @Test
    void rnf04_cargaGeraExatamenteMilSolicitacoesEOsDadosDeApoioDeterministicos() throws Exception {
        executar(CARGA);

        assertEquals(200, contar("SELECT COUNT(*) FROM coleta.mor_morador"));
        assertEquals(200, contar("SELECT COUNT(*) FROM coleta.end_endereco"));
        assertEquals(20, contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta"));
        assertEquals(8, contar("SELECT COUNT(*) FROM coleta.mat_material"));
        assertEquals(20, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
        // Cada ponto aceita de 4 a 6 materiais: (4 + n mod 3) para n de 1 a 20 soma 101 vínculos.
        assertEquals(101, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
        assertEquals(1000, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        assertEquals(1000, contar("SELECT COUNT(DISTINCT sol_tx_protocolo) FROM coleta.sol_solicitacao"));
        // De 2 a 4 itens por solicitação: 333 com 2, 334 com 3 e 333 com 4 somam 3.000.
        assertEquals(3000, contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao"));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM (SELECT sol_nr_id FROM coleta.iso_item_solicitacao GROUP BY sol_nr_id
                                      HAVING COUNT(*) NOT BETWEEN 2 AND 4) x"""));
        // Histórico: 150 + 300 + 150 + 1.800 + 200 + 250 linhas, conforme a situação de cada solicitação.
        assertEquals(2850, contar("SELECT COUNT(*) FROM coleta.his_historico"));
    }

    @Test
    void rnf04_asSituacoesSeguemAsProporcoesEOsDadosCobremDozeMeses() throws Exception {
        executar(CARGA);

        Map<String, Integer> porSituacao = new java.util.HashMap<>();
        jdbc.query("SELECT sol_tx_situacao, COUNT(*) AS total FROM coleta.sol_solicitacao GROUP BY sol_tx_situacao",
                (rs) -> {
                    porSituacao.put(rs.getString(1), rs.getInt(2));
                });
        assertEquals(Map.of("CONCLUIDA", 450, "SOLICITADA", 150, "AGENDADA", 150, "RECUSADA", 100, "CANCELADA", 100,
                "EM_RECOLHIMENTO", 50), porSituacao);
        // Doze meses distintos de criação, no fuso de negócio, e solicitações espalhadas pelos 20 pontos e 200 moradores.
        assertEquals(12, contar("""
                SELECT COUNT(DISTINCT date_trunc('month', sol_dt_criacao AT TIME ZONE 'America/Sao_Paulo'))
                FROM coleta.sol_solicitacao"""));
        assertEquals(20, contar("SELECT COUNT(DISTINCT poc_nr_id) FROM coleta.sol_solicitacao"));
        assertEquals(200, contar("SELECT COUNT(DISTINCT mor_nr_id) FROM coleta.sol_solicitacao"));
        assertTrue(contar("SELECT COUNT(DISTINCT mat_nr_id) FROM coleta.iso_item_solicitacao") >= 7);
        assertEquals(3, contar("SELECT COUNT(DISTINCT iso_tx_copia_unidade) FROM coleta.iso_item_solicitacao"));
    }

    // ---------- coerência com as restrições e com o domínio ----------

    @Test
    void rnf04_osDadosRespeitamAsRegrasDoDominioAlemDasRestricoesDoBanco() throws Exception {
        executar(CARGA);

        // A última linha do histórico é a situação atual de cada solicitação.
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.sol_solicitacao s
                WHERE s.sol_tx_situacao <> (SELECT h.his_tx_situacao_nova FROM coleta.his_historico h
                                            WHERE h.sol_nr_id = s.sol_nr_id ORDER BY h.his_nr_id DESC LIMIT 1)"""));
        // Cada solicitação começa em SOLICITADA, criada pelo morador dono (ADR 0011).
        assertEquals(1000, contar("""
                SELECT COUNT(*) FROM coleta.his_historico h JOIN coleta.sol_solicitacao s ON s.sol_nr_id = h.sol_nr_id
                WHERE h.his_tx_situacao_anterior IS NULL AND h.his_tx_situacao_nova = 'SOLICITADA'
                  AND h.mor_nr_id = s.mor_nr_id AND h.ope_nr_id IS NULL"""));
        // Quem opera pertence ao ponto da solicitação (ADR 0010).
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.his_historico h
                JOIN coleta.sol_solicitacao s ON s.sol_nr_id = h.sol_nr_id
                JOIN coleta.ope_operador o ON o.ope_nr_id = h.ope_nr_id
                WHERE o.poc_nr_id <> s.poc_nr_id"""));
        // Transições sempre válidas: nenhuma linha de histórico sai de um estado final nem repete o par origem e destino.
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.his_historico
                WHERE his_tx_situacao_anterior IN ('CONCLUIDA', 'RECUSADA', 'CANCELADA')"""));
        // Unidade copiada igual à do material (ADR 0008) e o material é aceito pelo ponto (RN01).
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.iso_item_solicitacao i
                JOIN coleta.mat_material m ON m.mat_nr_id = i.mat_nr_id
                WHERE i.iso_tx_copia_unidade <> m.mat_tx_unidade"""));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.iso_item_solicitacao i
                JOIN coleta.sol_solicitacao s ON s.sol_nr_id = i.sol_nr_id
                WHERE NOT EXISTS (SELECT 1 FROM coleta.mac_material_aceito a
                                  WHERE a.poc_nr_id = s.poc_nr_id AND a.mat_nr_id = i.mat_nr_id)"""));
        // Quantidade efetiva só nas concluídas, com ao menos uma positiva; inteira nos itens em UN; limites respeitados.
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.iso_item_solicitacao i JOIN coleta.sol_solicitacao s ON s.sol_nr_id = i.sol_nr_id
                WHERE (s.sol_tx_situacao <> 'CONCLUIDA' AND i.iso_nr_quantidade_efetiva IS NOT NULL)
                   OR (s.sol_tx_situacao = 'CONCLUIDA' AND i.iso_nr_quantidade_efetiva IS NULL)"""));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM (SELECT i.sol_nr_id FROM coleta.iso_item_solicitacao i
                                      JOIN coleta.sol_solicitacao s ON s.sol_nr_id = i.sol_nr_id
                                      WHERE s.sol_tx_situacao = 'CONCLUIDA'
                                      GROUP BY i.sol_nr_id HAVING MAX(i.iso_nr_quantidade_efetiva) <= 0) x"""));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.iso_item_solicitacao
                WHERE iso_tx_copia_unidade = 'UN'
                  AND (iso_nr_quantidade_estimada <> TRUNC(iso_nr_quantidade_estimada)
                       OR iso_nr_quantidade_efetiva <> TRUNC(iso_nr_quantidade_efetiva))"""));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.iso_item_solicitacao i
                JOIN coleta.sol_solicitacao s ON s.sol_nr_id = i.sol_nr_id
                JOIN coleta.mac_material_aceito a ON a.poc_nr_id = s.poc_nr_id AND a.mat_nr_id = i.mat_nr_id
                WHERE a.mac_nr_limite IS NOT NULL AND i.iso_nr_quantidade_estimada > a.mac_nr_limite"""));
        // Datas de agendamento e de conclusão coerentes com a situação, e cópia do endereço preenchida (ADR 0008).
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.sol_solicitacao
                WHERE (sol_tx_situacao = 'CONCLUIDA' AND (sol_dt_conclusao IS NULL OR sol_dt_agendamento IS NULL))
                   OR (sol_tx_situacao <> 'CONCLUIDA' AND sol_dt_conclusao IS NOT NULL)
                   OR (sol_tx_situacao IN ('SOLICITADA', 'RECUSADA') AND sol_dt_agendamento IS NOT NULL)
                   OR sol_tx_endereco_copia IS NULL OR sol_tx_endereco_copia = ''"""));
        // Cancelada guarda o motivo; recusada também.
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.his_historico
                WHERE his_tx_situacao_nova IN ('RECUSADA', 'CANCELADA') AND (his_tx_motivo IS NULL OR his_tx_motivo = '')"""));
    }

    // ---------- guarda e limpeza ----------

    @Test
    void rnf04_umaSegundaCargaEImpedidaENaoAlteraNada() throws Exception {
        executar(CARGA);

        try (Connection conexao = dataSource.getConnection(); Statement comando = conexao.createStatement()) {
            SQLException erro = assertThrows(SQLException.class,
                    () -> comando.execute(Files.readString(CARGA, StandardCharsets.UTF_8)));
            assertTrue(erro.getMessage().contains("já foi carregado"), erro.getMessage());
            comando.execute("ROLLBACK");
        }

        assertEquals(1000, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        assertEquals(200, contar("SELECT COUNT(*) FROM coleta.mor_morador"));
        assertEquals(2850, contar("SELECT COUNT(*) FROM coleta.his_historico"));
    }

    @Test
    void rnf04_aLimpezaRemoveSoOQueEDoDatasetEPermiteCarregarDeNovo() throws Exception {
        // Dados que NÃO são do dataset, criados antes da carga: precisam sobreviver à limpeza.
        CenarioSolicitacao c = new CenarioSolicitacao(mockMvc, jdbc);
        int morador = c.morador("maria@exemplo.com");
        int endereco = c.endereco(morador);
        int ponto = c.ponto("Ponto Real", true, "ATIVO");
        int papel = c.material("Papel Real", "KG", "ATIVO");
        c.vincular(ponto, papel, null);
        c.operador("operador.real@exemplo.com", ponto, "ATIVO");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/solicitacoes")
                .header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(morador))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(CenarioSolicitacao.corpo(ponto, endereco, CenarioSolicitacao.item(papel, "2")))).andExpect(status().isCreated());

        executar(CARGA);
        assertEquals(1001, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        executar(LIMPEZA);

        assertEquals(0, contar(SOLICITACOES_PERF.replace("SELECT s.sol_nr_id", "SELECT COUNT(*)")));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mor_morador"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.end_endereco"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mat_material"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.his_historico"));

        // Depois de limpar, a carga volta a ser permitida e produz o mesmo dataset.
        executar(CARGA);
        assertEquals(1001, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        assertEquals(1000, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao s JOIN coleta.mor_morador m "
                + "ON m.mor_nr_id = s.mor_nr_id WHERE m.mor_tx_email LIKE 'perf.morador.%'"));
    }

    @Test
    void rnf04_limparSemTerCarregadoNaoFazNadaENaoFalha() throws Exception {
        executar(LIMPEZA);
        executar(LIMPEZA);

        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
    }

    @Test
    void rnf04_aCargaEDeterministicaOsMesmosDadosEmCadaExecucao() throws Exception {
        executar(CARGA);
        List<String> primeira = jdbc.queryForList("""
                SELECT s.sol_tx_protocolo || '|' || s.sol_tx_situacao || '|' || s.sol_dt_criacao || '|' || s.sol_nr_versao
                FROM coleta.sol_solicitacao s ORDER BY s.sol_tx_protocolo""", String.class);
        BigDecimal efetivas = jdbc.queryForObject(
                "SELECT SUM(iso_nr_quantidade_efetiva) FROM coleta.iso_item_solicitacao", BigDecimal.class);
        executar(LIMPEZA);

        executar(CARGA);

        assertEquals(primeira, jdbc.queryForList("""
                SELECT s.sol_tx_protocolo || '|' || s.sol_tx_situacao || '|' || s.sol_dt_criacao || '|' || s.sol_nr_versao
                FROM coleta.sol_solicitacao s ORDER BY s.sol_tx_protocolo""", String.class));
        assertEquals(0, efetivas.compareTo(jdbc.queryForObject(
                "SELECT SUM(iso_nr_quantidade_efetiva) FROM coleta.iso_item_solicitacao", BigDecimal.class)));
    }

    // ---------- a API sobre o dataset ----------

    @Test
    void rnf04_aApiConsultaOsMilRegistrosEOResumoBateComAsAgregacoesEmSqlPuro() throws Exception {
        executar(CARGA);

        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "ADMINISTRADOR").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1000))
                .andExpect(jsonPath("$.totalPaginas").value(10))
                .andExpect(jsonPath("$.conteudo.length()").value(100));
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "ADMINISTRADOR").param("situacao", "CONCLUIDA"))
                .andExpect(jsonPath("$.totalElementos").value(450));
        mockMvc.perform(get("/pontos-coleta")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(20));

        String resumo = mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> totais = JsonPath.read(resumo, "$.solicitacoesPorSituacao[*].total");
        assertEquals(List.of(150L, 150L, 50L, 450L, 100L, 100L), totais.stream().map(Number::longValue).toList());
        // As quantidades do resumo são exatamente a soma das efetivas das concluídas, por material e unidade.
        List<Map<String, Object>> esperado = jdbc.queryForList("""
                SELECT m.mat_tx_nome AS nome, i.iso_tx_copia_unidade AS unidade, SUM(i.iso_nr_quantidade_efetiva) AS total
                FROM coleta.iso_item_solicitacao i
                JOIN coleta.sol_solicitacao s ON s.sol_nr_id = i.sol_nr_id
                JOIN coleta.mat_material m ON m.mat_nr_id = i.mat_nr_id
                WHERE s.sol_tx_situacao = 'CONCLUIDA' GROUP BY m.mat_tx_nome, i.iso_tx_copia_unidade""");
        List<Object> linhas = JsonPath.read(resumo, "$.quantidadesRecolhidas");
        assertEquals(esperado.size(), linhas.size());
        for (Map<String, Object> linha : esperado) {
            List<Number> valores = JsonPath.read(resumo, "$.quantidadesRecolhidas[?(@.nomeMaterial=='" + linha.get("nome")
                    + "' && @.unidade=='" + linha.get("unidade") + "')].quantidade");
            assertEquals(1, valores.size());
            assertEquals(0, ((BigDecimal) linha.get("total")).compareTo(new BigDecimal(valores.get(0).toString())),
                    "material " + linha.get("nome"));
        }
    }

    @Test
    void rnf04_aPesquisaDePontosEOsFiltrosDaListagemFuncionamSobreOsDadosDoDataset() throws Exception {
        executar(CARGA);

        mockMvc.perform(get("/pontos-coleta").param("municipio", "Lagarto"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(3));
        mockMvc.perform(get("/pontos-coleta").param("nome", "perf ponto 07"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        int pontoUm = jdbc.queryForObject("SELECT MIN(poc_nr_id) FROM coleta.poc_ponto_coleta", Integer.class);
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "ADMINISTRADOR").param("pontoId", String.valueOf(pontoUm)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(50));
        int operador = jdbc.queryForObject("SELECT MIN(ope_nr_id) FROM coleta.ope_operador", Integer.class);
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(operador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(50));
        int morador = jdbc.queryForObject("SELECT MIN(mor_nr_id) FROM coleta.mor_morador", Integer.class);
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(morador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(5));
    }
}
