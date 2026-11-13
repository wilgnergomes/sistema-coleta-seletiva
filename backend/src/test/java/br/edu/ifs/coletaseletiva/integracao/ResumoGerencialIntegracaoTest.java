package br.edu.ifs.coletaseletiva.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF15 de ponta a ponta: o resumo gerencial conta as solicitações por situação (pela data de criação) e soma as
 * quantidades <b>efetivas</b> recolhidas por material e unidade (pela data de conclusão), só das CONCLUIDA (ADR 0014).
 *
 * <p>Cenário (estimada ≠ efetiva de propósito, e dados "armadilha" que não podem entrar na soma):
 * <pre>
 *   s1 CONCLUIDA   criada 10/10, concluída 12/10  Papel(KG) est 10 efet 8,5 · Pilhas(UN) est 5 efet 4
 *   s2 CONCLUIDA   criada 20/10, concluída 02/11  Papel(KG) est 20 efet 20  · Vidro(KG) est 3 efet 0
 *   s3 EM_RECOLHIMENTO criada 25/10               Papel(KG) est 7 efet 99 (não concluída: não entra)
 *   s4 SOLICITADA  criada 03/11                   Vidro(KG) est 50 (sem efetiva)
 *   s5 CANCELADA   criada 05/11                   Papel(KG) est 1 efet 77 (cancelada: não entra)
 * </pre>
 */
class ResumoGerencialIntegracaoTest extends IntegracaoBase {

    private CenarioSolicitacao c;
    private int maria;
    private int endereco;
    private int ponto;
    private int papel;
    private int vidro;
    private int pilhas;
    private int s1;
    private int s2;
    private int s3;
    private int s4;
    private int s5;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        endereco = c.endereco(maria);
        ponto = c.ponto("Ponto A", true, "ATIVO");
        papel = c.material("Papel", "KG", "ATIVO");
        vidro = c.material("Vidro", "KG", "ATIVO");
        pilhas = c.material("Pilhas", "UN", "ATIVO");
        c.vincular(ponto, papel, null);
        c.vincular(ponto, vidro, null);
        c.vincular(ponto, pilhas, null);

        s1 = criar(item(papel, "10") + "," + item(pilhas, "5"));
        s2 = criar(item(papel, "20") + "," + item(vidro, "3"));
        s3 = criar(item(papel, "7"));
        s4 = criar(item(vidro, "50"));
        s5 = criar(item(papel, "1"));

        situar(s1, "CONCLUIDA", "2026-10-10T15:00:00Z", "2026-10-12T15:00:00Z");
        situar(s2, "CONCLUIDA", "2026-10-20T15:00:00Z", "2026-11-02T15:00:00Z");
        situar(s3, "EM_RECOLHIMENTO", "2026-10-25T15:00:00Z", null);
        situar(s4, "SOLICITADA", "2026-11-03T15:00:00Z", null);
        situar(s5, "CANCELADA", "2026-11-05T15:00:00Z", null);

