package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF09 de ponta a ponta: filtros, paginação, contexto do ator e bordas do período (ADR 0014).
 *
 * <p>Cenário fixo: dois moradores (Maria e João), dois pontos (A e B) e quatro solicitações.
 * <pre>
 *   s1 Maria -> A  SOLICITADA  2026-10-10
 *   s2 Maria -> B  CANCELADA   2026-10-20
 *   s3 João  -> A  SOLICITADA  2026-10-25
 *   s4 João  -> B  AGENDADA    2026-11-01
 * </pre>
 */
class SolicitacaoConsultaIntegracaoTest extends IntegracaoBase {

    private int maria;
    private int joao;
    private int pontoA;
    private int pontoB;
    private int operadorA;
    private int operadorB;
    private int s1;
    private int s2;
    private int s3;
    private int s4;

    @BeforeEach
    void cenario() throws Exception {
        CenarioSolicitacao c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        joao = c.morador("joao@exemplo.com");
        int enderecoMaria = c.endereco(maria);
        int enderecoJoao = c.endereco(joao);
        pontoA = c.ponto("Ponto A", true, "ATIVO");
        pontoB = c.ponto("Ponto B", true, "ATIVO");
        int papel = c.material("Papel", "KG", "ATIVO");
        c.vincular(pontoA, papel, null);
        c.vincular(pontoB, papel, null);
        operadorA = c.operador("op.a@exemplo.com", pontoA, "ATIVO");
        operadorB = c.operador("op.b@exemplo.com", pontoB, "ATIVO");

        String itens = item(papel, "2");
        s1 = criar(maria, corpo(pontoA, enderecoMaria, itens));
        s2 = criar(maria, corpo(pontoB, enderecoMaria, itens));
        s3 = criar(joao, corpo(pontoA, enderecoJoao, itens));
        s4 = criar(joao, corpo(pontoB, enderecoJoao, itens));

        ajustar(s1, "SOLICITADA", "2026-10-10T15:00:00Z");
        ajustar(s2, "CANCELADA", "2026-10-20T15:00:00Z");
        ajustar(s3, "SOLICITADA", "2026-10-25T15:00:00Z");
        ajustar(s4, "AGENDADA", "2026-11-01T15:00:00Z");
    }

