package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentação da API no Swagger (springdoc): confere o documento gerado em {@code /v3/api-docs}. Garante que toda
 * operação está explicada, que a identificação do ator só aparece onde é exigida, que os exemplos de corpo são
 * aceitos pelos formulários reais e que as operações coincidem com o contrato {@code docs/api/openapi.yaml}.
 */
class OpenApiDocumentacaoTest extends IntegracaoBase {

    private static final String PACOTE_FORMS = "br.edu.ifs.coletaseletiva.form.";

    /** Consultas públicas (o ator é opcional) e o único cadastro público (sem ator algum). */
    private static final Set<String> ATOR_OPCIONAL = Set.of(
            "GET /materiais", "GET /materiais/{id}", "GET /pontos-coleta", "GET /pontos-coleta/{id}");
    private static final Set<String> SEM_ATOR = Set.of("POST /moradores");

    private static final Set<String> CRIACOES = Set.of(
            "POST /moradores", "POST /moradores/{moradorId}/enderecos", "POST /materiais", "POST /pontos-coleta",
            "POST /operadores", "POST /solicitacoes");

    @Autowired
    private JsonMapper json;

    @Autowired
    private Validator validador;

    private Map<String, Object> doc;

    @BeforeEach
    void gerarDocumento() throws Exception {
        String corpo = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        doc = JsonPath.read(corpo, "$");
    }

    // ---------- toda operação está explicada ----------

    @Test
    void doc_todaOperacaoTemResumoDescricaoGrupoEParametrosDescritos() {
        for (var op : operacoes().entrySet()) {
            Map<String, Object> o = op.getValue();
            assertTrue(texto(o, "summary").length() > 5, op.getKey() + ": sem resumo");
            assertTrue(texto(o, "description").length() > 20, op.getKey() + ": sem descrição");
            assertFalse(((List<?>) o.get("tags")).isEmpty(), op.getKey() + ": sem grupo");
            assertTrue(sucessos(o).size() == 1, op.getKey() + ": deve declarar exatamente uma resposta de sucesso");
            for (Object p : (List<?>) o.getOrDefault("parameters", List.of())) {
                Map<?, ?> parametro = (Map<?, ?>) p;
                assertTrue(parametro.get("description") instanceof String d && !d.isBlank(),
                        op.getKey() + ": parâmetro " + parametro.get("name") + " sem descrição");
            }
        }
    }

    @Test
    void doc_respostasDeErroUsamOFormatoRealDaApi() {
        for (var op : operacoes().entrySet()) {
            Map<String, Object> respostas = mapa(op.getValue(), "responses");
            for (var r : respostas.entrySet()) {
                if (Integer.parseInt(r.getKey()) < 400) {
                    continue;
                }
                Map<String, Object> media = mapa(mapa(mapa(r.getValue(), "content"), "application/json"), "schema");
                assertEquals("#/components/schemas/RespostaErro", media.get("$ref"),
                        op.getKey() + " " + r.getKey() + ": erro fora do formato { codigo, mensagem, erros }");
            }
        }
        assertTrue(mapa(mapa(doc, "components"), "schemas").containsKey("RespostaErro"));
    }

    @Test
    void doc_criacoesRespondem201ComLocationENaoDocumentam200() {
        for (String chave : CRIACOES) {
            Map<String, Object> respostas = mapa(operacoes().get(chave), "responses");
            assertTrue(respostas.containsKey("201"), chave + ": falta o 201");
            assertFalse(respostas.containsKey("200"), chave + ": o 200 sobrou");
            assertTrue(mapa(mapa(respostas, "201"), "headers").containsKey("Location"), chave + ": falta o Location");
        }
        assertTrue(mapa(operacoes().get("DELETE /pontos-coleta/{pontoId}/materiais/{materialId}"), "responses")
                .containsKey("204"));
    }

    // ---------- identificação do ator ----------

    @Test
    void doc_identificacaoDoAtorApareceSoOndeEExigida() {
        assertFalse(doc.containsKey("security"), "não deve haver exigência global de ator");
        Map<String, Object> esquemas = mapa(mapa(doc, "components"), "securitySchemes");
        assertEquals("X-Perfil", mapa(esquemas, "perfil").get("name"));
        assertEquals("X-Usuario-Id", mapa(esquemas, "usuarioId").get("name"));

        for (var op : operacoes().entrySet()) {
            List<?> seguranca = (List<?>) op.getValue().get("security");
            String chave = op.getKey();
            if (SEM_ATOR.contains(chave)) {
                assertEquals(List.of(), seguranca, chave + ": cadastro público não pede ator");
            } else if (ATOR_OPCIONAL.contains(chave)) {
                assertEquals(2, seguranca.size(), chave + ": consulta pública aceita ator opcional");
                assertTrue(((Map<?, ?>) seguranca.get(0)).isEmpty(), chave + ": falta a alternativa sem ator");
                assertEquals(Set.of("perfil", "usuarioId"), ((Map<?, ?>) seguranca.get(1)).keySet());
            } else {
                assertEquals(1, seguranca.size(), chave + ": exige ator");
                assertEquals(Set.of("perfil", "usuarioId"), ((Map<?, ?>) seguranca.get(0)).keySet(), chave);
            }
        }
    }

