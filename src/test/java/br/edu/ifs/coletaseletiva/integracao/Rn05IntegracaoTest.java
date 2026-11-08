package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.ADMIN;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN05 sem concorrência (Issue #57): inativar um ponto, ou remover um vínculo de material, é bloqueado enquanto houver
 * solicitação aberta afetada (SOLICITADA, AGENDADA ou EM_RECOLHIMENTO). As situações finais não bloqueiam, e o bloqueio
 * some depois que o atendimento é resolvido (recusado ou cancelado).
 */
class Rn05IntegracaoTest extends IntegracaoBase {

    private CenarioSolicitacao c;
    private int maria;
    private int endereco;
    private int pontoA;
    private int pontoB;
    private int papel;
    private int vidro;
    private int operadorA;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        endereco = c.endereco(maria);
        pontoA = c.ponto("Ponto A", true, "ATIVO");
        pontoB = c.ponto("Ponto B", true, "ATIVO");
        papel = c.material("Papel", "KG", "ATIVO");
        vidro = c.material("Vidro", "KG", "ATIVO");
        c.vincular(pontoA, papel, null);
        c.vincular(pontoA, vidro, null);
        c.vincular(pontoB, papel, null);
        operadorA = c.operador("op.a@exemplo.com", pontoA, "ATIVO");
    }

    // ---------- ajudantes ----------

    private int solicitar(int ponto, String itens) throws Exception {
        String resposta = mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(ponto, endereco, itens)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return CenarioSolicitacao.id(resposta);
    }

    private void situar(int solicitacao, String situacao) {
        jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);
    }

    private ResultActions atualizarPonto(int ponto, String nome, String situacao, String... idioma) throws Exception {
        var requisicao = put("/pontos-coleta/" + ponto).header("X-Perfil", ADMIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"" + nome + "\", \"recolhimentoDomiciliar\": true, \"situacao\": \"" + situacao + "\", "
                        + "\"endereco\": {\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"Centro\", "
                        + "\"municipio\": \"Lagarto\", \"uf\": \"SE\", \"cep\": \"49400000\"}}");
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions removerVinculo(int ponto, int material, String... idioma) throws Exception {
        var requisicao = delete("/pontos-coleta/" + ponto + "/materiais/" + material).header("X-Perfil", ADMIN);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private String situacaoDoPonto(int ponto) {
        return jdbc.queryForObject("SELECT poc_tx_situacao FROM coleta.poc_ponto_coleta WHERE poc_nr_id = ?", String.class, ponto);
    }

    private int vinculos(int ponto, int material) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM coleta.mac_material_aceito WHERE poc_nr_id = ? AND mat_nr_id = ?",
                Integer.class, ponto, material);
    }

    // ---------- inativação do ponto ----------

    @Test
    void rn05_inativarPontoComSolicitacaoAbertaEmQualquerSituacaoAbertaRetorna409ENadaMuda() throws Exception {
        int solicitacao = solicitar(pontoA, item(papel, "1"));

        for (String situacao : List.of("SOLICITADA", "AGENDADA", "EM_RECOLHIMENTO")) {
            situar(solicitacao, situacao);

            atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                    .andExpect(jsonPath("$.mensagem").value("Não é possível inativar o ponto: ele possui 1 solicitação aberta. "
                            + "Recuse ou cancele os atendimentos antes de inativá-lo."));
            assertEquals("ATIVO", situacaoDoPonto(pontoA), "o ponto continua ativo com a solicitação " + situacao);
        }
    }

    @Test
    void rn05_amensagemInformaAQuantidadeDeSolicitacoesAbertasNoSingularENoPluralNosDoisIdiomas() throws Exception {
        solicitar(pontoA, item(papel, "1"));
        solicitar(pontoA, item(vidro, "1"));

        atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não é possível inativar o ponto: ele possui 2 solicitações abertas. "
                        + "Recuse ou cancele os atendimentos antes de inativá-lo."));
        atualizarPonto(pontoA, "Ponto A", "INATIVO", "en").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("The collection point cannot be deactivated: it has 2 open requests. "
                        + "Reject or cancel them first."));
    }

    @Test
    void rn05_solicitacoesEmSituacaoFinalNaoBloqueiamAInativacao() throws Exception {
        int concluida = solicitar(pontoA, item(papel, "1"));
        int recusada = solicitar(pontoA, item(papel, "1"));
        int cancelada = solicitar(pontoA, item(papel, "1"));
        situar(concluida, "CONCLUIDA");
        situar(recusada, "RECUSADA");
        situar(cancelada, "CANCELADA");

        atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));

        assertEquals("INATIVO", situacaoDoPonto(pontoA));
    }

    @Test
    void rn05_depoisDeRecusarOuCancelarAsAbertasAInativacaoPassa() throws Exception {
        int paraRecusar = solicitar(pontoA, item(papel, "1"));
        int paraCancelar = solicitar(pontoA, item(vidro, "1"));
        atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isConflict());

        mockMvc.perform(post("/solicitacoes/" + paraRecusar + "/recusa").header("X-Perfil", "OPERADOR")
                        .header("X-Usuario-Id", String.valueOf(operadorA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Fora da área\", \"versao\": 0}"))
                .andExpect(status().isOk());
        atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não é possível inativar o ponto: ele possui 1 solicitação aberta. "
                        + "Recuse ou cancele os atendimentos antes de inativá-lo."));
        mockMvc.perform(post("/solicitacoes/" + paraCancelar + "/cancelamento").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Não estarei em casa\", \"versao\": 0}"))
                .andExpect(status().isOk());

        atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isOk());
        assertEquals("INATIVO", situacaoDoPonto(pontoA));
    }

    @Test
    void rn05_solicitacaoAbertaEmOutroPontoNaoBloqueiaAInativacao() throws Exception {
        solicitar(pontoB, item(papel, "1"));

        atualizarPonto(pontoA, "Ponto A", "INATIVO").andExpect(status().isOk());
        atualizarPonto(pontoB, "Ponto B", "INATIVO").andExpect(status().isConflict());
    }

    @Test
    void rn05_soAPassagemDeAtivoParaInativoEBloqueadaReativarEEditarOutrosDadosPassam() throws Exception {
        solicitar(pontoA, item(papel, "1"));

        // Editar o nome mantendo o ponto ativo, com abertas, é permitido.
        atualizarPonto(pontoA, "Ponto A Renomeado", "ATIVO").andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ponto A Renomeado"));
        // Um ponto já inativo (por SQL, com a solicitação ainda aberta) pode ser reativado e editado.
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO' WHERE poc_nr_id = ?", pontoA);
        atualizarPonto(pontoA, "Ponto A Renomeado", "INATIVO").andExpect(status().isOk());
        atualizarPonto(pontoA, "Ponto A Renomeado", "ATIVO").andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
    }

    // ---------- remoção do vínculo ----------

    @Test
    void rn05_removerVinculoComSolicitacaoAbertaDoPontoComOMaterialRetorna409ComNomeEQuantidadeENadaMuda() throws Exception {
        int solicitacao = solicitar(pontoA, item(papel, "1"));

        for (String situacao : List.of("SOLICITADA", "AGENDADA", "EM_RECOLHIMENTO")) {
            situar(solicitacao, situacao);

            removerVinculo(pontoA, papel).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                    .andExpect(jsonPath("$.mensagem").value("Não é possível remover o vínculo do material Papel: o ponto possui "
                            + "1 solicitação aberta com esse material. Recuse ou cancele os atendimentos antes de remover o vínculo."));
            assertEquals(1, vinculos(pontoA, papel));
        }
        solicitar(pontoA, item(papel, "2") + "," + item(vidro, "2"));
        removerVinculo(pontoA, papel, "en").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("The link to the material Papel cannot be removed: the collection point has "
                        + "2 open requests with this material. Reject or cancel them first."));
    }

    @Test
    void rn05_vinculoDeOutroMaterialOuSolicitacaoAbertaDeOutroPontoNaoBloqueiam() throws Exception {
        solicitar(pontoA, item(papel, "1"));   // só papel no ponto A
        solicitar(pontoB, item(papel, "1"));   // papel no ponto B

        // O vidro do ponto A não tem solicitação aberta com vidro.
        removerVinculo(pontoA, vidro).andExpect(status().isNoContent());
        assertEquals(0, vinculos(pontoA, vidro));
        // O papel do ponto B tem aberta, mas o papel do ponto A ainda é bloqueado pela sua.
        removerVinculo(pontoB, papel).andExpect(status().isConflict());
        removerVinculo(pontoA, papel).andExpect(status().isConflict());
    }

    @Test
    void rn05_solicitacoesFinaisNaoBloqueiamARemocaoEDepoisDeResolverAsAbertasElaPassa() throws Exception {
        int finalizada = solicitar(pontoA, item(papel, "1"));
        situar(finalizada, "CONCLUIDA");
        int aberta = solicitar(pontoA, item(papel, "1"));
        removerVinculo(pontoA, papel).andExpect(status().isConflict());

        mockMvc.perform(post("/solicitacoes/" + aberta + "/cancelamento").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Mudança de planos\", \"versao\": 0}"))
                .andExpect(status().isOk());

        removerVinculo(pontoA, papel).andExpect(status().isNoContent());
        assertEquals(0, vinculos(pontoA, papel));
    }

    @Test
    void rn05_vinculoInexistenteContinuaSendo404EOutrosPerfisContinuamRecebendo403() throws Exception {
        removerVinculo(pontoB, vidro).andExpect(status().isNotFound());
        removerVinculo(999999, papel).andExpect(status().isNotFound());

        mockMvc.perform(delete("/pontos-coleta/" + pontoA + "/materiais/" + papel).header("X-Perfil", "MORADOR")
                .header("X-Usuario-Id", String.valueOf(maria))).andExpect(status().isForbidden());
        // O corpo é válido: a validação do formato (400) vem antes da autorização, então um corpo vazio não chegaria ao 403.
        mockMvc.perform(put("/pontos-coleta/" + pontoA).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", String.valueOf(operadorA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"Ponto A\", \"recolhimentoDomiciliar\": true, \"situacao\": \"INATIVO\", \"endereco\": "
                        + "{\"logradouro\": \"Rua A\", \"numero\": \"1\", \"bairro\": \"Centro\", \"municipio\": \"Lagarto\", "
                        + "\"uf\": \"SE\", \"cep\": \"49400000\"}}"))
                .andExpect(status().isForbidden());
        assertEquals("ATIVO", situacaoDoPonto(pontoA));
    }
}
