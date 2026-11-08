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

/** RF07 de ponta a ponta, com PostgreSQL real: vínculo operador-ponto (1:N) e e-mail normalizado. */
class OperadorIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    private static String corpo(String nome, String email, int pontoId, String situacao) {
        String sit = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        return "{\"nome\": \"%s\", \"email\": \"%s\", \"pontoId\": %d%s}".formatted(nome, email, pontoId, sit);
    }

    @Test
    void rf07_postDepoisGetDevolveOperadorComEmailNormalizadoVinculadoAoPonto() throws Exception {
        int ponto = novoPonto("Ponto A", null);

        int id = novoOperador(corpo("Carla Nunes", "  Carla.Nunes@Exemplo.COM ", ponto, null));

        mockMvc.perform(get("/operadores/" + id).header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carla.nunes@exemplo.com"))
                .andExpect(jsonPath("$.pontoId").value(ponto))
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
        assertEquals("carla.nunes@exemplo.com", jdbc.queryForObject(
                "SELECT ope_tx_email FROM coleta.ope_operador WHERE ope_nr_id = ?", String.class, id));
    }

    @Test
    void rf07_doisOperadoresNoMesmoPontoEEmailRepetidoEmOutraCaixaRetorna409() throws Exception {
        int ponto = novoPonto("Ponto A", null);
        novoOperador(corpo("Carla", "carla@exemplo.com", ponto, null));
        novoOperador(corpo("Paulo", "paulo@exemplo.com", ponto, null));

        mockMvc.perform(post("/operadores").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outra", " CARLA@exemplo.com", ponto, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));

        assertEquals(2, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
    }

    @Test
    void rf07_pontoInexistenteRetorna404EDadosInvalidosRetornam400() throws Exception {
        mockMvc.perform(post("/operadores").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Carla", "carla@exemplo.com", 999999, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Ponto de coleta não encontrado."));
        mockMvc.perform(post("/operadores").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Carla\", \"email\": \"x\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
    }

    @Test
    void rf07_listaPaginadaComFiltroPorPonto() throws Exception {
        int a = novoPonto("Ponto A", null);
        int b = novoPonto("Ponto B", null);
        novoOperador(corpo("O1", "o1@exemplo.com", a, null));
        novoOperador(corpo("O2", "o2@exemplo.com", a, null));
        int o3 = novoOperador(corpo("O3", "o3@exemplo.com", b, null));

        mockMvc.perform(get("/operadores").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(3));
        mockMvc.perform(get("/operadores?pontoId=" + a).header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/operadores?pontoId=" + b).header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.conteudo[0].id").value(o3));
        mockMvc.perform(get("/operadores?pontoId=999999").header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/operadores?size=2&page=1").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void rf07_atualizarMudaOPontoEInativaOOperador() throws Exception {
        int a = novoPonto("Ponto A", null);
        int b = novoPonto("Ponto B", "INATIVO");
        int id = novoOperador(corpo("Carla", "carla@exemplo.com", a, null));

        mockMvc.perform(put("/operadores/" + id).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Carla Nunes", "carla@exemplo.com", b, "INATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontoId").value(b))
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
        mockMvc.perform(put("/operadores/999999").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("X", "x@exemplo.com", a, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rf07_atualizarComEmailDeOutroOperadorRetorna409EMantemOProprioEmail() throws Exception {
        int ponto = novoPonto("Ponto A", null);
        novoOperador(corpo("Carla", "carla@exemplo.com", ponto, null));
        int paulo = novoOperador(corpo("Paulo", "paulo@exemplo.com", ponto, null));

        mockMvc.perform(put("/operadores/" + paulo).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Paulo", "CARLA@exemplo.com", ponto, null)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/operadores/" + paulo).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Paulo Santos", "paulo@exemplo.com", ponto, null)))
                .andExpect(status().isOk());
    }

    @Test
    void rf07_soAdministradorGerenciaOperadores() throws Exception {
        int ponto = novoPonto("Ponto A", null);
        int id = novoOperador(corpo("Carla", "carla@exemplo.com", ponto, null));

        mockMvc.perform(get("/operadores").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(id)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/operadores/" + id).header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/operadores").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("X", "x@exemplo.com", ponto, null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/operadores/" + id).header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Y", "y@exemplo.com", ponto, null)))
                .andExpect(status().isForbidden());

        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.ope_operador"));
    }

    @Test
    void i18n_operadorInexistenteSaiEmInglesComAcceptLanguage() throws Exception {
        mockMvc.perform(get("/operadores/999999").header("X-Perfil", ADMIN).header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Operator not found."));
    }

    @Test
    void rf07_bancoRejeitaEmailDuplicadoNaoNormalizadoEOperadorSemPonto() throws Exception {
        int ponto = novoPonto("Ponto A", null);
        String inserir = "INSERT INTO coleta.ope_operador (ope_tx_nome, ope_tx_email, poc_nr_id) VALUES ('Op', ?, ?)";
        jdbc.update(inserir, "op@exemplo.com", ponto);

        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "op@exemplo.com", ponto))).contains("uq_ope_email"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "Op2@Exemplo.com", ponto))).contains("ck_ope_email_normalizado"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "op3@exemplo.com", 999999))).contains("fk_ope_poc"));
    }

    // ---------- apoio ----------

    private int novoPonto(String nome, String situacao) throws Exception {
        String sit = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        String corpo = ("{\"nome\": \"%s\"%s, \"endereco\": {\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"Centro\", "
                + "\"municipio\": \"Lagarto\", \"uf\": \"SE\", \"cep\": \"49400000\"}}").formatted(nome, sit);
        return id(mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private int novoOperador(String corpo) throws Exception {
        return id(mockMvc.perform(post("/operadores").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private static int id(String json) {
        return JsonPath.read(json, "$.id");
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static String mensagem(DataIntegrityViolationException erro) {
        return erro.getMostSpecificCause().getMessage();
    }
}
