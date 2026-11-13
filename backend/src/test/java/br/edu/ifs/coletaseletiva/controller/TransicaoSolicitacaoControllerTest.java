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
import br.edu.ifs.coletaseletiva.form.ConclusaoForm;
import br.edu.ifs.coletaseletiva.form.VersaoForm;
import br.edu.ifs.coletaseletiva.form.MotivoForm;
import br.edu.ifs.coletaseletiva.form.ReagendamentoForm;
import br.edu.ifs.coletaseletiva.form.FalhaForm;
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

    // ---------- RF11: recusa ----------

    private ResultActions recusar(String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/9/recusa").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "5")
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    @Test
    void rf11_recusaValidaRetorna200ERepassaOMotivoSemEspacosNasPontas() throws Exception {
        when(service.recusar(any(Ator.class), eq(9), any(MotivoForm.class))).thenReturn(
                new SolicitacaoDetalheDTO(9, "SOL-20261107-ABC234", 1, 1, "Rua A, 1", SituacaoSolicitacao.RECUSADA,
                        OffsetDateTime.now(), null, null, null, 1, List.of(), List.of()));

        recusar("{\"motivo\": \"  Fora da área de atendimento  \", \"versao\": 0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("RECUSADA"))
                .andExpect(jsonPath("$.versao").value(1));

        ArgumentCaptor<MotivoForm> form = ArgumentCaptor.forClass(MotivoForm.class);
        verify(service).recusar(any(Ator.class), eq(9), form.capture());
        assertEquals("Fora da área de atendimento", form.getValue().motivo());
    }

    @Test
    void rf11_motivoAusenteVazioOuSoComEspacosRetorna400NoCampoMotivoSemChamarOService() throws Exception {
        recusar("{\"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("o motivo é obrigatório"));
        recusar("{\"motivo\": \"\", \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        recusar("{\"motivo\": \"     \", \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        recusar("{\"motivo\": null, \"versao\": 0}").andExpect(status().isBadRequest());
        recusar("{\"versao\": 0}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("the reason is required"));

        verifyNoInteractions(service);
    }

    @Test
    void rf11_motivoComMaisDe500CaracteresEVersaoInvalidaRetornam400() throws Exception {
        recusar("{\"motivo\": \"" + "a".repeat(501) + "\", \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("deve ter no máximo 500 caracteres"));
        recusar("{\"motivo\": \"" + "a".repeat(500) + "\", \"versao\": 0}").andExpect(status().isOk());
        recusar("{\"motivo\": \"Sem condições\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        recusar("{\"motivo\": \"Sem condições\", \"versao\": -1}").andExpect(status().isBadRequest());
    }

    @Test
    void rf11_violacoesRetornam403404E409() throws Exception {
        when(service.recusar(any(Ator.class), eq(9), any(MotivoForm.class)))
                .thenThrow(new AcessoNegadoException("erro.solicitacao.somente-operador"))
                .thenThrow(new NaoEncontradoException("erro.solicitacao.nao-encontrada"))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.transicao-invalida",
                        SituacaoSolicitacao.AGENDADA, SituacaoSolicitacao.RECUSADA))
                .thenThrow(new ConflitoVersaoException("erro.solicitacao.versao-desatualizada"));
        String corpo = "{\"motivo\": \"Sem condições\", \"versao\": 0}";

        recusar(corpo).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        recusar(corpo).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        recusar(corpo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está AGENDADA e não pode passar para RECUSADA."));
        recusar(corpo).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }

    // ---------- RF12: cancelamento ----------

    private ResultActions cancelar(String perfil, String usuarioId, String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/9/cancelamento").header("X-Perfil", perfil)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", usuarioId);
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    @Test
    void rf12_moradorEOperadorCancelamComMotivoSemEspacosNasPontas() throws Exception {
        when(service.cancelar(any(Ator.class), eq(9), any(MotivoForm.class))).thenReturn(
                new SolicitacaoDetalheDTO(9, "SOL-20261107-ABC234", 1, 1, "Rua A, 1", SituacaoSolicitacao.CANCELADA,
                        OffsetDateTime.now(), null, null, null, 2, List.of(), List.of()));
        String corpo = "{\"motivo\": \"  Não estarei em casa  \", \"versao\": 1}";

        cancelar("MORADOR", "3", corpo).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA")).andExpect(jsonPath("$.versao").value(2));
        cancelar("OPERADOR", "5", corpo).andExpect(status().isOk());

        ArgumentCaptor<MotivoForm> form = ArgumentCaptor.forClass(MotivoForm.class);
        ArgumentCaptor<Ator> ator = ArgumentCaptor.forClass(Ator.class);
        verify(service, org.mockito.Mockito.times(2)).cancelar(ator.capture(), eq(9), form.capture());
        assertEquals("Não estarei em casa", form.getValue().motivo());
        assertEquals(List.of(3, 5), ator.getAllValues().stream().map(Ator::usuarioId).toList());
    }

    @Test
    void rf12_motivoOuVersaoInvalidosRetornam400SemChamarOService() throws Exception {
        cancelar("MORADOR", "3", "{\"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("o motivo é obrigatório"));
        cancelar("MORADOR", "3", "{\"motivo\": \"     \", \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        cancelar("OPERADOR", "5", "{\"motivo\": \"" + "a".repeat(501) + "\", \"versao\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("deve ter no máximo 500 caracteres"));
        cancelar("MORADOR", "3", "{\"motivo\": \"Mudança\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        cancelar("MORADOR", "3", "{\"versao\": 0}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("the reason is required"));

        verifyNoInteractions(service);
    }

    @Test
    void rf12_violacoesRetornam403404E409() throws Exception {
        when(service.cancelar(any(Ator.class), eq(9), any(MotivoForm.class)))
                .thenThrow(new AcessoNegadoException("erro.solicitacao.cancelamento-sem-permissao"))
                .thenThrow(new NaoEncontradoException("erro.solicitacao.nao-encontrada"))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.transicao-invalida",
                        SituacaoSolicitacao.EM_RECOLHIMENTO, SituacaoSolicitacao.CANCELADA))
                .thenThrow(new ConflitoVersaoException("erro.solicitacao.versao-desatualizada"));
        String corpo = "{\"motivo\": \"Mudança\", \"versao\": 0}";

        cancelar("ADMINISTRADOR", null, corpo).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem").value("Apenas o morador da solicitação ou o operador do ponto pode cancelá-la."));
        cancelar("MORADOR", "3", corpo).andExpect(status().isNotFound());
        cancelar("MORADOR", "3", corpo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A solicitação está EM_RECOLHIMENTO e não pode passar para CANCELADA."));
        cancelar("OPERADOR", "5", corpo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }

    // ---------- RF13: início e conclusão ----------

    private ResultActions acaoDoOperador(String caminho, String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes/9/" + caminho).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "5")
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private static SolicitacaoDetalheDTO detalheEm(SituacaoSolicitacao situacao, int versao) {
        return new SolicitacaoDetalheDTO(9, "SOL-20261107-ABC234", 1, 1, "Rua A, 1", situacao,
                OffsetDateTime.now(), null, null, null, versao,
                List.of(new ItemDTO(1, "Papel", Unidade.KG, new BigDecimal("5.5"), new BigDecimal("5"))), List.of());
    }

    @Test
    void rf13_inicioValidoRetorna200ComEmRecolhimento() throws Exception {
        when(service.iniciar(any(Ator.class), eq(9), any(VersaoForm.class)))
                .thenReturn(detalheEm(SituacaoSolicitacao.EM_RECOLHIMENTO, 2));

        acaoDoOperador("inicio", "{\"versao\": 1}").andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_RECOLHIMENTO")).andExpect(jsonPath("$.versao").value(2));

        ArgumentCaptor<VersaoForm> form = ArgumentCaptor.forClass(VersaoForm.class);
        verify(service).iniciar(any(Ator.class), eq(9), form.capture());
        assertEquals(1, form.getValue().versao());
    }

    @Test
    void rf13_inicioSemVersaoOuComVersaoNegativaRetorna400SemChamarOService() throws Exception {
        acaoDoOperador("inicio", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')].mensagem").value("a versão é obrigatória"));
        acaoDoOperador("inicio", "{\"versao\": -1}").andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rf13_conclusaoValidaRetorna200ComConcluidaERepassaAsQuantidades() throws Exception {
        when(service.concluir(any(Ator.class), eq(9), any(ConclusaoForm.class)))
                .thenReturn(detalheEm(SituacaoSolicitacao.CONCLUIDA, 3));

        acaoDoOperador("conclusao", "{\"itens\": [{\"materialId\": 1, \"quantidadeEfetiva\": 5}, "
                + "{\"materialId\": 2, \"quantidadeEfetiva\": 0}], \"versao\": 2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.itens[0].quantidadeEfetiva").value(5));

        ArgumentCaptor<ConclusaoForm> form = ArgumentCaptor.forClass(ConclusaoForm.class);
        verify(service).concluir(any(Ator.class), eq(9), form.capture());
        assertEquals(2, form.getValue().itens().size());
        assertEquals(0, form.getValue().itens().get(1).quantidadeEfetiva().signum());
        assertEquals(2, form.getValue().versao());
    }

    @Test
    void rf13_conclusaoComListaOuQuantidadesInvalidasRetorna400SemChamarOService() throws Exception {
        acaoDoOperador("conclusao", "{\"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens')].mensagem").value("informe a quantidade efetiva de cada item"));
        acaoDoOperador("conclusao", "{\"itens\": [], \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens')]").exists());
        // O elemento nulo é 400 (achado C-04 da auditoria), e não 500.
        acaoDoOperador("conclusao", "{\"itens\": [null], \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0]')].mensagem").value("o item não pode ser nulo"));
        acaoDoOperador("conclusao", "{\"itens\": [{\"materialId\": 1}], \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEfetiva')].mensagem")
                        .value("a quantidade efetiva é obrigatória"));
        acaoDoOperador("conclusao", "{\"itens\": [{\"quantidadeEfetiva\": 1}], \"versao\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].materialId')]").exists());
        acaoDoOperador("conclusao", "{\"itens\": [{\"materialId\": 1, \"quantidadeEfetiva\": -0.5}], \"versao\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEfetiva')].mensagem")
                        .value("deve ser maior ou igual a zero"));
        acaoDoOperador("conclusao", "{\"itens\": [{\"materialId\": 1, \"quantidadeEfetiva\": 1.2345}], \"versao\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEfetiva')]").exists());
        acaoDoOperador("conclusao", "{\"itens\": [{\"materialId\": 1, \"quantidadeEfetiva\": 1000000000}], \"versao\": 0}")
                .andExpect(status().isBadRequest());
        acaoDoOperador("conclusao", "{\"itens\": [{\"materialId\": 1, \"quantidadeEfetiva\": 1}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        acaoDoOperador("conclusao", "{\"itens\": [null], \"versao\": 0}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0]')].mensagem").value("the item cannot be null"));

        verifyNoInteractions(service);
    }

    @Test
    void rf13_todasZeroRetorna400NoCampoItensEAsRegrasDeEstadoRetornam403404E409() throws Exception {
        String corpo = "{\"itens\": [{\"materialId\": 1, \"quantidadeEfetiva\": 0}], \"versao\": 0}";
        when(service.concluir(any(Ator.class), eq(9), any(ConclusaoForm.class)))
                .thenThrow(new ParametroInvalidoException("itens", "erro.solicitacao.conclusao-sem-quantidade-positiva"))
                .thenThrow(new AcessoNegadoException("erro.solicitacao.somente-operador"))
                .thenThrow(new NaoEncontradoException("erro.solicitacao.nao-encontrada"))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.conclusao-item-faltante", "Vidro"))
                .thenThrow(new ConflitoVersaoException("erro.solicitacao.versao-desatualizada"));

        acaoDoOperador("conclusao", corpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("itens"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Informe pelo menos uma quantidade efetiva maior que zero."));
        acaoDoOperador("conclusao", corpo).andExpect(status().isForbidden());
        acaoDoOperador("conclusao", corpo).andExpect(status().isNotFound());
        acaoDoOperador("conclusao", corpo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("Informe a quantidade efetiva de todos os itens. Faltam: Vidro."));
        acaoDoOperador("conclusao", corpo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }

    // ---------- RF14: reagendamento e tentativa sem sucesso ----------

    private static final String DATA = "2026-11-20T09:00:00-03:00";

    @Test
    void rf14_reagendamentoValidoRetorna200ERepassaMotivoSemEspacosEDataComOffset() throws Exception {
        when(service.reagendar(any(Ator.class), eq(9), any(ReagendamentoForm.class)))
                .thenReturn(detalheEm(SituacaoSolicitacao.AGENDADA, 2));

        acaoDoOperador("reagendamento", "{\"dataHora\": \"" + DATA + "\", \"motivo\": \"  Veículo em manutenção  \", \"versao\": 1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.versao").value(2));

        ArgumentCaptor<ReagendamentoForm> form = ArgumentCaptor.forClass(ReagendamentoForm.class);
        verify(service).reagendar(any(Ator.class), eq(9), form.capture());
        assertEquals("Veículo em manutenção", form.getValue().motivo());
        assertEquals(OffsetDateTime.parse(DATA).toInstant(), form.getValue().dataHora().toInstant());
        assertEquals(1, form.getValue().versao());
    }

    @Test
    void rf14_reagendamentoComCamposInvalidosRetorna400SemChamarOService() throws Exception {
        acaoDoOperador("reagendamento", "{\"motivo\": \"Motivo\", \"versao\": 1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='dataHora')].mensagem")
                        .value("a data e a hora do agendamento são obrigatórias"));
        acaoDoOperador("reagendamento", "{\"dataHora\": \"" + DATA + "\", \"versao\": 1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("o motivo é obrigatório"));
        acaoDoOperador("reagendamento", "{\"dataHora\": \"" + DATA + "\", \"motivo\": \"     \", \"versao\": 1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        acaoDoOperador("reagendamento", "{\"dataHora\": \"" + DATA + "\", \"motivo\": \"" + "a".repeat(501) + "\", \"versao\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("deve ter no máximo 500 caracteres"));
        acaoDoOperador("reagendamento", "{\"dataHora\": \"" + DATA + "\", \"motivo\": \"Motivo\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());
        acaoDoOperador("reagendamento", "{\"dataHora\": \"amanhã\", \"motivo\": \"Motivo\", \"versao\": 1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        acaoDoOperador("reagendamento", "{\"dataHora\": \"" + DATA + "\", \"versao\": 1}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("the reason is required"));

        verifyNoInteractions(service);
    }

    @Test
    void rf14_falhaValidaRetorna200ERepassaOsTextosSemEspacos() throws Exception {
        when(service.registrarFalha(any(Ator.class), eq(9), any(FalhaForm.class)))
                .thenReturn(detalheEm(SituacaoSolicitacao.AGENDADA, 3));

        acaoDoOperador("falha", "{\"motivo\": \"  Morador ausente  \", \"informacoesTentativa\": \"  Portão fechado  \", "
                + "\"dataHora\": \"" + DATA + "\", \"versao\": 2}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("AGENDADA"));

        ArgumentCaptor<FalhaForm> form = ArgumentCaptor.forClass(FalhaForm.class);
        verify(service).registrarFalha(any(Ator.class), eq(9), form.capture());
        assertEquals("Morador ausente", form.getValue().motivo());
        assertEquals("Portão fechado", form.getValue().informacoesTentativa());
        assertEquals(2, form.getValue().versao());
    }

    @Test
    void rf14_falhaSemInformacoesDaTentativaOuComCamposInvalidosRetorna400SemChamarOService() throws Exception {
        String base = "\"motivo\": \"Morador ausente\", \"dataHora\": \"" + DATA + "\", \"versao\": 2";
        acaoDoOperador("falha", "{" + base + "}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='informacoesTentativa')].mensagem")
                        .value("as informações da tentativa são obrigatórias"));
        acaoDoOperador("falha", "{\"informacoesTentativa\": \"\", " + base + "}").andExpect(status().isBadRequest());
        acaoDoOperador("falha", "{\"informacoesTentativa\": \"     \", " + base + "}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='informacoesTentativa')]").exists());
        acaoDoOperador("falha", "{\"informacoesTentativa\": \"" + "a".repeat(501) + "\", " + base + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='informacoesTentativa')].mensagem").value("deve ter no máximo 500 caracteres"));
        acaoDoOperador("falha", "{\"informacoesTentativa\": \"Portão fechado\", \"dataHora\": \"" + DATA + "\", \"versao\": 2}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        acaoDoOperador("falha", "{\"informacoesTentativa\": \"Portão fechado\", \"motivo\": \"Morador ausente\", \"versao\": 2}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[?(@.campo=='dataHora')]").exists());
        acaoDoOperador("falha", "{" + base + "}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='informacoesTentativa')].mensagem").value("the attempt information is required"));

        verifyNoInteractions(service);
    }

    @Test
    void rf14_regrasDoReagendamentoERetornam400403404E409() throws Exception {
        String reagendar = "{\"dataHora\": \"" + DATA + "\", \"motivo\": \"Motivo\", \"versao\": 1}";
        String falha = "{\"motivo\": \"Motivo\", \"informacoesTentativa\": \"Informações\", \"dataHora\": \"" + DATA + "\", \"versao\": 1}";
        when(service.reagendar(any(Ator.class), eq(9), any(ReagendamentoForm.class)))
                .thenThrow(new ParametroInvalidoException("dataHora", "erro.solicitacao.agendamento-passado"))
                .thenThrow(new AcessoNegadoException("erro.solicitacao.somente-operador"))
                .thenThrow(new NaoEncontradoException("erro.solicitacao.nao-encontrada"))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.reagendamento-mesma-data"))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.reagendamento-somente-agendada", SituacaoSolicitacao.SOLICITADA))
                .thenThrow(new ConflitoVersaoException("erro.solicitacao.versao-desatualizada"));
        when(service.registrarFalha(any(Ator.class), eq(9), any(FalhaForm.class)))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.falha-somente-em-recolhimento", SituacaoSolicitacao.AGENDADA));

        acaoDoOperador("reagendamento", reagendar).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("dataHora"));
        acaoDoOperador("reagendamento", reagendar).andExpect(status().isForbidden());
        acaoDoOperador("reagendamento", reagendar).andExpect(status().isNotFound());
        acaoDoOperador("reagendamento", reagendar).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A nova data deve ser diferente da data já marcada."));
        acaoDoOperador("reagendamento", reagendar).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Apenas uma solicitação AGENDADA pode ser reagendada. Situação atual: SOLICITADA."));
        acaoDoOperador("reagendamento", reagendar).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
        acaoDoOperador("falha", falha).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "A tentativa sem sucesso só pode ser registrada em uma solicitação EM_RECOLHIMENTO. Situação atual: AGENDADA."));
    }

    @Test
    void rf10_semCabecalhosDoAtorRetorna403() throws Exception {
        mockMvc.perform(post("/solicitacoes/9/agendamento").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
