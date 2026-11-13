package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF04 de ponta a ponta, com PostgreSQL real: o endereço embutido (@Embedded) e as restrições do banco. */
class PontoColetaIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    private static String corpo(String nome, String contato, String situacao) {
        String c = contato == null ? "" : ", \"contato\": \"" + contato + "\"";
        String s = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        return ("{\"nome\": \"%s\"%s%s, \"endereco\": {\"logradouro\": \"Avenida Principal\", \"numero\": \"s/n\", "
                + "\"bairro\": \"Centro\", \"municipio\": \"Lagarto\", \"uf\": \"SE\", \"cep\": \"49400000\"}}")
                .formatted(nome, c, s);
    }

    @Test
    void rf04_postDepoisGetPersisteTodosOsCamposInclusiveOEnderecoEmbutido() throws Exception {
        int id = novoPonto(corpo("  Ecoponto ", "(79) 3333-0000", null));

        mockMvc.perform(get("/pontos-coleta/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ecoponto"))
                .andExpect(jsonPath("$.contato").value("(79) 3333-0000"))
                .andExpect(jsonPath("$.recolhimentoDomiciliar").value(false))
                .andExpect(jsonPath("$.situacao").value("ATIVO"))
                .andExpect(jsonPath("$.endereco.numero").value("S/N"))
                .andExpect(jsonPath("$.endereco.municipio").value("Lagarto"))
                .andExpect(jsonPath("$.materiaisAceitos.length()").value(0));

        // As colunas do endereço estão na própria tabela do ponto (ADR 0005)
        assertEquals("49400000", jdbc.queryForObject(
                "SELECT poc_tx_cep FROM coleta.poc_ponto_coleta WHERE poc_nr_id = ?", String.class, id));
    }

    @Test
    void rf04_contatoEmBrancoEhGravadoComoNulo() throws Exception {
        int id = novoPonto(corpo("Sem contato", "   ", null));

        assertNull(jdbc.queryForObject(
                "SELECT poc_tx_contato FROM coleta.poc_ponto_coleta WHERE poc_nr_id = ?", String.class, id));
    }

    @Test
    void rf04_pontoSemNomeOuSemEnderecoOuComCepInvalidoRetorna400SemGravar() throws Exception {
        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ponto", null, null).replace("49400000", "49400-000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='endereco.cep')]").exists());

        assertEquals(0, contarPontos());
    }

    @Test
    void rf04_soAdministradorCadastraEAtualiza() throws Exception {
        int id = novoPonto(corpo("Ponto", null, null));

        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Outro", null, null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/pontos-coleta/" + id).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Ponto", null, "INATIVO")))
                .andExpect(status().isForbidden());

        assertEquals(1, contarPontos());
    }

    @Test
    void rf04_atualizarEInativarOPonto() throws Exception {
        int id = novoPonto(corpo("Ponto", "(79) 1", null));

        mockMvc.perform(put("/pontos-coleta/" + id).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ponto renomeado", null, "INATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ponto renomeado"))
                .andExpect(jsonPath("$.situacao").value("INATIVO"))
                .andExpect(jsonPath("$.contato").doesNotExist());
        mockMvc.perform(put("/pontos-coleta/999999").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("X", null, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rf04_listagemPublicaSoAtivosAdministradorFiltraPorSituacao() throws Exception {
        novoPonto(corpo("Ativo A", null, null));
        int inativo = novoPonto(corpo("Inativo", null, "INATIVO"));
        novoPonto(corpo("Ativo B", null, null));

        mockMvc.perform(get("/pontos-coleta"))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/pontos-coleta?situacao=INATIVO").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1"))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/pontos-coleta").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(3));
        mockMvc.perform(get("/pontos-coleta?situacao=INATIVO").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(inativo));
        mockMvc.perform(get("/pontos-coleta?size=2&page=1").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void rf04_pontoInativoEhNaoEncontradoParaOPublicoEVisivelParaOAdministrador() throws Exception {
        int inativo = novoPonto(corpo("Inativo", null, "INATIVO"));

        mockMvc.perform(get("/pontos-coleta/" + inativo)).andExpect(status().isNotFound());
        mockMvc.perform(get("/pontos-coleta/" + inativo).header("X-Perfil", ADMIN)).andExpect(status().isOk());
    }

    @Test
    void i18n_pontoInexistenteSaiEmInglesComAcceptLanguage() throws Exception {
        mockMvc.perform(get("/pontos-coleta/999999").header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Collection point not found."));
    }

    @Test
    void rf04_bancoRejeitaNomeContatoHorariosVaziosCepEUfInvalidos() {
        String inserir = """
                INSERT INTO coleta.poc_ponto_coleta (poc_tx_nome, poc_tx_contato, poc_tx_horarios, poc_tx_logradouro,
                    poc_tx_numero, poc_tx_bairro, poc_tx_municipio, poc_tx_uf, poc_tx_cep)
                VALUES (?, ?, ?, 'Rua A', '1', 'Centro', 'Lagarto', ?, ?)""";

        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, " ", null, null, "SE", "49400000"))).contains("ck_poc_nome"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "P", " ", null, "SE", "49400000"))).contains("ck_poc_contato"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "P", null, "", "SE", "49400000"))).contains("ck_poc_horarios"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "P", null, null, "se", "49400000"))).contains("ck_poc_uf"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "P", null, null, "SE", "4940"))).contains("ck_poc_cep"));
    }

    // ---------- apoio ----------

    private int novoPonto(String corpo) throws Exception {
        String resposta = mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int contarPontos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta.poc_ponto_coleta", Integer.class);
    }

    private static String mensagem(DataIntegrityViolationException erro) {
        return erro.getMostSpecificCause().getMessage();
    }
}
