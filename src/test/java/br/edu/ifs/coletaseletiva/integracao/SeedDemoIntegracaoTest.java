package br.edu.ifs.coletaseletiva.integracao;

import br.edu.ifs.coletaseletiva.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida o próprio seed do perfil "demo" (ADR 0022). É a única classe de teste que ativa o perfil;
 * todos os outros testes rodam sem seed e criam os próprios dados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class SeedDemoIntegracaoTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rf01_perfilDemoCarregaCincoMoradoresFicticiosENormalizados() {
        assertEquals(5, contar("SELECT COUNT(*) FROM coleta.mor_morador"));
        assertEquals(5, contar("SELECT COUNT(DISTINCT mor_tx_email) FROM coleta.mor_morador"));
        assertEquals(0, contar(
                "SELECT COUNT(*) FROM coleta.mor_morador WHERE mor_tx_email <> LOWER(BTRIM(mor_tx_email))"));
        assertEquals(4, contar("SELECT COUNT(*) FROM coleta.mor_morador WHERE mor_tx_situacao = 'ATIVO'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mor_morador WHERE mor_tx_situacao = 'INATIVO'"));
    }

    @Test
    void rf01_executarOSeedDeNovoNaoDuplicaNemConsomeIdentidade() {
        List<Integer> antes = jdbc.queryForList(
                "SELECT mor_nr_id FROM coleta.mor_morador ORDER BY mor_nr_id", Integer.class);

        flyway.migrate();
        flyway.migrate();

        List<Integer> depois = jdbc.queryForList(
                "SELECT mor_nr_id FROM coleta.mor_morador ORDER BY mor_nr_id", Integer.class);
        assertEquals(antes, depois);

        // Se a reexecução tivesse consumido a identidade, o próximo id não seguiria o último.
        Integer proximo = jdbc.queryForObject("""
                INSERT INTO coleta.mor_morador (mor_tx_nome, mor_tx_email)
                VALUES ('Teste', 'teste.identidade@exemplo.com') RETURNING mor_nr_id
                """, Integer.class);
        assertEquals(antes.get(antes.size() - 1) + 1, proximo);
        jdbc.update("DELETE FROM coleta.mor_morador WHERE mor_nr_id = ?", proximo);
    }

    @Test
    void rf02_seedTemEnderecosParaOsMoradoresComSemNumeroEComplementoAusente() {
        assertEquals(7, contar("SELECT COUNT(*) FROM coleta.end_endereco"));
        assertEquals(2, contar("SELECT COUNT(*) FROM coleta.end_endereco WHERE end_tx_numero = 'S/N'"));
        assertTrue(contar("SELECT COUNT(*) FROM coleta.end_endereco WHERE end_tx_complemento IS NULL") > 0);
        assertEquals(2, contar("""
                SELECT COUNT(*) FROM coleta.end_endereco e JOIN coleta.mor_morador m ON m.mor_nr_id = e.mor_nr_id
                WHERE m.mor_tx_email = 'maria.silva@exemplo.com'"""));
        // Todo morador do seed tem pelo menos um endereço
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.mor_morador m
                WHERE NOT EXISTS (SELECT 1 FROM coleta.end_endereco e WHERE e.mor_nr_id = m.mor_nr_id)"""));
    }

    @Test
    void rf02_reexecutarOSeedNaoDuplicaEnderecos() {
        int antes = contar("SELECT COUNT(*) FROM coleta.end_endereco");

        flyway.migrate();

        assertEquals(antes, contar("SELECT COUNT(*) FROM coleta.end_endereco"));
    }

    @Test
    void rf03_seedTemMateriaisEmKgLEUnComUmInativoEReexecucaoNaoDuplica() {
        assertEquals(7, contar("SELECT COUNT(*) FROM coleta.mat_material"));
        assertEquals(5, contar("SELECT COUNT(*) FROM coleta.mat_material WHERE mat_tx_unidade = 'KG'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mat_material WHERE mat_tx_unidade = 'L'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mat_material WHERE mat_tx_unidade = 'UN'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mat_material WHERE mat_tx_situacao = 'INATIVO'"));
        assertEquals(4, contar("""
                SELECT COUNT(*) FROM coleta.mat_material
                WHERE LOWER(mat_tx_nome) IN ('papel', 'plástico', 'vidro', 'metal') AND mat_tx_unidade = 'KG'"""));

        flyway.migrate();

        assertEquals(7, contar("SELECT COUNT(*) FROM coleta.mat_material"));
    }

    @Test
    void rf04_seedTemPontosDeMunicipiosDiferentesComInativoESemContato() {
        assertEquals(5, contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta"));
        assertTrue(contar("SELECT COUNT(DISTINCT poc_tx_municipio) FROM coleta.poc_ponto_coleta") >= 4);
        assertTrue(contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta WHERE poc_bl_rec_domiciliar") > 0);
        assertTrue(contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta WHERE NOT poc_bl_rec_domiciliar") > 0);
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta WHERE poc_tx_situacao = 'INATIVO'"));
        assertEquals(1, contar("""
                SELECT COUNT(*) FROM coleta.poc_ponto_coleta
                WHERE poc_tx_contato IS NULL AND poc_tx_horarios IS NULL"""));

        flyway.migrate();

        assertEquals(5, contar("SELECT COUNT(*) FROM coleta.poc_ponto_coleta"));
    }

    @Test
    void rf05_seedTemVinculosComESemLimiteEPontosQueNaoAceitamVidro() {
        assertEquals(14, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
        assertTrue(contar("SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE mac_nr_limite IS NULL") > 0);
        assertTrue(contar("SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE mac_nr_limite IS NOT NULL") > 0);
        assertTrue(contar("SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE mac_tx_observacoes IS NOT NULL") > 0);
        // Pontos ativos que NÃO aceitam vidro (cenário de material incompatível)
        assertEquals(2, contar("""
                SELECT COUNT(*) FROM coleta.poc_ponto_coleta p
                WHERE p.poc_tx_situacao = 'ATIVO' AND NOT EXISTS (
                    SELECT 1 FROM coleta.mac_material_aceito a JOIN coleta.mat_material m ON m.mat_nr_id = a.mat_nr_id
                    WHERE a.poc_nr_id = p.poc_nr_id AND m.mat_tx_nome = 'Vidro')"""));

        flyway.migrate();

        assertEquals(14, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
    }

    @Test
    void rf07_seedTemDoisOperadoresNoMesmoPontoEUmEmOutroComEmailNormalizado() {
        assertEquals(3, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
        assertEquals(2, contar("""
                SELECT MAX(qtd) FROM (SELECT COUNT(*) AS qtd FROM coleta.ope_operador GROUP BY poc_nr_id) t"""));
        assertEquals(2, contar("SELECT COUNT(DISTINCT poc_nr_id) FROM coleta.ope_operador"));
        assertEquals(0, contar(
                "SELECT COUNT(*) FROM coleta.ope_operador WHERE ope_tx_email <> LOWER(BTRIM(ope_tx_email))"));

        flyway.migrate();

        assertEquals(3, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
    }

    @Test
    void rf01_oSeedNaoApareceNoHistoricoDoFlyway() {
        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.flyway_schema_history WHERE description ILIKE '%morador%'"));
        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.flyway_schema_history WHERE description ILIKE '%endere%'"));
    }

    @Test
    void rf01_moradoresDoSeedServemDeAtoresConhecidos() throws Exception {
        int ativo = idPorEmail("maria.silva@exemplo.com");
        int inativo = idPorEmail("beatriz.rocha@exemplo.com");

        mockMvc.perform(get("/moradores/" + ativo)
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(ativo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria.silva@exemplo.com"));

        mockMvc.perform(get("/moradores/" + inativo)
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(inativo)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf09_seedTemUmaSolicitacaoEmCadaSituacaoComItensEHistoricoCoerentes() {
        assertEquals(11, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        assertEquals(6, contar("SELECT COUNT(DISTINCT sol_tx_situacao) FROM coleta.sol_solicitacao"));
        assertEquals(20, contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao"));
        // A última linha do histórico de cada solicitação é a sua situação atual.
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.sol_solicitacao s
                WHERE s.sol_tx_situacao <> (SELECT h.his_tx_situacao_nova FROM coleta.his_historico h
                                            WHERE h.sol_nr_id = s.sol_nr_id ORDER BY h.his_nr_id DESC LIMIT 1)"""));
        // Quem opera pertence ao ponto da solicitação (ADR 0010).
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.his_historico h
                JOIN coleta.sol_solicitacao s ON s.sol_nr_id = h.sol_nr_id
                JOIN coleta.ope_operador o ON o.ope_nr_id = h.ope_nr_id
                WHERE o.poc_nr_id <> s.poc_nr_id"""));
        // Só a solicitação concluída tem quantidade efetiva.
        assertEquals(9, contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao WHERE iso_nr_quantidade_efetiva IS NOT NULL"));
    }

    @Test
    void rf09_executarOSeedDeNovoNaoDuplicaSolicitacoesItensNemHistorico() {
        int historico = contar("SELECT COUNT(*) FROM coleta.his_historico");

        flyway.migrate();

        assertEquals(11, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao"));
        assertEquals(20, contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao"));
        assertEquals(historico, contar("SELECT COUNT(*) FROM coleta.his_historico"));
    }

    @Test
    void rf09_atoresDoSeedConsultamSomenteOQueOContextoPermite() throws Exception {
        int maria = idPorEmail("maria.silva@exemplo.com");
        int carla = jdbc.queryForObject(
                "SELECT ope_nr_id FROM coleta.ope_operador WHERE ope_tx_email = 'carla.nunes@exemplo.com'", Integer.class);
        int rita = jdbc.queryForObject(
                "SELECT ope_nr_id FROM coleta.ope_operador WHERE ope_tx_email = 'rita.alves@exemplo.com'", Integer.class);

        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(11));
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(4));
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(carla)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(9));
        // O operador do Ecoponto Centro (sem recolhimento domiciliar) não tem solicitações.
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(rita)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "ADMINISTRADOR").param("situacao", "CONCLUIDA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(4));
        mockMvc.perform(get("/solicitacoes").header("X-Perfil", "ADMINISTRADOR")
                        .param("de", "2026-11-01").param("ate", "2026-11-30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2));
    }

    // ---------- RF15: seed 080 e o resumo gerencial do perfil demo ----------

    @Test
    void rf15_seedTemTresConcluidasAcrescentadasEUmVinculoQueTornaPossivelUnidadeDePecaEmPontoDomiciliar() {
        assertEquals(4, contar("SELECT COUNT(*) FROM coleta.sol_solicitacao WHERE sol_tx_situacao = 'CONCLUIDA'"));
        assertEquals(3, contar("""
                SELECT COUNT(*) FROM coleta.sol_solicitacao
                WHERE sol_tx_protocolo IN ('SOL-20260915-SEEDJ2', 'SOL-20261008-SEEDK3', 'SOL-20261030-SEEDM5')"""));
        // Uma concluída por mês (setembro, outubro e novembro) entre as acrescentadas pelo seed 080.
        assertEquals(3, contar("""
                SELECT COUNT(DISTINCT EXTRACT(MONTH FROM sol_dt_conclusao AT TIME ZONE 'America/Sao_Paulo'))
                FROM coleta.sol_solicitacao
                WHERE sol_tx_protocolo IN ('SOL-20260915-SEEDJ2', 'SOL-20261008-SEEDK3', 'SOL-20261030-SEEDM5')"""));
        // A estimada difere da efetiva em ao menos um item e há um item não recolhido (efetiva 0).
        assertTrue(contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao "
                + "WHERE iso_nr_quantidade_efetiva <> iso_nr_quantidade_estimada") >= 3);
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.iso_item_solicitacao WHERE iso_nr_quantidade_efetiva = 0"));
        // O vínculo novo existe, e toda solicitação do seed respeita a RN01: ponto ativo, domiciliar e material aceito.
        assertEquals(1, contar("""
                SELECT COUNT(*) FROM coleta.mac_material_aceito a
                JOIN coleta.poc_ponto_coleta p ON p.poc_nr_id = a.poc_nr_id
                JOIN coleta.mat_material m ON m.mat_nr_id = a.mat_nr_id
                WHERE p.poc_tx_nome = 'Cooperativa Verde Vida' AND m.mat_tx_nome = 'Pilhas e baterias'"""));
        assertEquals(0, contar("""
                SELECT COUNT(*) FROM coleta.iso_item_solicitacao i
                JOIN coleta.sol_solicitacao s ON s.sol_nr_id = i.sol_nr_id
                WHERE NOT EXISTS (SELECT 1 FROM coleta.mac_material_aceito a
                                  WHERE a.poc_nr_id = s.poc_nr_id AND a.mat_nr_id = i.mat_nr_id)"""));
    }

    @Test
    void rf15_resumoGerencialDoPerfilDemoSomaSoAsEfetivasDasConcluidasSemMisturarUnidades() throws Exception {
        String json = mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Papel 17,5 (seed 070) + 18,5 + 40; Vidro 10 + 0; Plástico 7,25; Metal 5; Pilhas e baterias 10 + 6 em UN.
        assertEquals(0, new java.math.BigDecimal("76").compareTo(quantidade(json, "Papel", "KG")));
        assertEquals(0, new java.math.BigDecimal("10").compareTo(quantidade(json, "Vidro", "KG")));
        assertEquals(0, new java.math.BigDecimal("7.25").compareTo(quantidade(json, "Plástico", "KG")));
        assertEquals(0, new java.math.BigDecimal("5").compareTo(quantidade(json, "Metal", "KG")));
        assertEquals(0, new java.math.BigDecimal("16").compareTo(quantidade(json, "Pilhas e baterias", "UN")));
        assertEquals(5, ((java.util.List<?>) com.jayway.jsonpath.JsonPath.read(json, "$.quantidadesRecolhidas")).size());
        // Nenhuma linha mistura unidades: o material em UN não aparece em KG, e os de KG não aparecem em UN.
        assertEquals(0, ((java.util.List<?>) com.jayway.jsonpath.JsonPath.read(json,
                "$.quantidadesRecolhidas[?(@.nomeMaterial=='Pilhas e baterias' && @.unidade=='KG')]")).size());
    }

    @Test
    void rf15_resumoGerencialDoPerfilDemoContaTodasAsSituacoesEFiltraPorMes() throws Exception {
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao.length()").value(6))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[0].total").value(3))   // SOLICITADA
                .andExpect(jsonPath("$.solicitacoesPorSituacao[1].total").value(1))   // AGENDADA
                .andExpect(jsonPath("$.solicitacoesPorSituacao[2].total").value(1))   // EM_RECOLHIMENTO
                .andExpect(jsonPath("$.solicitacoesPorSituacao[3].total").value(4))   // CONCLUIDA
                .andExpect(jsonPath("$.solicitacoesPorSituacao[4].total").value(1))   // RECUSADA
                .andExpect(jsonPath("$.solicitacoesPorSituacao[5].total").value(1));  // CANCELADA

        // Setembro, pela data de conclusão: só a SEEDJ2 (17/09). A SEEDD5 foi concluída em 02/10.
        String setembro = mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR")
                        .param("de", "2026-09-01").param("ate", "2026-09-30"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(0, new java.math.BigDecimal("18.5").compareTo(quantidade(setembro, "Papel", "KG")));
        assertEquals(0, new java.math.BigDecimal("10").compareTo(quantidade(setembro, "Pilhas e baterias", "UN")));
        assertEquals(2, ((java.util.List<?>) com.jayway.jsonpath.JsonPath.read(setembro, "$.quantidadesRecolhidas")).size());
    }

    // ---------- RN05: roteiro de demonstração com o perfil demo ----------

    @Test
    void rn05_noPerfilDemoNaoSeInativaOPontoNemSeRemoveOVinculoComSolicitacoesAbertas() throws Exception {
        int verdeVida = jdbc.queryForObject(
                "SELECT poc_nr_id FROM coleta.poc_ponto_coleta WHERE poc_tx_nome = 'Cooperativa Verde Vida'", Integer.class);
        int vidro = jdbc.queryForObject("SELECT mat_nr_id FROM coleta.mat_material WHERE mat_tx_nome = 'Vidro'", Integer.class);

        // A Cooperativa Verde Vida tem 3 abertas no seed 070 (SOLICITADA, AGENDADA e EM_RECOLHIMENTO).
        mockMvc.perform(put("/pontos-coleta/" + verdeVida).header("X-Perfil", "ADMINISTRADOR")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Cooperativa Verde Vida\", \"recolhimentoDomiciliar\": true, \"situacao\": \"INATIVO\", "
                                + "\"endereco\": {\"logradouro\": \"Avenida Principal\", \"numero\": \"S/N\", \"bairro\": \"Distrito Industrial\", "
                                + "\"municipio\": \"Lagarto\", \"uf\": \"SE\", \"cep\": \"49400000\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não é possível inativar o ponto: ele possui 3 solicitações abertas. "
                        + "Recuse ou cancele os atendimentos antes de inativá-lo."));
        // O vidro é pedido em duas abertas (a AGENDADA e a EM_RECOLHIMENTO); a concluída com vidro não conta.
        mockMvc.perform(delete("/pontos-coleta/" + verdeVida + "/materiais/" + vidro).header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não é possível remover o vínculo do material Vidro: o ponto possui "
                        + "2 solicitações abertas com esse material. Recuse ou cancele os atendimentos antes de remover o vínculo."));

        assertEquals("ATIVO", jdbc.queryForObject(
                "SELECT poc_tx_situacao FROM coleta.poc_ponto_coleta WHERE poc_nr_id = ?", String.class, verdeVida));
        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE poc_nr_id = " + verdeVida
                + " AND mat_nr_id = " + vidro));
    }

    private java.math.BigDecimal quantidade(String json, String material, String unidade) {
        java.util.List<Number> valores = com.jayway.jsonpath.JsonPath.read(json,
                "$.quantidadesRecolhidas[?(@.nomeMaterial=='" + material + "' && @.unidade=='" + unidade + "')].quantidade");
        assertEquals(1, valores.size(), material + " em " + unidade);
        return new java.math.BigDecimal(valores.get(0).toString());
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private int idPorEmail(String email) {
        return jdbc.queryForObject(
                "SELECT mor_nr_id FROM coleta.mor_morador WHERE mor_tx_email = ?", Integer.class, email);
    }
}
