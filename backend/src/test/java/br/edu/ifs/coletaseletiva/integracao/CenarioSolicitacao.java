package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Monta, pela própria API, o cadastro necessário para testar solicitações (RF08). */
class CenarioSolicitacao {

    static final String ADMIN = "ADMINISTRADOR";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbc;

    CenarioSolicitacao(MockMvc mockMvc, JdbcTemplate jdbc) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
    }

    int morador(String email) throws Exception {
        return id(mockMvc.perform(post("/moradores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"Morador\", \"email\": \"" + email + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    int endereco(int moradorId) throws Exception {
        return endereco(moradorId, "Rua das Flores", null);
    }

    int endereco(int moradorId, String logradouro, String complemento) throws Exception {
        String comp = complemento == null ? "" : ", \"complemento\": \"" + complemento + "\"";
        String corpo = ("{\"logradouro\": \"%s\", \"numero\": \"120\", \"bairro\": \"Centro\", \"municipio\": \"Lagarto\", "
                + "\"uf\": \"SE\", \"cep\": \"49400000\"%s}").formatted(logradouro, comp);
        return id(mockMvc.perform(post("/moradores/" + moradorId + "/enderecos").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    int ponto(String nome, boolean domiciliar, String situacao) throws Exception {
        String corpo = ("{\"nome\": \"%s\", \"recolhimentoDomiciliar\": %s, \"situacao\": \"%s\", \"endereco\": "
                + "{\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"Centro\", \"municipio\": \"Lagarto\", "
                + "\"uf\": \"SE\", \"cep\": \"49400000\"}}").formatted(nome, domiciliar, situacao);
        return id(mockMvc.perform(post("/pontos-coleta").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    int material(String nome, String unidade, String situacao) throws Exception {
        String corpo = "{\"nome\": \"%s\", \"descricao\": \"d\", \"unidade\": \"%s\", \"orientacao\": \"o\", \"situacao\": \"%s\"}"
                .formatted(nome, unidade, situacao);
        return id(mockMvc.perform(post("/materiais").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    void vincular(int pontoId, int materialId, String limite) throws Exception {
        String corpo = limite == null ? "{}" : "{\"limite\": " + limite + "}";
        mockMvc.perform(put("/pontos-coleta/" + pontoId + "/materiais/" + materialId).header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());
    }

    int operador(String email, int pontoId, String situacao) throws Exception {
        String corpo = "{\"nome\": \"Operador\", \"email\": \"%s\", \"pontoId\": %d, \"situacao\": \"%s\"}"
                .formatted(email, pontoId, situacao);
        return id(mockMvc.perform(post("/operadores").header("X-Perfil", ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    static String corpo(int pontoId, int enderecoId, String itens) {
        return "{\"pontoId\": %d, \"enderecoId\": %d, \"observacoes\": \"Caixas na calçada\", \"itens\": [%s]}"
                .formatted(pontoId, enderecoId, itens);
    }

    static String item(int materialId, String quantidade) {
        return "{\"materialId\": %d, \"quantidadeEstimada\": %s}".formatted(materialId, quantidade);
    }

    int contar(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta." + tabela, Integer.class);
    }

    static int id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
