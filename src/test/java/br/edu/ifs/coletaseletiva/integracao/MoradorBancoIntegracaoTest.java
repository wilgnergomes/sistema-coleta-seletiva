package br.edu.ifs.coletaseletiva.integracao;

import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras que o próprio banco garante, mesmo que a aplicação falhe ou seja contornada.
 * Os testes falam direto com o repositório ou com SQL, sem passar pelo service.
 */
class MoradorBancoIntegracaoTest extends IntegracaoBase {

    private static final String INSERIR = """
            INSERT INTO coleta.mor_morador (mor_tx_nome, mor_tx_email, mor_tx_situacao)
            VALUES ('Maria', ?, ?)
            """;

    @Autowired
    private MoradorRepository repository;

    // ---------- mapeamento e consultas ----------

    @Test
    void rf01_salvarEBuscarPreservaOsCamposESituacaoPadraoEhAtivo() {
        Morador salvo = repository.saveAndFlush(Morador.builder()
                .nome("Maria").email("maria@exemplo.com").fone("79999990000").build());

        Morador lido = repository.findById(salvo.getId()).orElseThrow();

        assertEquals("Maria", lido.getNome());
        assertEquals("maria@exemplo.com", lido.getEmail());
        assertEquals("79999990000", lido.getFone());
        assertEquals(SituacaoCadastro.ATIVO, lido.getSituacao());
    }

    @Test
    void rf01_consultasDeEmailRepetidoFuncionamNoBanco() {
        Morador maria = repository.saveAndFlush(
                Morador.builder().nome("Maria").email("maria@exemplo.com").build());

        assertTrue(repository.existsByEmail("maria@exemplo.com"));
        assertFalse(repository.existsByEmail("outro@exemplo.com"));
        // O próprio morador não conta como duplicado de si mesmo; outro id, sim.
        assertFalse(repository.existsByEmailAndIdNot("maria@exemplo.com", maria.getId()));
        assertTrue(repository.existsByEmailAndIdNot("maria@exemplo.com", maria.getId() + 1));
    }

    // ---------- UNIQUE ----------

    @Test
    void rf01_bancoRejeitaDoisMoradoresComOMesmoEmail() {
        repository.saveAndFlush(Morador.builder().nome("Maria").email("maria@exemplo.com").build());
        Morador duplicado = Morador.builder().nome("Outra").email("maria@exemplo.com").build();

        DataIntegrityViolationException erro =
                assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));

        assertTrue(erro.getMostSpecificCause().getMessage().contains("uq_mor_email"));
    }

    // ---------- CHECK de e-mail normalizado ----------

    @Test
    void rf01_bancoRejeitaEmailComMaiusculas() {
        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERIR, "Maria@Exemplo.com", "ATIVO"));

        assertTrue(erro.getMostSpecificCause().getMessage().contains("ck_mor_email_normalizado"));
    }

    @Test
    void rf01_bancoRejeitaEmailComEspacosNasPontas() {
        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERIR, " maria@exemplo.com ", "ATIVO"));

        assertTrue(erro.getMostSpecificCause().getMessage().contains("ck_mor_email_normalizado"));
    }

    @Test
    void rf01_entidadeNaoNormalizaSozinha_ABarreiraDoBancoProtegeContraOEsquecimentoDoService() {
        Morador semNormalizar = Morador.builder().nome("Maria").email("Maria@Exemplo.com").build();

        DataIntegrityViolationException erro =
                assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(semNormalizar));

        assertTrue(erro.getMostSpecificCause().getMessage().contains("ck_mor_email_normalizado"));
    }

    // ---------- CHECK de situação ----------

    @Test
    void rf01_bancoRejeitaSituacaoForaDeAtivoEInativo() {
        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERIR, "maria@exemplo.com", "SUSPENSO"));

        assertTrue(erro.getMostSpecificCause().getMessage().contains("ck_mor_situacao"));
    }

    // ---------- exclusão física (ON DELETE RESTRICT) ----------

    @Test
    void rf01_bancoImpedeExcluirMoradorQueTemEndereco() {
        jdbc.update(INSERIR, "maria@exemplo.com", "ATIVO");
        Integer id = jdbc.queryForObject(
                "SELECT mor_nr_id FROM coleta.mor_morador WHERE mor_tx_email = 'maria@exemplo.com'", Integer.class);
        jdbc.update("""
                INSERT INTO coleta.end_endereco
                    (mor_nr_id, end_tx_logradouro, end_tx_numero, end_tx_bairro,
                     end_tx_municipio, end_tx_uf, end_tx_cep)
                VALUES (?, 'Rua das Flores', '10', 'Centro', 'Aracaju', 'SE', '49000000')
                """, id);

        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM coleta.mor_morador WHERE mor_nr_id = ?", id));

        assertTrue(erro.getMostSpecificCause().getMessage().contains("fk_end_mor"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM coleta.mor_morador", Integer.class));
    }
}
