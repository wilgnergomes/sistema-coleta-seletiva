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

/** RF03 de ponta a ponta, com PostgreSQL real, incluindo a regra do ADR 0013 com fixtures SQL. */
class MaterialIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    private static String corpo(String nome, String unidade, String situacao) {
        String sit = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        return "{\"nome\": \"%s\", \"descricao\": \"Descrição\", \"unidade\": \"%s\", \"orientacao\": \"Lavar\"%s}"
                .formatted(nome, unidade, sit);
    }

    // ---------- cadastro, consulta e visibilidade ----------

    @Test
    void rf03_postDepoisGetDevolveOMaterialAtivoPorPadrao() throws Exception {
        int id = novoMaterial(corpo("  Vidro ", "KG", null));

        mockMvc.perform(get("/materiais/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Vidro"))
                .andExpect(jsonPath("$.unidade").value("KG"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
    }

    @Test
    void rf03_nomeRepetidoEmOutraCaixaEComEspacosRetorna409() throws Exception {
        novoMaterial(corpo("Vidro", "KG", null));

        mockMvc.perform(post("/materiais").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("  VIDRO ", "L", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));

        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mat_material"));
    }

    @Test
    void rf03_consultaPublicaVeSoAtivosEAdministradorVeTodosEFiltra() throws Exception {
        novoMaterial(corpo("Papel", "KG", null));
        int inativo = novoMaterial(corpo("Isopor", "KG", "INATIVO"));

        mockMvc.perform(get("/materiais"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Papel"));
        mockMvc.perform(get("/materiais?situacao=INATIVO").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].situacao").value("ATIVO"));
        mockMvc.perform(get("/materiais").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/materiais?situacao=INATIVO").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(inativo));
    }

    @Test
    void rf03_materialInativoEhNaoEncontradoParaOPublicoEVisivelParaOAdministrador() throws Exception {
        int inativo = novoMaterial(corpo("Isopor", "KG", "INATIVO"));

        mockMvc.perform(get("/materiais/" + inativo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        mockMvc.perform(get("/materiais/" + inativo).header("X-Perfil", ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void rf03_listagemEhPaginadaOrdenadaPorId() throws Exception {
        int a = novoMaterial(corpo("A", "KG", null));
        novoMaterial(corpo("B", "KG", null));
        int c = novoMaterial(corpo("C", "KG", null));

        mockMvc.perform(get("/materiais?page=0&size=2"))
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.conteudo[0].id").value(a))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mockMvc.perform(get("/materiais?page=1&size=2"))
                .andExpect(jsonPath("$.conteudo[0].id").value(c));
    }

    // ---------- permissões e atualização ----------

    @Test
    void rf03_soAdministradorCadastraEAtualiza() throws Exception {
        int id = novoMaterial(corpo("Vidro", "KG", null));

        mockMvc.perform(post("/materiais").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Metal", "KG", null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/materiais/" + id).header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Vidro", "KG", "INATIVO")))
                .andExpect(status().isForbidden());

        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mat_material"));
        assertEquals("ATIVO", jdbc.queryForObject(
                "SELECT mat_tx_situacao FROM coleta.mat_material WHERE mat_nr_id = ?", String.class, id));
    }

    @Test
    void rf03_administradorAtualizaDadosEInativaOMaterial() throws Exception {
        int id = novoMaterial(corpo("Vidro", "KG", null));

        mockMvc.perform(put("/materiais/" + id).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Vidro reciclável", "KG", "INATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Vidro reciclável"))
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
        mockMvc.perform(put("/materiais/999999").header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("X", "KG", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rf03_atualizarComONomeDeOutroMaterialRetorna409() throws Exception {
        novoMaterial(corpo("Papel", "KG", null));
        int vidro = novoMaterial(corpo("Vidro", "KG", null));

        mockMvc.perform(put("/materiais/" + vidro).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("PAPEL", "KG", null)))
                .andExpect(status().isConflict());
    }

    // ---------- ADR 0013: bloqueio da troca de unidade (fixtures SQL) ----------

    @Test
    void rf03_trocarAUnidadeSemDependenciasEPermitido() throws Exception {
        int id = novoMaterial(corpo("Vidro", "KG", null));

        mockMvc.perform(put("/materiais/" + id).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Vidro", "L", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidade").value("L"));
    }

    @Test
    void rf03_trocarAUnidadeComVinculoComPontoRetorna409ENadaMuda() throws Exception {
        int id = novoMaterial(corpo("Vidro", "KG", null));
        int ponto = novoPonto();
        jdbc.update("INSERT INTO coleta.mac_material_aceito (poc_nr_id, mat_nr_id) VALUES (?, ?)", ponto, id);

        mockMvc.perform(put("/materiais/" + id).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Vidro", "L", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value(
                        "A unidade não pode ser alterada: o material já tem item de solicitação ou vínculo com ponto de coleta."));

        assertEquals("KG", unidadeNoBanco(id));
    }

    @Test
    void rf03_trocarAUnidadeComItemDeSolicitacaoRetorna409EmInglesENadaMuda() throws Exception {
        int id = novoMaterial(corpo("Vidro", "KG", null));
        int ponto = novoPonto();
        int morador = jdbc.queryForObject("""
                INSERT INTO coleta.mor_morador (mor_tx_nome, mor_tx_email) VALUES ('Maria', 'maria@exemplo.com')
                RETURNING mor_nr_id""", Integer.class);
        int solicitacao = jdbc.queryForObject("""
                INSERT INTO coleta.sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia)
                VALUES ('PROTO-1', ?, ?, 'Rua A, 1') RETURNING sol_nr_id""", Integer.class, morador, ponto);
        jdbc.update("""
                INSERT INTO coleta.iso_item_solicitacao (sol_nr_id, mat_nr_id, iso_tx_copia_unidade, iso_nr_quantidade_estimada)
                VALUES (?, ?, 'KG', 2)""", solicitacao, id);

        mockMvc.perform(put("/materiais/" + id).header("X-Perfil", ADMIN).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Vidro", "UN", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "The unit cannot be changed: the material already has a request item or is linked to a collection point."));

        assertEquals("KG", unidadeNoBanco(id));
    }

    @Test
    void rf03_comDependenciasOsDemaisCamposContinuamEditaveisEMantendoAUnidade() throws Exception {
        int id = novoMaterial(corpo("Vidro", "KG", null));
        int ponto = novoPonto();
        jdbc.update("INSERT INTO coleta.mac_material_aceito (poc_nr_id, mat_nr_id) VALUES (?, ?)", ponto, id);

        mockMvc.perform(put("/materiais/" + id).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Vidro novo", "KG", "INATIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
    }

    // ---------- restrições do banco ----------

    @Test
    void rf03_bancoRejeitaNomeDuplicadoSemDiferenciarCaixaEUnidadeInvalida() {
        String inserir = """
                INSERT INTO coleta.mat_material (mat_tx_nome, mat_tx_descricao, mat_tx_unidade, mat_tx_orientacao)
                VALUES (?, 'd', ?, 'o')""";
        jdbc.update(inserir, "Vidro", "KG");

        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, " vidro ", "KG"))).contains("uq_mat_nome"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, "Metal", "TON"))).contains("ck_mat_unidade"));
    }

    // ---------- apoio ----------

    private int novoMaterial(String corpo) throws Exception {
        String resposta = mockMvc.perform(post("/materiais").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int novoPonto() {
        return jdbc.queryForObject("""
                INSERT INTO coleta.poc_ponto_coleta (poc_tx_nome, poc_tx_logradouro, poc_tx_numero, poc_tx_bairro,
                    poc_tx_municipio, poc_tx_uf, poc_tx_cep)
                VALUES ('Ponto Teste', 'Rua A', '1', 'Centro', 'Lagarto', 'SE', '49400000') RETURNING poc_nr_id""",
                Integer.class);
    }

    private String unidadeNoBanco(int id) {
        return jdbc.queryForObject("SELECT mat_tx_unidade FROM coleta.mat_material WHERE mat_nr_id = ?", String.class, id);
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static String mensagem(DataIntegrityViolationException erro) {
        return erro.getMostSpecificCause().getMessage();
    }
}