    @Test
    void doc_cabecalhoDeIdiomaEstaEmTodaOperacao() {
        for (var op : operacoes().entrySet()) {
            boolean tem = ((List<?>) op.getValue().get("parameters")).stream()
                    .anyMatch(p -> "Accept-Language".equals(((Map<?, ?>) p).get("name")));
            assertTrue(tem, op.getKey() + ": falta o Accept-Language");
        }
    }

    // ---------- exemplos de corpo ----------

    @Test
    void doc_exemplosDosCorposSaoAceitosPelosFormularios() throws Exception {
        Map<String, Object> esquemas = mapa(mapa(doc, "components"), "schemas");
        Set<String> verificados = new TreeSet<>();
        for (var op : operacoes().entrySet()) {
            Map<String, Object> corpo = mapa(op.getValue(), "requestBody");
            if (corpo.isEmpty()) {
                continue;
            }
            String ref = (String) mapa(mapa(mapa(corpo, "content"), "application/json"), "schema").get("$ref");
            String nome = ref.substring(ref.lastIndexOf('/') + 1);
            Object exemplo = exemploDe(esquemas, nome);
            Object form = json.readValue(json.writeValueAsString(exemplo), Class.forName(PACOTE_FORMS + nome));
            var violacoes = validador.validate(form);
            assertTrue(violacoes.isEmpty(), op.getKey() + ": exemplo de " + nome + " rejeitado: " + violacoes);
            verificados.add(nome);
        }
        assertTrue(verificados.size() >= 14, "poucos corpos verificados: " + verificados);
    }

    // ---------- contrato ----------

    @Test
    void doc_operacoesGeradasCoincidemComOContratoYaml() throws Exception {
        Map<String, Object> contrato = new Yaml().load(Files.readString(Path.of("docs/api/openapi.yaml")));
        Set<String> doContrato = new TreeSet<>();
        for (var caminho : mapa(contrato, "paths").entrySet()) {
            for (String metodo : List.of("get", "post", "put", "delete")) {
                if (mapa(caminho.getValue(), metodo).size() > 0) {
                    doContrato.add(metodo.toUpperCase() + " " + caminho.getKey());
                }
            }
        }
        assertEquals(doContrato, new TreeSet<>(operacoes().keySet()),
                "operações do contrato e do Swagger divergem");
    }

    // ---------- apoio ----------

    private Map<String, Map<String, Object>> operacoes() {
        Map<String, Map<String, Object>> ops = new LinkedHashMap<>();
        for (var caminho : mapa(doc, "paths").entrySet()) {
            for (var metodo : mapa(caminho.getValue()).entrySet()) {
                if (Set.of("get", "post", "put", "delete").contains(metodo.getKey())) {
                    ops.put(metodo.getKey().toUpperCase() + " " + caminho.getKey(), mapa(metodo.getValue()));
                }
            }
        }
        return ops;
    }

    private static List<String> sucessos(Map<String, Object> operacao) {
        List<String> codigos = new ArrayList<>();
        for (String codigo : mapa(operacao, "responses").keySet()) {
            if (codigo.startsWith("2")) {
                codigos.add(codigo);
            }
        }
        return codigos;
    }

    /** Monta um corpo com o {@code example} de cada propriedade, resolvendo $ref e listas. */
    @SuppressWarnings("unchecked")
    private Object exemploDe(Map<String, Object> esquemas, String nome) {
        Map<String, Object> propriedades = mapa(mapa(esquemas, nome), "properties");
        Map<String, Object> corpo = new LinkedHashMap<>();
        for (var p : propriedades.entrySet()) {
            Map<String, Object> esquema = mapa(p.getValue());
            if (esquema.containsKey("$ref")) {
                corpo.put(p.getKey(), exemploDe(esquemas, nomeDe((String) esquema.get("$ref"))));
            } else if ("array".equals(esquema.get("type"))) {
                Map<String, Object> item = mapa(esquema, "items");
                corpo.put(p.getKey(), List.of(exemploDe(esquemas, nomeDe((String) item.get("$ref")))));
            } else if (esquema.containsKey("example")) {
                corpo.put(p.getKey(), esquema.get("example"));
            }
        }
        return corpo;
    }

    private static String nomeDe(String ref) {
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    private static String texto(Map<String, Object> mapa, String chave) {
        return mapa.get(chave) instanceof String s ? s : "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static Map<String, Object> mapa(Object o, String chave) {
        return mapa(mapa(o).get(chave));
    }
}
