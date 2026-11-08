package br.edu.ifs.coletaseletiva.integracao;

import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.repository.HistoricoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de rollback exigido pelo RNF08 (RN06): se a gravação do histórico falhar depois de a solicitação e os
 * itens já terem sido enviados ao banco, a transação inteira é desfeita e nada fica gravado.
 */
class SolicitacaoRollbackIntegracaoTest extends IntegracaoBase {

    @MockitoSpyBean
    private HistoricoRepository historicoRepository;

    @Test
    void rf08_falhaNaGravacaoDoHistoricoDesfazSolicitacaoEItens() throws Exception {
        CenarioSolicitacao c = new CenarioSolicitacao(mockMvc, jdbc);
        int maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        int ponto = c.ponto("Ponto A", true, "ATIVO");
        int papel = c.material("Papel", "KG", "ATIVO");
        int vidro = c.material("Vidro", "KG", "ATIVO");
        c.vincular(ponto, papel, null);
        c.vincular(ponto, vidro, null);
        doThrow(new IllegalStateException("falha induzida no histórico")).when(historicoRepository).save(any(Historico.class));

        mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR").header("X-Usuario-Id", String.valueOf(maria))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(ponto, endereco, item(papel, "1") + "," + item(vidro, "2"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        assertEquals(0, c.contar("sol_solicitacao"));
        assertEquals(0, c.contar("iso_item_solicitacao"));
        assertEquals(0, c.contar("his_historico"));
    }
}
