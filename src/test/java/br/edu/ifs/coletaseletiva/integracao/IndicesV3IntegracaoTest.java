package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF04 / Issue #33: a migração V3 cria os sete índices de desempenho aprovados, com a tabela, as colunas, a ordem e as
 * expressões esperadas, e não cria os três candidatos descartados (I5, I6 e I8). O teste olha esses nomes específicos e
 * não o conjunto inteiro de índices, então não impede índices legítimos de migrações futuras.
 */
class IndicesV3IntegracaoTest extends IntegracaoBase {

    /** nome do índice, tabela e o trecho da definição depois de "USING btree" (como o PostgreSQL a devolve). */
    private static final List<String[]> APROVADOS = List.of(
            new String[] {"idx_sol_poc_criacao", "sol_solicitacao", "(poc_nr_id, sol_dt_criacao DESC, sol_nr_id DESC)"},
            new String[] {"idx_sol_mor_criacao", "sol_solicitacao", "(mor_nr_id, sol_dt_criacao DESC, sol_nr_id DESC)"},
            new String[] {"idx_sol_criacao", "sol_solicitacao", "(sol_dt_criacao DESC, sol_nr_id DESC)"},
            new String[] {"idx_sol_situacao_criacao", "sol_solicitacao",
                    "(sol_tx_situacao, sol_dt_criacao DESC, sol_nr_id DESC)"},
            new String[] {"idx_his_sol", "his_historico", "(sol_nr_id)"},
            new String[] {"idx_poc_municipio_lower", "poc_ponto_coleta", "(lower((poc_tx_municipio)::text))"},
            new String[] {"idx_poc_bairro_lower", "poc_ponto_coleta", "(lower((poc_tx_bairro)::text))"});

    /** Candidatos avaliados e descartados nesta migração (docs/desempenho/analise-indices-v3.md). */
    private static final List<String> DESCARTADOS = List.of("idx_sol_conclusao", "idx_iso_mat", "idx_mac_mat");

    private Map<String, Object> indice(String nome) {
        List<Map<String, Object>> linhas = jdbc.queryForList(
                "SELECT tablename, indexdef FROM pg_indexes WHERE schemaname = 'coleta' AND indexname = ?", nome);
        assertEquals(1, linhas.size(), "o índice " + nome + " deveria existir");
        return linhas.get(0);
    }

    @Test
    void rnf04_osSeteIndicesAprovadosExistemComTabelaColunasOrdemEExpressoes() {
        for (String[] esperado : APROVADOS) {
            Map<String, Object> linha = indice(esperado[0]);
            assertEquals(esperado[1], linha.get("tablename"), esperado[0] + ": tabela");
            String definicao = (String) linha.get("indexdef");
            assertTrue(definicao.startsWith("CREATE INDEX " + esperado[0] + " ON coleta." + esperado[1] + " USING btree "),
                    esperado[0] + ": deve ser um índice btree comum (não UNIQUE): " + definicao);
            assertTrue(definicao.endsWith(esperado[2]), esperado[0] + ": colunas, ordem e expressão. Veio: " + definicao);
            assertTrue(!definicao.contains(" WHERE "), esperado[0] + ": não deve ser parcial");
        }
    }

    @Test
    void rnf04_osTresCandidatosDescartadosNaoForamCriadosPelaV3() {
        for (String nome : DESCARTADOS) {
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'coleta' AND indexname = ?", Integer.class, nome),
                    "o candidato descartado " + nome + " não deve existir");
        }
    }

    @Test
    void rnf04_aV3VemDepoisDaV1EDaV2NaOrdemDoFlyway() {
        List<Integer> versoes = jdbc.queryForList(
                "SELECT version::int FROM coleta.flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank",
                Integer.class);
        // As três primeiras são a V1, a V2 e a V3, nessa ordem; migrações futuras podem vir depois
        assertTrue(versoes.size() >= 3);
        assertEquals(List.of(1, 2, 3), versoes.subList(0, 3));
    }
}
