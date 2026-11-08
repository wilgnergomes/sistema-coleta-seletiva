package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.ItemDTO;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.ConflitoVersaoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.form.AgendamentoForm;
import br.edu.ifs.coletaseletiva.service.TransicaoSolicitacaoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransicaoSolicitacaoController.class)
class TransicaoSolicitacaoControllerTest {

    private static final String VALIDO = "{\"dataHora\": \"2026-11-20T09:00:00-03:00\", \"versao\": 0}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransicaoSolicitacaoService service;

    private ResultActions agendar(String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/9/agendamento").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "5")
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    @Test
    void rf10_agendamentoValidoRetorna200ComASolicitacaoAgendada() throws Exception {
        OffsetDateTime dataHora = OffsetDateTime.parse("2026-11-20T09:00:00-03:00");
        when(service.agendar(any(Ator.class), eq(9), any(AgendamentoForm.class))).thenReturn(
                new SolicitacaoDetalheDTO(9, "SOL-20261107-ABC234", 1, 1, "Rua A, 1", SituacaoSolicitacao.AGENDADA,
                        OffsetDateTime.now(), dataHora, null, null, 1,
                        List.of(new ItemDTO(1, "Papel", Unidade.KG, new BigDecimal("5.5"), null)), List.of()));

        agendar(VALIDO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.agendadaPara").exists());

        ArgumentCaptor<AgendamentoForm> form = ArgumentCaptor.forClass(AgendamentoForm.class);
        verify(service).agendar(any(Ator.class), eq(9), form.capture());
        assertEquals(dataHora.toInstant(), form.getValue().dataHora().toInstant());
        assertEquals(0, form.getValue().versao());
    }

    @Test
    void rf10_corpoInvalidoRetorna400SemChamarOService() throws Exception {
        agendar("{\"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='dataHora')].mensagem")
                        .value("a data e a hora do agendamento são obrigatórias"));
        agendar("{\"dataHora\": \"2026-11-20T09:00:00-03:00\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')].mensagem").value("a versão é obrigatória"));
        agendar("{\"dataHora\": \"2026-11-20T09:00:00-03:00\", \"versao\": -1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        agendar("{\"dataHora\": \"amanhã\", \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        agendar("{\"versao\": 0}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='dataHora')].mensagem")
                        .value("the scheduling date and time are required"));

        verifyNoInteractions(service);
    }

    @Test
    void rf10_dataNoPassadoRetorna400NoCampoDataHoraNoIdiomaDaRequisicao() throws Exception {
        when(service.agendar(any(Ator.class), eq(9), any(AgendamentoForm.class)))
                .thenThrow(new ParametroInvalidoException("dataHora", "erro.solicitacao.agendamento-passado"));

        agendar(VALIDO).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("dataHora"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A data e a hora do agendamento devem ser futuras."));
        agendar(VALIDO, "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("The scheduling date and time must be in the future."));
    }

    @Test
    void rf10_violacoesRetornam403404E409() throws Exception {
        when(service.agendar(any(Ator.class), eq(9), any(AgendamentoForm.class)))
                .thenThrow(new AcessoNegadoException("erro.solicitacao.somente-operador"))
                .thenThrow(new NaoEncontradoException("erro.solicitacao.nao-encontrada"))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.transicao-invalida",
                        SituacaoSolicitacao.CANCELADA, SituacaoSolicitacao.AGENDADA))
                .thenThrow(new ConflitoVersaoException("erro.solicitacao.versao-desatualizada"));

        agendar(VALIDO).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        agendar(VALIDO).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        agendar(VALIDO).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A solicitação está CANCELADA e não pode passar para AGENDADA."));
        agendar(VALIDO).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }

    @Test
    void rf10_semCabecalhosDoAtorRetorna403() throws Exception {
        mockMvc.perform(post("/solicitacoes/9/agendamento").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
