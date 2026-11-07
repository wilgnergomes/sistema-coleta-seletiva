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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void rf01_oSeedNaoApareceNoHistoricoDoFlyway() {
        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.flyway_schema_history WHERE description ILIKE '%morador%'"));
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

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private int idPorEmail(String email) {
        return jdbc.queryForObject(
                "SELECT mor_nr_id FROM coleta.mor_morador WHERE mor_tx_email = ?", Integer.class, email);
    }
}
