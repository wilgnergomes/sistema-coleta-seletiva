package br.edu.ifs.coletaseletiva.integracao;

import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.repository.HistoricoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN06: a mudança de situação e o histórico são uma única transação. Se a gravação do histórico falhar depois de a
 * solicitação já ter sido enviada ao banco com a nova situação, o rollback desfaz a mudança.
 */
class AgendamentoRollbackIntegracaoTest extends IntegracaoBase {

    @MockitoSpyBean
    private HistoricoRepository historicoRepository;

    @Test
    void rf10_falhaNaGravacaoDoHistoricoDesfazAMudancaDeSituacao() throws Exception {
        CenarioSolicitacao c = new CenarioSolicitacao(mockMvc, jdbc);
        int maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        int ponto = c.ponto("Ponto A", true, "ATIVO");
        int papel = c.material("Papel", "KG", "ATIVO");
        c.vincular(ponto, papel, null);
        int operador = c.operador("op.a@exemplo.com", ponto, "ATIVO");
        int solicitacao = CenarioSolicitacao.id(mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(ponto, endereco, item(papel, "2"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        // A falha só é induzida depois da criação, que também grava histórico.
        doThrow(new IllegalStateException("falha induzida no histórico")).when(historicoRepository).save(any(Historico.class));
        String dataHora = OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString();

        mockMvc.perform(post("/solicitacoes/" + solicitacao + "/agendamento").header("X-Perfil", "OPERADOR")
                        .header("X-Usuario-Id", String.valueOf(operador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataHora\": \"" + dataHora + "\", \"versao\": 0}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        assertEquals("SOLICITADA", jdbc.queryForObject(
                "SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", String.class, solicitacao));
        assertNull(jdbc.queryForObject(
                "SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao));
        assertEquals(0, jdbc.queryForObject(
                "SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Integer.class, solicitacao));
        assertEquals(1, c.contar("his_historico"));
    }
}
