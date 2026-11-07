package br.edu.ifs.coletaseletiva.integracao;

import br.edu.ifs.coletaseletiva.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base dos testes de integração: sobe a aplicação inteira contra um PostgreSQL 17 real
 * (Testcontainers), com o esquema criado pelo Flyway. Todas as subclasses compartilham
 * a mesma configuração, então o Spring reaproveita um único contêiner.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class IntegracaoBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Cada teste começa com a tabela de moradores vazia. */
    @BeforeEach
    void limparMoradores() {
        jdbc.update("DELETE FROM coleta.end_endereco");
        jdbc.update("DELETE FROM coleta.mor_morador");
    }
}
