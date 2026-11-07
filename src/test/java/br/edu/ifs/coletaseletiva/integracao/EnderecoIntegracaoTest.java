package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF02 de ponta a ponta, com PostgreSQL real: o vínculo morador-endereço e as restrições do banco. */
class EnderecoIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    private static String corpo(String logradouro, String numero, String complemento) {
        String comp = complemento == null ? "" : ", \"complemento\": \"" + complemento + "\"";
        return "{\"logradouro\": \"%s\", \"numero\": \"%s\", \"bairro\": \"Centro\", \"municipio\": \"Lagarto\", \"uf\": \"SE\", \"cep\": \"49400000\"%s}"
                .formatted(logradouro, numero, comp);
    }

    // ---------- cadastro e listagem ----------

    @Test
    void rf02_postDepoisGetListaEnderecoGravadoComSemNumeroPadronizadoEComplementoNulo() throws Exception {
        int morador = novoMorador("maria@exemplo.com");

        int id = novoEndereco(morador, corpo("Rua das Flores", "s/n", "   "));

        mockMvc.perform(get("/moradores/" + morador + "/enderecos").header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].moradorId").value(morador))
                .andExpect(jsonPath("$[0].numero").value("S/N"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.end_endereco WHERE end_tx_complemento IS NOT NULL", Integer.class));
    }

    @Test
    void rf02_moradorComDoisEnderecosListaSomenteOsSeusEmOrdem() throws Exception {
        int maria = novoMorador("maria@exemplo.com");
        int joao = novoMorador("joao@exemplo.com");
        int e1 = novoEndereco(maria, corpo("Rua A", "1", null));
        int e2 = novoEndereco(maria, corpo("Rua B", "2", "Fundos"));
        novoEndereco(joao, corpo("Rua C", "3", null));

        mockMvc.perform(get("/moradores/" + maria + "/enderecos").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(e1))
                .andExpect(jsonPath("$[1].id").value(e2));
        mockMvc.perform(get("/moradores/" + joao + "/enderecos").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rf02_enderecoIncompletoOuComFormatoErradoRetorna400SemGravar() throws Exception {
        int morador = novoMorador("maria@exemplo.com");

        mockMvc.perform(post("/moradores/" + morador + "/enderecos").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"logradouro\": \"Rua A\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/moradores/" + morador + "/enderecos").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Rua A", "1", null).replace("49400000", "49400-000")))
                .andExpect(status().isBadRequest());

        assertEquals(0, contarEnderecos());
    }

    // ---------- vínculo morador x endereço ----------

    @Test
    void rf02_atualizarEnderecoPelaUrlDeOutroMoradorRetorna404EnaoAltera() throws Exception {
        int maria = novoMorador("maria@exemplo.com");
        int joao = novoMorador("joao@exemplo.com");
        int enderecoDoJoao = novoEndereco(joao, corpo("Rua do Joao", "9", null));

        mockMvc.perform(put("/moradores/" + maria + "/enderecos/" + enderecoDoJoao).header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Invadida", "1", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));

        String logradouro = jdbc.queryForObject(
                "SELECT end_tx_logradouro FROM coleta.end_endereco WHERE end_nr_id = ?", String.class, enderecoDoJoao);
        assertEquals("Rua do Joao", logradouro);
    }

    @Test
    void rf02_atualizarEnderecoDoProprioMoradorMantemODono() throws Exception {
        int maria = novoMorador("maria@exemplo.com");
        int id = novoEndereco(maria, corpo("Rua A", "1", null));

        mockMvc.perform(put("/moradores/" + maria + "/enderecos/" + id)
                        .header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Rua Nova", "S/N", "Apto 2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logradouro").value("Rua Nova"))
                .andExpect(jsonPath("$.moradorId").value(maria))
                .andExpect(jsonPath("$.complemento").value("Apto 2"));
    }

    // ---------- permissões e morador inexistente ----------

    @Test
    void rf02_moradorNaoMexeNosEnderecosDeOutroEOperadorEhNegado() throws Exception {
        int maria = novoMorador("maria@exemplo.com");
        int joao = novoMorador("joao@exemplo.com");

        mockMvc.perform(get("/moradores/" + joao + "/enderecos")
                        .header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/moradores/" + joao + "/enderecos")
                        .header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Rua A", "1", null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/moradores/" + maria + "/enderecos")
                        .header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden());
        assertEquals(0, contarEnderecos());
    }

    @Test
    void rf02_moradorInativoNaoCadastraEnderecoEMoradorInexistenteRetorna404() throws Exception {
        int maria = novoMorador("maria@exemplo.com");
        jdbc.update("UPDATE coleta.mor_morador SET mor_tx_situacao = 'INATIVO' WHERE mor_nr_id = ?", maria);

        mockMvc.perform(post("/moradores/" + maria + "/enderecos")
                        .header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Rua A", "1", null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/moradores/999999/enderecos").header("X-Perfil", ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void i18n_enderecoNaoEncontradoSaiEmInglesComAcceptLanguage() throws Exception {
        int maria = novoMorador("maria@exemplo.com");

        mockMvc.perform(put("/moradores/" + maria + "/enderecos/999999").header("X-Perfil", ADMIN)
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Rua A", "1", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Address not found for this resident."));
    }

    // ---------- restrições do banco ----------

    @Test
    void rf02_bancoRejeitaCepUfENumeroInvalidos() {
        int maria = jdbc.queryForObject("""
                INSERT INTO coleta.mor_morador (mor_tx_nome, mor_tx_email) VALUES ('Maria', 'maria@exemplo.com')
                RETURNING mor_nr_id""", Integer.class);
        String inserir = """
                INSERT INTO coleta.end_endereco (mor_nr_id, end_tx_logradouro, end_tx_numero, end_tx_bairro,
                    end_tx_municipio, end_tx_uf, end_tx_cep) VALUES (?, 'Rua A', ?, 'Centro', 'Lagarto', ?, ?)""";

        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, maria, "1", "SE", "4940000"))).contains("ck_end_cep"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, maria, "1", "se", "49400000"))).contains("ck_end_uf"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, maria, "  ", "SE", "49400000"))).contains("ck_end_numero"));
    }

    @Test
    void rf02_bancoNaoAceitaEnderecoDeMoradorInexistente() {
        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO coleta.end_endereco (mor_nr_id, end_tx_logradouro, end_tx_numero, end_tx_bairro,
                    end_tx_municipio, end_tx_uf, end_tx_cep) VALUES (999999, 'Rua A', '1', 'Centro', 'Lagarto', 'SE', '49400000')"""));

        assertTrue(mensagem(erro).contains("fk_end_mor"));
    }

    // ---------- apoio ----------

    private int novoMorador(String email) throws Exception {
        String resposta = mockMvc.perform(post("/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Morador\", \"email\": \"" + email + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int novoEndereco(int morador, String corpo) throws Exception {
        String resposta = mockMvc.perform(post("/moradores/" + morador + "/enderecos").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int contarEnderecos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta.end_endereco", Integer.class);
    }

    private static String mensagem(DataIntegrityViolationException erro) {
        return erro.getMostSpecificCause().getMessage();
    }
}
