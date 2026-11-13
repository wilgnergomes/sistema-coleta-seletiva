package br.edu.ifs.coletaseletiva;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ColetaSeletivaApplicationTests {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	void semPerfilDemoOBancoNaoRecebeDadosDeSeed() {
		Integer moradores = jdbc.queryForObject("SELECT COUNT(*) FROM coleta.mor_morador", Integer.class);
		assertEquals(0, moradores);
	}

}
