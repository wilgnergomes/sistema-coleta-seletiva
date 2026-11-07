package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF01 de ponta a ponta: requisição HTTP, controller, service, repository e PostgreSQL real.
 * Aqui a prova é de que as regras funcionam juntas, e não cada peça isolada.
 */
class MoradorFluxoIntegracaoTest extends IntegracaoBase {

    private static final String ADMIN = "ADMINISTRADOR";

    // ---------- cadastro e consulta ----------

    @Test
    void rf01_postDepoisGetDevolveOMoradorGravadoComEmailNormalizado() throws Exception {
        String resposta = mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "  Maria Silva ", "email": "  Maria.Silva@Exemplo.COM ", "fone": "79999990000"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(resposta, "$.id");

        mockMvc.perform(get("/moradores/" + id).header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria.silva@exemplo.com"))
                .andExpect(jsonPath("$.fone").value("79999990000"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"));

        // O que ficou gravado no banco é o e-mail normalizado, não o que o cliente digitou.
        String emailNoBanco = jdbc.queryForObject(
                "SELECT mor_tx_email FROM coleta.mor_morador WHERE mor_nr_id = ?", String.class, id);
        assertEquals("maria.silva@exemplo.com", emailNoBanco);
    }

    @Test
    void rf01_foneEmBrancoEhGravadoComoNulo() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", "   ");

        String fone = jdbc.queryForObject(
                "SELECT mor_tx_fone FROM coleta.mor_morador WHERE mor_nr_id = ?", String.class, id);
        assertNull(fone);
    }

    @Test
    void rf01_postComEmailRepetidoEmOutraCaixaRetorna409EMantemUmaLinha() throws Exception {
        cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outra Maria", "email": "  MARIA@exemplo.com "}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));

        assertEquals(1, contarMoradores());
    }

    @Test
    void i18n_emailRepetidoComAcceptLanguageEnSaiEmInglesComOMesmoCodigoEStatus() throws Exception {
        cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(post("/moradores")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outra Maria", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A resident with this email is already registered."));
    }

    @Test
    void rf01_listagemEhPaginadaEOrdenadaPorId() throws Exception {
        int primeiro = cadastrar("Ana", "ana@exemplo.com", null);
        cadastrar("Bia", "bia@exemplo.com", null);
        int terceiro = cadastrar("Caio", "caio@exemplo.com", null);

        mockMvc.perform(get("/moradores?page=0&size=2").header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.conteudo[0].id").value(primeiro))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        mockMvc.perform(get("/moradores?page=1&size=2").header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(terceiro));
    }

    @Test
    void rf01_buscarIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/moradores/999999").header("X-Perfil", ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    // ---------- atualização e situação ----------

    @Test
    void rf01_moradorAtualizaOProprioCadastro() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(put("/moradores/" + id)
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria Silva", "email": " Maria.Silva@Exemplo.com ", "fone": "79988887777"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria.silva@exemplo.com"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
    }

    @Test
    void rf01_putComOMesmoEmailDoProprioMoradorNaoEhConflito() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(put("/moradores/" + id)
                        .header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria Souza", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void rf01_putComEmailDeOutroMoradorRetorna409() throws Exception {
        cadastrar("Ana", "ana@exemplo.com", null);
        int bia = cadastrar("Bia", "bia@exemplo.com", null);

        mockMvc.perform(put("/moradores/" + bia)
                        .header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bia", "email": "ANA@exemplo.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));
    }

    @Test
    void rf01_administradorInativaMoradorQueEntaoPerdeOAcesso() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(put("/moradores/" + id)
                        .header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com", "situacao": "INATIVO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));

        mockMvc.perform(get("/moradores/" + id)
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(id)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        // Inativar não apaga: o cadastro continua no banco e o administrador ainda o enxerga.
        mockMvc.perform(get("/moradores/" + id).header("X-Perfil", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
    }

    @Test
    void rf01_putSemSituacaoMantemOMoradorInativo() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", null);
        jdbc.update("UPDATE coleta.mor_morador SET mor_tx_situacao = 'INATIVO' WHERE mor_nr_id = ?", id);

        mockMvc.perform(put("/moradores/" + id)
                        .header("X-Perfil", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria Silva", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
    }

    @Test
    void rf01_moradorNaoAlteraAPropriaSituacaoENadaMudaNoBanco() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(put("/moradores/" + id)
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com", "situacao": "INATIVO"}
                                """))
                .andExpect(status().isForbidden());

        String situacao = jdbc.queryForObject(
                "SELECT mor_tx_situacao FROM coleta.mor_morador WHERE mor_nr_id = ?", String.class, id);
        assertEquals("ATIVO", situacao);
    }

    // ---------- permissões ----------

    @Test
    void rf01_moradorNaoVeOutroMoradorEOperadorEhNegado() throws Exception {
        int maria = cadastrar("Maria", "maria@exemplo.com", null);
        int joao = cadastrar("João", "joao@exemplo.com", null);

        mockMvc.perform(get("/moradores/" + joao)
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/moradores/" + maria)
                        .header("X-Perfil", "OPERADOR")
                        .header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden());
    }

    // ---------- exclusão física ----------

    @Test
    void rf01_deleteNaoExisteEOCadastroPermaneceNoBanco() throws Exception {
        int id = cadastrar("Maria", "maria@exemplo.com", null);

        mockMvc.perform(delete("/moradores/" + id).header("X-Perfil", ADMIN))
                .andExpect(status().isMethodNotAllowed());

        assertEquals(1, contarMoradores());
    }

    // ---------- concorrência ----------

    @Test
    void rf01_cadastrosSimultaneosComOMesmoEmailCriamApenasUm() throws Exception {
        int tentativas = 8;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);

        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            Callable<Integer> tentativa = () -> {
                largada.await();
                return mockMvc.perform(post("/moradores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"nome": "Maria", "email": "corrida@exemplo.com"}
                                        """))
                        .andReturn().getResponse().getStatus();
            };
            resultados.add(pool.submit(tentativa));
        }
        largada.countDown();

        int criados = 0;
        int conflitos = 0;
        for (Future<Integer> resultado : resultados) {
            int codigo = resultado.get();
            if (codigo == 201) {
                criados++;
            } else if (codigo == 409) {
                conflitos++;
            }
        }
        pool.shutdown();

        assertEquals(1, criados, "exatamente um cadastro deve ser criado");
        assertEquals(tentativas - 1, conflitos, "os demais devem receber 409, nunca 500");
        assertEquals(1, contarMoradores());
    }

    // ---------- apoio ----------

    private int cadastrar(String nome, String email, String fone) throws Exception {
        String corpo = fone == null
                ? "{\"nome\": \"%s\", \"email\": \"%s\"}".formatted(nome, email)
                : "{\"nome\": \"%s\", \"email\": \"%s\", \"fone\": \"%s\"}".formatted(nome, email, fone);

        String resposta = mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private int contarMoradores() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta.mor_morador", Integer.class);
    }
}
