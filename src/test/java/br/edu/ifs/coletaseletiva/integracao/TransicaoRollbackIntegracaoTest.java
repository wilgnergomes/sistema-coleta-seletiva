package br.edu.ifs.coletaseletiva.integracao;

import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.repository.HistoricoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

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
 * solicitação já ter sido enviada ao banco com a nova situação, o rollback desfaz a mudança. Cada ação de situação
 * (agendamento do RF10, recusa do RF11 e as próximas) ganha aqui o seu caso.
 */
class TransicaoRollbackIntegracaoTest extends IntegracaoBase {

    @MockitoSpyBean
    private HistoricoRepository historicoRepository;

    private CenarioSolicitacao c;
    private int operador;
    private int solicitacao;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        int maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        int ponto = c.ponto("Ponto A", true, "ATIVO");
        int papel = c.material("Papel", "KG", "ATIVO");
        c.vincular(ponto, papel, null);
        operador = c.operador("op.a@exemplo.com", ponto, "ATIVO");
        solicitacao = CenarioSolicitacao.id(mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(ponto, endereco, item(papel, "2"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        // A falha só é induzida depois da criação, que também grava histórico.
        doThrow(new IllegalStateException("falha induzida no histórico")).when(historicoRepository).save(any(Historico.class));
    }

    private ResultActions acao(String caminho, String corpo) throws Exception {
        return mockMvc.perform(post("/solicitacoes/" + solicitacao + "/" + caminho).header("X-Perfil", "OPERADOR")
                .header("X-Usuario-Id", String.valueOf(operador)).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private void nadaFoiGravado() {
        assertEquals("SOLICITADA", jdbc.queryForObject(
                "SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", String.class, solicitacao));
        assertNull(jdbc.queryForObject(
                "SELECT sol_dt_agendamento FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Object.class, solicitacao));
        assertEquals(0, jdbc.queryForObject(
                "SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?", Integer.class, solicitacao));
        assertEquals(1, c.contar("his_historico"));
    }

    @Test
    void rf10_falhaNaGravacaoDoHistoricoDesfazAMudancaDeSituacaoDoAgendamento() throws Exception {
        String dataHora = OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString();

        acao("agendamento", "{\"dataHora\": \"" + dataHora + "\", \"versao\": 0}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        nadaFoiGravado();
    }

    @Test
    void rf11_falhaNaGravacaoDoHistoricoDesfazARecusa() throws Exception {
        acao("recusa", "{\"motivo\": \"Fora da área de atendimento\", \"versao\": 0}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        nadaFoiGravado();
    }

    @Test
    void rf12_falhaNaGravacaoDoHistoricoDesfazOCancelamento() throws Exception {
        acao("cancelamento", "{\"motivo\": \"Não estarei em casa\", \"versao\": 0}")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        nadaFoiGravado();
    }
}