    private int criar(int moradorId, String corpo) throws Exception {
        String resposta = mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(moradorId))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return CenarioSolicitacao.id(resposta);
    }

    /** O teste controla a situação e a data de criação direto no banco, para montar o cenário. */
    private void ajustar(int id, String situacao, String criadaEm) {
        jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ?, sol_dt_criacao = ? WHERE sol_nr_id = ?",
                situacao, OffsetDateTime.parse(criadaEm), id);
    }

    private ResultActions consultar(String perfil, Integer usuarioId, String consulta, String... idioma) throws Exception {
        var requisicao = get("/solicitacoes" + consulta).header("X-Perfil", perfil);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", String.valueOf(usuarioId));
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private List<Integer> ids(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.conteudo[*].id");
    }

    // ---------- contexto do ator ----------

    @Test
    void rf09_administradorVeTodasDaMaisRecenteParaAMaisAntiga() throws Exception {
        assertEquals(List.of(s4, s3, s2, s1), ids(consultar("ADMINISTRADOR", null, "")));
        consultar("ADMINISTRADOR", null, "").andExpect(jsonPath("$.totalElementos").value(4));
    }

    @Test
    void rf09_moradorVeSomenteAsSuas() throws Exception {
        assertEquals(List.of(s2, s1), ids(consultar("MORADOR", maria, "")));
        assertEquals(List.of(s4, s3), ids(consultar("MORADOR", joao, "")));
    }

    @Test
    void rf09_operadorVeSomenteAsDoSeuPonto() throws Exception {
        assertEquals(List.of(s3, s1), ids(consultar("OPERADOR", operadorA, "")));
        assertEquals(List.of(s4, s2), ids(consultar("OPERADOR", operadorB, "")));
    }

    @Test
    void rf09_filtroNuncaAmpliaOContextoDoAtor() throws Exception {
        // Maria pede o ponto A: só a dela nesse ponto. O operador A pede o ponto B: nada, e não vaza.
        assertEquals(List.of(s1), ids(consultar("MORADOR", maria, "?pontoId=" + pontoA)));
        assertEquals(List.of(), ids(consultar("OPERADOR", operadorA, "?pontoId=" + pontoB)));
        assertEquals(List.of(s3), ids(consultar("OPERADOR", operadorA, "?pontoId=" + pontoA + "&de=2026-10-25")));
    }

    // ---------- filtros ----------

    @Test
    void rf09_filtraPorSituacaoPontoECombinaOsFiltros() throws Exception {
        assertEquals(List.of(s3, s1), ids(consultar("ADMINISTRADOR", null, "?situacao=SOLICITADA")));
        assertEquals(List.of(s2), ids(consultar("ADMINISTRADOR", null, "?situacao=CANCELADA")));
        assertEquals(List.of(), ids(consultar("ADMINISTRADOR", null, "?situacao=CONCLUIDA")));
        assertEquals(List.of(s4, s2), ids(consultar("ADMINISTRADOR", null, "?pontoId=" + pontoB)));
        assertEquals(List.of(s3), ids(consultar("ADMINISTRADOR", null, "?pontoId=" + pontoA + "&situacao=SOLICITADA&de=2026-10-20")));
    }

    @Test
    void rf09_filtraPorPeriodoComOsDoisDiasInclusive() throws Exception {
        assertEquals(List.of(s3, s2), ids(consultar("ADMINISTRADOR", null, "?de=2026-10-20&ate=2026-10-25")));
        assertEquals(List.of(s4), ids(consultar("ADMINISTRADOR", null, "?de=2026-11-01")));
        assertEquals(List.of(s2, s1), ids(consultar("ADMINISTRADOR", null, "?ate=2026-10-20")));
    }

    // ---------- bordas do período no fuso de negócio (ADR 0014) ----------

    @Test
    void rf09_bordasDoPeriodoUsamOFusoDeNegocioEIntervaloSemiaberto() throws Exception {
        // 2026-11-01 02:59:59Z = 31/10 23:59:59 em São Paulo (UTC-3): ainda é 31/10, embora em UTC já seja 01/11.
        ajustar(s1, "SOLICITADA", "2026-11-01T02:59:59Z");
        // 03:00:00Z = 01/11 00:00:00 em São Paulo: primeiro instante de 01/11.
        ajustar(s2, "SOLICITADA", "2026-11-01T03:00:00Z");
        // 2026-11-02 02:59:59Z = 01/11 23:59:59 em São Paulo: último instante de 01/11.
        ajustar(s3, "SOLICITADA", "2026-11-02T02:59:59Z");
        // 03:00:00Z = 02/11 00:00:00 em São Paulo: já é 02/11.
        ajustar(s4, "SOLICITADA", "2026-11-02T03:00:00Z");

        assertEquals(List.of(s3, s2), ids(consultar("ADMINISTRADOR", null, "?de=2026-11-01&ate=2026-11-01")));
        assertEquals(List.of(s1), ids(consultar("ADMINISTRADOR", null, "?ate=2026-10-31")));
        assertEquals(List.of(s4), ids(consultar("ADMINISTRADOR", null, "?de=2026-11-02")));
    }

    // ---------- paginação e formato ----------

    @Test
    void rf09_paginaOResultadoEInformaOsTotais() throws Exception {
        consultar("ADMINISTRADOR", null, "?size=3&page=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(s1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(3))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        assertEquals(List.of(), ids(consultar("ADMINISTRADOR", null, "?page=5")));
    }

    @Test
    void rf09_listaTrazResumoSemItensNemHistoricoEODetalheContinuaCompleto() throws Exception {
        consultar("ADMINISTRADOR", null, "?size=1")
                .andExpect(jsonPath("$.conteudo[0].protocolo").exists())
                .andExpect(jsonPath("$.conteudo[0].enderecoCopia").exists())
                .andExpect(jsonPath("$.conteudo[0].versao").exists())
                .andExpect(jsonPath("$.conteudo[0].itens").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].historico").doesNotExist());

        mockMvc.perform(get("/solicitacoes/" + s4).header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.historico.length()").value(1));
    }

    // ---------- entrada inválida (400) ----------

    @Test
    void rf09_parametrosInvalidosRetornam400() throws Exception {
        consultar("ADMINISTRADOR", null, "?de=2026-11-10&ate=2026-11-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("ate"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A data final não pode ser anterior à data inicial."));
        consultar("ADMINISTRADOR", null, "?situacao=XYZ")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
        consultar("ADMINISTRADOR", null, "?de=01/11/2026")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        consultar("ADMINISTRADOR", null, "?size=101").andExpect(status().isBadRequest());
        consultar("ADMINISTRADOR", null, "?page=-1").andExpect(status().isBadRequest());
    }

    @Test
    void rf09_periodoInvertidoSaiEmInglesComAcceptLanguage() throws Exception {
        consultar("ADMINISTRADOR", null, "?de=2026-11-10&ate=2026-11-01", "en")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("The end date cannot be earlier than the start date."));
    }

    // ---------- quem pode consultar (403) ----------

    @Test
    void rf09_semAtorOperadorInexistenteOuInativoEMoradorInativoRecebem403() throws Exception {
        mockMvc.perform(get("/solicitacoes")).andExpect(status().isForbidden());
        consultar("OPERADOR", 9999, "").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        jdbc.update("UPDATE coleta.ope_operador SET ope_tx_situacao = 'INATIVO' WHERE ope_nr_id = ?", operadorA);
        consultar("OPERADOR", operadorA, "").andExpect(status().isForbidden());

        jdbc.update("UPDATE coleta.mor_morador SET mor_tx_situacao = 'INATIVO' WHERE mor_nr_id = ?", maria);
        consultar("MORADOR", maria, "").andExpect(status().isForbidden());
        consultar("MORADOR", 9999, "").andExpect(status().isForbidden());
    }

    @Test
    void rf09_operadorAtivoContinuaConsultandoMesmoComOPontoInativo() throws Exception {
        // O ADR 0019 só exige operador existente e ativo; a situação do ponto não entra na regra.
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", pontoB);
        assertEquals(List.of(s4, s2), ids(consultar("OPERADOR", operadorB, "")));
    }
}
