package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF05 de ponta a ponta, com PostgreSQL real: chave composta, upsert e materiais nas respostas de ponto. */
class MaterialAceitoIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    private int ponto;
    private int vidro;
    private int papel;

    private void preparar() throws Exception {
        ponto = novoPonto("Ponto A", null);
        vidro = novoMaterial("Vidro", "KG", null);
        papel = novoMaterial("Papel", "KG", null);
    }

    private org.springframework.test.web.servlet.ResultActions vincular(int p, int m, String corpo) throws Exception {
        return mockMvc.perform(put("/pontos-coleta/" + p + "/materiais/" + m).header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void rf05_vincularDuasVezesNaoRepeteOVinculoEAtualizaOsDados() throws Exception {
        preparar();

        vincular(ponto, vidro, "{\"observacoes\": \"Limpas\", \"limite\": 50}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.limite").value(50.0));
        vincular(ponto, vidro, "{\"observacoes\": \"Qualquer\", \"limite\": 80.25}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.observacoes").value("Qualquer"));

        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
        assertEquals(0, new java.math.BigDecimal("80.250").compareTo(jdbc.queryForObject(
                "SELECT mac_nr_limite FROM coleta.mac_material_aceito", java.math.BigDecimal.class)));
    }

    @Test
    void rf05_limiteInvalidoRetorna400SemGravar() throws Exception {
        preparar();

        vincular(ponto, vidro, "{\"limite\": 0}").andExpect(status().isBadRequest());
        vincular(ponto, vidro, "{\"limite\": -1}").andExpect(status().isBadRequest());

        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
    }

    @Test
    void rf05_umPontoAceitaVariosMateriaisEOMesmoMaterialVariosPontos() throws Exception {
        preparar();
        int outroPonto = novoPonto("Ponto B", null);

        vincular(ponto, vidro, "{}").andExpect(status().isOk());
        vincular(ponto, papel, "{}").andExpect(status().isOk());
        vincular(outroPonto, vidro, "{}").andExpect(status().isOk());

        assertEquals(3, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
    }

    @Test
    void rf05_pontoOuMaterialInexistenteRetorna404() throws Exception {
        preparar();

        vincular(999999, vidro, "{}").andExpect(status().isNotFound());
        vincular(ponto, 999999, "{}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Material não encontrado."));
        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
    }

    @Test
    void rf05_pontoEMaterialInativosPodemSerVinculadosPeloAdministrador() throws Exception {
        int pontoInativo = novoPonto("Inativo", "INATIVO");
        int materialInativo = novoMaterial("Isopor", "KG", "INATIVO");

        vincular(pontoInativo, materialInativo, "{}").andExpect(status().isOk());
    }

    @Test
    void rf05_soAdministradorVinculaEDesvincula() throws Exception {
        preparar();
        vincular(ponto, vidro, "{}").andExpect(status().isOk());

        mockMvc.perform(put("/pontos-coleta/" + ponto + "/materiais/" + papel)
                        .header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/pontos-coleta/" + ponto + "/materiais/" + vidro)
                        .header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden());

        assertEquals(1, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
    }

    @Test
    void rf05_desvincularRemoveOVinculoEDeNovoRetorna404() throws Exception {
        preparar();
        vincular(ponto, vidro, "{}").andExpect(status().isOk());

        mockMvc.perform(delete("/pontos-coleta/" + ponto + "/materiais/" + vidro).header("X-Perfil", ADMIN))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/pontos-coleta/" + ponto + "/materiais/" + vidro).header("X-Perfil", ADMIN).header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Link between the collection point and the material not found."));

        assertEquals(0, contar("SELECT COUNT(*) FROM coleta.mac_material_aceito"));
    }

    @Test
    void rf05_osMateriaisAceitosAparecemNaConsultaDoPontoNaListagemEEmBranco() throws Exception {
        preparar();
        int semMateriais = novoPonto("Ponto sem materiais", null);
        vincular(ponto, papel, "{}").andExpect(status().isOk());
        vincular(ponto, vidro, "{\"observacoes\": \"Limpas\", \"limite\": 50}").andExpect(status().isOk());

        mockMvc.perform(get("/pontos-coleta/" + ponto))
                .andExpect(jsonPath("$.materiaisAceitos.length()").value(2))
                .andExpect(jsonPath("$.materiaisAceitos[0].nomeMaterial").value("Vidro"))
                .andExpect(jsonPath("$.materiaisAceitos[0].unidade").value("KG"))
                .andExpect(jsonPath("$.materiaisAceitos[0].observacoes").value("Limpas"))
                .andExpect(jsonPath("$.materiaisAceitos[0].limite").value(50.0))
                .andExpect(jsonPath("$.materiaisAceitos[1].nomeMaterial").value("Papel"));
        mockMvc.perform(get("/pontos-coleta"))
                .andExpect(jsonPath("$.conteudo[?(@.id==" + ponto + ")].materiaisAceitos.length()").value(2))
                .andExpect(jsonPath("$.conteudo[?(@.id==" + semMateriais + ")].materiaisAceitos.length()").value(0));
    }

    @Test
    void rf05_bancoRejeitaLimiteZeroEVinculoRepetidoEMantemAIntegridade() throws Exception {
        preparar();
        String inserir = "INSERT INTO coleta.mac_material_aceito (poc_nr_id, mat_nr_id, mac_nr_limite) VALUES (?, ?, ?)";

        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, ponto, vidro, 0))).contains("ck_mac_limite"));
        jdbc.update(inserir, ponto, vidro, 10);
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, ponto, vidro, 20))).contains("pk_mac_material_aceito"));
        assertTrue(mensagem(assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(inserir, 999999, vidro, 20))).contains("fk_mac_poc"));
    }

    @Test
    void rf05_umVinculoPassaABloquearATrocaDeUnidadeDoMaterial() throws Exception {
        preparar();
        vincular(ponto, vidro, "{}").andExpect(status().isOk());

        mockMvc.perform(put("/materiais/" + vidro).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Vidro\", \"descricao\": \"d\", \"unidade\": \"L\", \"orientacao\": \"o\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- apoio ----------

    private int novoPonto(String nome, String situacao) throws Exception {
        String sit = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        String corpo = ("{\"nome\": \"%s\"%s, \"endereco\": {\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"Centro\", "
                + "\"municipio\": \"Lagarto\", \"uf\": \"SE\", \"cep\": \"49400000\"}}").formatted(nome, sit);
        String resposta = mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int novoMaterial(String nome, String unidade, String situacao) throws Exception {
        String sit = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        String corpo = "{\"nome\": \"%s\", \"descricao\": \"d\", \"unidade\": \"%s\", \"orientacao\": \"o\"%s}"
                .formatted(nome, unidade, sit);
        String resposta = mockMvc.perform(post("/materiais").header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static String mensagem(DataIntegrityViolationException erro) {
        return erro.getMostSpecificCause().getMessage();
    }
}