        efetiva(s1, papel, "8.5");
        efetiva(s1, pilhas, "4");
        efetiva(s2, papel, "20");
        efetiva(s2, vidro, "0");
        efetiva(s3, papel, "99");
        efetiva(s5, papel, "77");
    }

    // ---------- ajudantes ----------

    private int criar(String itens) throws Exception {
        String resposta = mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(ponto, endereco, itens)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return CenarioSolicitacao.id(resposta);
    }

    private void situar(int id, String situacao, String criadaEm, String concluidaEm) {
        jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ?, sol_dt_criacao = ?, sol_dt_conclusao = ? "
                        + "WHERE sol_nr_id = ?", situacao, OffsetDateTime.parse(criadaEm),
                concluidaEm == null ? null : OffsetDateTime.parse(concluidaEm), id);
    }

    private void efetiva(int solicitacao, int material, String quantidade) {
        jdbc.update("UPDATE coleta.iso_item_solicitacao SET iso_nr_quantidade_efetiva = ? WHERE sol_nr_id = ? AND mat_nr_id = ?",
                new BigDecimal(quantidade), solicitacao, material);
    }

    private ResultActions resumo(String consulta) throws Exception {
        return mockMvc.perform(get("/resumo-gerencial" + consulta).header("X-Perfil", "ADMINISTRADOR"));
    }

    private String corpoDoResumo(String consulta) throws Exception {
        return resumo(consulta).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** Total da situação no primeiro bloco. */
    private long total(String json, String situacao) {
        List<Number> valores = JsonPath.read(json, "$.solicitacoesPorSituacao[?(@.situacao=='" + situacao + "')].total");
        assertEquals(1, valores.size(), "cada situação aparece exatamente uma vez");
        return valores.get(0).longValue();
    }

    /** Quantidade recolhida do material na unidade, ou null se não houver linha. Compara em BigDecimal. */
    private BigDecimal quantidade(String json, String material, String unidade) {
        List<Number> valores = JsonPath.read(json, "$.quantidadesRecolhidas[?(@.nomeMaterial=='" + material
                + "' && @.unidade=='" + unidade + "')].quantidade");
        assertTrue(valores.size() <= 1, "no máximo uma linha por material e unidade");
        return valores.isEmpty() ? null : new BigDecimal(valores.get(0).toString());
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas foi " + real);
    }

    // ---------- soma: só a efetiva das concluídas ----------

    @Test
    void rf15_somaSoAQuantidadeEfetivaDasConcluidasPorMaterialEUnidade() throws Exception {
        String json = corpoDoResumo("");

        // Papel: 8,5 (s1) + 20 (s2). Nada de s3 (EM_RECOLHIMENTO, 99) nem de s5 (CANCELADA, 77) e nada de estimadas.
        igual("28.5", quantidade(json, "Papel", "KG"));
        igual("4", quantidade(json, "Pilhas", "UN"));
        // Vidro: o item de s2 com efetiva 0 conta como 0; o de s4 (sem efetiva) não entra.
        igual("0", quantidade(json, "Vidro", "KG"));
        assertEquals(3, ((List<?>) JsonPath.read(json, "$.quantidadesRecolhidas")).size());
    }

    @Test
    void rf15_materiaisEmUnidadesDiferentesFicamEmLinhasSeparadasSemTotalGeral() throws Exception {
        resumo("").andExpect(jsonPath("$.quantidadesRecolhidas.length()").value(3))
                .andExpect(jsonPath("$.quantidadesRecolhidas[0].nomeMaterial").value("Papel"))
                .andExpect(jsonPath("$.quantidadesRecolhidas[0].unidade").value("KG"))
                .andExpect(jsonPath("$.quantidadesRecolhidas[1].nomeMaterial").value("Pilhas"))
                .andExpect(jsonPath("$.quantidadesRecolhidas[1].unidade").value("UN"))
                .andExpect(jsonPath("$.quantidadesRecolhidas[2].nomeMaterial").value("Vidro"))
                // O resumo não tem nenhum campo de total geral que misture kg com unidades.
                .andExpect(jsonPath("$.total").doesNotExist())
                .andExpect(jsonPath("$.quantidadeTotal").doesNotExist());
    }

    @Test
    void rf15_aUnidadeCopiadaNoItemEQuemAgrupaMesmoSeOCadastroDoMaterialMudar() throws Exception {
        // O cadastro do papel passa a ser em L, mas as cópias nos itens continuam KG (ADR 0008).
        jdbc.update("UPDATE coleta.mat_material SET mat_tx_unidade = 'L' WHERE mat_nr_id = ?", papel);

        String json = corpoDoResumo("");

        igual("28.5", quantidade(json, "Papel", "KG"));
        assertEquals(null, quantidade(json, "Papel", "L"));
    }

    // ---------- contagem por situação ----------

    @Test
    void rf15_contaTodasAsSituacoesNaOrdemDoEnumInclusiveComZero() throws Exception {
        resumo("").andExpect(jsonPath("$.solicitacoesPorSituacao.length()").value(6))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[0].situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[0].total").value(1))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[1].situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[1].total").value(0))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[2].situacao").value("EM_RECOLHIMENTO"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[2].total").value(1))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[3].situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[3].total").value(2))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[4].situacao").value("RECUSADA"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[4].total").value(0))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[5].situacao").value("CANCELADA"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[5].total").value(1));
    }

    @Test
    void rf15_semNenhumaOcorrenciaNoPeriodoDevolveSeisZerosEListaVazia() throws Exception {
        resumo("?de=2030-01-01").andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitacoesPorSituacao.length()").value(6))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[?(@.total != 0)]").isEmpty())
                .andExpect(jsonPath("$.quantidadesRecolhidas").isEmpty());
    }

    // ---------- períodos: cada indicador usa a sua data (ADR 0014) ----------

    @Test
    void rf15_aContagemUsaADataDeCriacaoEAsQuantidadesUsamADataDeConclusao() throws Exception {
        // Outubro inteiro. Criadas em outubro: s1, s2 (CONCLUIDA) e s3 (EM_RECOLHIMENTO).
        // Concluídas em outubro: só s1 (s2 foi concluída em 02/11, embora criada em outubro).
        String outubro = corpoDoResumo("?de=2026-10-01&ate=2026-10-31");
        assertEquals(2, total(outubro, "CONCLUIDA"), "pela criação, s1 e s2 entram em outubro");
        assertEquals(1, total(outubro, "EM_RECOLHIMENTO"));
        assertEquals(0, total(outubro, "SOLICITADA"));
        igual("8.5", quantidade(outubro, "Papel", "KG"));
        igual("4", quantidade(outubro, "Pilhas", "UN"));
        assertEquals(null, quantidade(outubro, "Vidro", "KG"), "o vidro de s2 foi concluído só em novembro");

        // Novembro inteiro. Criadas em novembro: s4 (SOLICITADA) e s5 (CANCELADA). Concluída em novembro: s2.
        String novembro = corpoDoResumo("?de=2026-11-01&ate=2026-11-30");
        assertEquals(1, total(novembro, "SOLICITADA"));
        assertEquals(1, total(novembro, "CANCELADA"));
        assertEquals(0, total(novembro, "CONCLUIDA"), "nenhuma foi criada em novembro");
        igual("20", quantidade(novembro, "Papel", "KG"));
        igual("0", quantidade(novembro, "Vidro", "KG"));
        assertEquals(null, quantidade(novembro, "Pilhas", "UN"));
    }

    @Test
    void rf15_soInicioOuSoFimFiltraApenasUmLado() throws Exception {
        String aPartirDe = corpoDoResumo("?de=2026-11-01");
        igual("20", quantidade(aPartirDe, "Papel", "KG"));
        assertEquals(2, total(aPartirDe, "SOLICITADA") + total(aPartirDe, "CANCELADA"));

        String ate = corpoDoResumo("?ate=2026-10-31");
        igual("8.5", quantidade(ate, "Papel", "KG"));
        assertEquals(0, total(ate, "CANCELADA"));
    }

    @Test
    void rf15_bordasDoPeriodoUsamOFusoDeNegocioEIntervaloSemiaberto() throws Exception {
        // 02:59:59Z de 01/11 ainda é 31/10 em São Paulo (UTC-3); 03:00:00Z é 00h00 de 01/11.
        situar(s1, "CONCLUIDA", "2026-11-01T02:59:59Z", "2026-11-01T02:59:59Z");
        situar(s2, "CONCLUIDA", "2026-11-01T03:00:00Z", "2026-11-01T03:00:00Z");

        // Só o dia 01/11: entra s2 (00h00 em São Paulo), e não s1 (23h59 de 31/10), embora em UTC ambas sejam 01/11.
        String primeiroDeNovembro = corpoDoResumo("?de=2026-11-01&ate=2026-11-01");
        assertEquals(1, total(primeiroDeNovembro, "CONCLUIDA"));
        igual("20", quantidade(primeiroDeNovembro, "Papel", "KG"));
        igual("0", quantidade(primeiroDeNovembro, "Vidro", "KG"));
        assertEquals(null, quantidade(primeiroDeNovembro, "Pilhas", "UN"));

        // Até 31/10: entra s1, e não s2.
        String ateTrintaEUm = corpoDoResumo("?de=2026-10-31&ate=2026-10-31");
        assertEquals(1, total(ateTrintaEUm, "CONCLUIDA"));
        igual("8.5", quantidade(ateTrintaEUm, "Papel", "KG"));
        igual("4", quantidade(ateTrintaEUm, "Pilhas", "UN"));
        assertEquals(null, quantidade(ateTrintaEUm, "Vidro", "KG"));
    }

    // ---------- entrada inválida e permissões ----------

    @Test
    void rf15_datasInvalidasRetornam400NoCampoCorrespondente() throws Exception {
        resumo("?de=01/11/2026").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("de"));
        resumo("?ate=2026-13-45").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[0].campo").value("ate"));
        resumo("?de=2026-11-10&ate=2026-11-01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("ate"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A data final não pode ser anterior à data inicial."));
        mockMvc.perform(get("/resumo-gerencial?de=2026-11-10&ate=2026-11-01").header("X-Perfil", "ADMINISTRADOR")
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("The end date cannot be earlier than the start date."));
        // De e até iguais é um dia só, e vale.
        resumo("?de=2026-10-12&ate=2026-10-12").andExpect(status().isOk());
    }

    @Test
    void rf15_soOAdministradorConsultaMoradorOperadorESemAtorRecebem403() throws Exception {
        int operador = c.operador("op.a@exemplo.com", ponto, "ATIVO");

        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem").value("Apenas o administrador pode consultar o resumo gerencial."));
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(operador)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/resumo-gerencial")).andExpect(status().isForbidden());
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf15_consultarOResumoNaoAlteraNadaNoBanco() throws Exception {
        int solicitacoesAntes = c.contar("sol_solicitacao");
        int itensAntes = c.contar("iso_item_solicitacao");

        corpoDoResumo("");
        corpoDoResumo("?de=2026-10-01&ate=2026-10-31");

        assertEquals(solicitacoesAntes, c.contar("sol_solicitacao"));
        assertEquals(itensAntes, c.contar("iso_item_solicitacao"));
        assertEquals(5, solicitacoesAntes);
    }
}
