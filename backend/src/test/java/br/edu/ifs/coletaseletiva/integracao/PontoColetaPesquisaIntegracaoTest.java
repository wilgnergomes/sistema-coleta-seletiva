package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF06: pesquisa de pontos com filtros, contra o PostgreSQL real. */
class PontoColetaPesquisaIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    private int verdeVida;       // Lagarto / Centro, ativo, aceita vidro e papel
    private int ecoCentro;       // Aracaju / Centro, ativo, aceita só papel
    private int antigo;          // Lagarto / Distrito, INATIVO, aceita vidro
    private int vidro;
    private int papel;

    private void catalogo() throws Exception {
        vidro = novoMaterial("Vidro");
        papel = novoMaterial("Papel");
        verdeVida = novoPonto("Cooperativa Verde Vida", "Lagarto", "Centro", null);
        ecoCentro = novoPonto("Ecoponto Centro", "Aracaju", "Centro", null);
        antigo = novoPonto("Ecoponto Antigo", "Lagarto", "Distrito", "INATIVO");
        vincular(verdeVida, vidro);
        vincular(verdeVida, papel);
        vincular(ecoCentro, papel);
        vincular(antigo, vidro);
    }

    @Test
    void rf06_filtroPorMaterialVidroRetornaSomentePontosAtivosQueAceitamVidro() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?materialId=" + vidro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(verdeVida))
                // o ponto devolvido mostra todos os seus materiais, não só o filtrado
                .andExpect(jsonPath("$.conteudo[0].materiaisAceitos.length()").value(2));
    }

    @Test
    void rf06_administradorCombinaMaterialESituacaoEVeOPontoInativoQueAceitaVidro() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?materialId=" + vidro).header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/pontos-coleta?materialId=" + vidro + "&situacao=INATIVO").header("X-Perfil", ADMIN))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(antigo));
        // situacao não dá ao público acesso aos inativos
        mockMvc.perform(get("/pontos-coleta?materialId=" + vidro + "&situacao=INATIVO"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(verdeVida));
    }

    @Test
    void rf06_nomeEPorTrechoSemDiferenciarMaiusculas() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?nome=VERDE")).andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/pontos-coleta?nome=eco")).andExpect(jsonPath("$.totalElementos").value(1)); // o antigo é inativo
        mockMvc.perform(get("/pontos-coleta").param("nome", "  vida ")).andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/pontos-coleta?nome=inexistente")).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void rf06_caracteresCuringaDigitadosValemComoTextoComum() throws Exception {
        catalogo();

        // Sem o escape, "%" casaria com todos os nomes e "_" com qualquer caractere
        mockMvc.perform(get("/pontos-coleta").param("nome", "%")).andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/pontos-coleta").param("nome", "_")).andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/pontos-coleta").param("nome", "Eco_onto")).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void rf06_municipioEBairroSaoIgualdadeSemDiferenciarMaiusculas() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?municipio=lagarto")).andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/pontos-coleta?municipio=Lag")).andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/pontos-coleta?bairro=CENTRO")).andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get("/pontos-coleta?municipio=Aracaju&bairro=Centro"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(ecoCentro));
    }

    @Test
    void rf06_filtrosSeCombinamComE() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?municipio=Lagarto&materialId=" + papel + "&nome=verde"))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/pontos-coleta?municipio=Aracaju&materialId=" + vidro))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void rf06_materialInexistenteRetorna200ComListaVazia() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?materialId=999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.conteudo.length()").value(0));
    }

    @Test
    void rf06_paginacaoFuncionaComFiltro() throws Exception {
        catalogo();

        mockMvc.perform(get("/pontos-coleta?bairro=Centro&size=1&page=0"))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo[0].id").value(verdeVida));
        mockMvc.perform(get("/pontos-coleta?bairro=Centro&size=1&page=1"))
                .andExpect(jsonPath("$.conteudo[0].id").value(ecoCentro));
    }

    @Test
    void rf06_pontoDesvinculadoDeixaDeAparecerNoFiltroDoMaterial() throws Exception {
        catalogo();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/pontos-coleta/" + verdeVida + "/materiais/" + vidro).header("X-Perfil", ADMIN))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/pontos-coleta?materialId=" + vidro)).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void i18n_filtroLongoDemaisSaiEmInglesComAcceptLanguage() throws Exception {
        mockMvc.perform(get("/pontos-coleta?bairro=" + "x".repeat(101)).header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='bairro')].mensagem").value("must have at most 100 characters"));
    }

    // ---------- apoio ----------

    private int novoMaterial(String nome) throws Exception {
        String corpo = "{\"nome\": \"%s\", \"descricao\": \"d\", \"unidade\": \"KG\", \"orientacao\": \"o\"}".formatted(nome);
        return id(mockMvc.perform(post("/materiais").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private int novoPonto(String nome, String municipio, String bairro, String situacao) throws Exception {
        String sit = situacao == null ? "" : ", \"situacao\": \"" + situacao + "\"";
        String corpo = ("{\"nome\": \"%s\"%s, \"endereco\": {\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"%s\", "
                + "\"municipio\": \"%s\", \"uf\": \"SE\", \"cep\": \"49400000\"}}").formatted(nome, sit, bairro, municipio);
        return id(mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private void vincular(int ponto, int material) throws Exception {
        mockMvc.perform(put("/pontos-coleta/" + ponto + "/materiais/" + material).header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }

    private static int id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
