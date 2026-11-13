package br.edu.ifs.coletaseletiva.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.corpo;
import static br.edu.ifs.coletaseletiva.integracao.CenarioSolicitacao.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF14 de ponta a ponta: o reagendamento (AGENDADA -> AGENDADA) e a tentativa sem sucesso (EM_RECOLHIMENTO ->
 * AGENDADA). Cada tentativa e cada alteração acrescentam uma linha ao histórico e as anteriores permanecem
 * (cenário "Tentativa sem sucesso" do enunciado). A versão sobe a cada alteração válida.
 */
class ReagendamentoIntegracaoTest extends IntegracaoBase {

    private static final String MOTIVO = "Veículo em manutenção";
    private static final String TENTATIVA = "Portão fechado, ninguém atendeu";

    private CenarioSolicitacao c;
    private int maria;
    private int operadorA;
    private int operadorB;
    private int papel;
    private int solicitacao;
    private OffsetDateTime primeiraData;

    @BeforeEach
    void cenario() throws Exception {
        c = new CenarioSolicitacao(mockMvc, jdbc);
        maria = c.morador("maria@exemplo.com");
        int endereco = c.endereco(maria);
        int pontoA = c.ponto("Ponto A", true, "ATIVO");
        int pontoB = c.ponto("Ponto B", true, "ATIVO");
        papel = c.material("Papel", "KG", "ATIVO");
        c.vincular(pontoA, papel, null);
        c.vincular(pontoB, papel, null);
        operadorA = c.operador("op.a@exemplo.com", pontoA, "ATIVO");
        operadorB = c.operador("op.b@exemplo.com", pontoB, "ATIVO");
        primeiraData = dia(2);

        String resposta = mockMvc.perform(post("/solicitacoes").header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(pontoA, endereco, item(papel, "2"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        solicitacao = CenarioSolicitacao.id(resposta);
    }

    // ---------- ajudantes ----------

    /** Daqui a N dias, em UTC e sem frações de segundo. */
    private static OffsetDateTime dia(int dias) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(dias).truncatedTo(ChronoUnit.SECONDS);
    }

    private ResultActions acao(String perfil, Integer usuarioId, int id, String caminho, String corpo, String... idioma)
            throws Exception {
        var requisicao = post("/solicitacoes/" + id + "/" + caminho).header("X-Perfil", perfil)
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", String.valueOf(usuarioId));
        }
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions comoOperadorA(String caminho, String corpo) throws Exception {
        return acao("OPERADOR", operadorA, solicitacao, caminho, corpo);
    }

    private static String versao(int versao) {
        return "{\"versao\": " + versao + "}";
    }

    private static String reagendamento(OffsetDateTime dataHora, String motivo, int versao) {
        String texto = motivo == null ? "null" : "\"" + motivo + "\"";
        return "{\"dataHora\": \"" + dataHora + "\", \"motivo\": " + texto + ", \"versao\": " + versao + "}";
    }

    private static String falha(OffsetDateTime dataHora, String motivo, String informacoes, int versao) {
        String m = motivo == null ? "null" : "\"" + motivo + "\"";
        String i = informacoes == null ? "null" : "\"" + informacoes + "\"";
        return "{\"motivo\": " + m + ", \"informacoesTentativa\": " + i + ", \"dataHora\": \"" + dataHora + "\", \"versao\": "
                + versao + "}";
    }

    /** SOLICITADA -> AGENDADA para a primeira data. A versão passa a 1. */
    private void agendar() throws Exception {
        comoOperadorA("agendamento", "{\"dataHora\": \"" + primeiraData + "\", \"versao\": 0}").andExpect(status().isOk());
    }

    /** SOLICITADA -> AGENDADA -> EM_RECOLHIMENTO, pela própria API. A versão passa a 2. */
    private void emRecolhimento() throws Exception {
        agendar();
        comoOperadorA("inicio", versao(1)).andExpect(status().isOk());
    }

    private String situacaoNoBanco() {
        return jdbc.queryForObject("SELECT sol_tx_situacao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                String.class, solicitacao);
    }

    private int versaoNoBanco() {
        return jdbc.queryForObject("SELECT sol_nr_versao FROM coleta.sol_solicitacao WHERE sol_nr_id = ?",
                Integer.class, solicitacao);
    }

    private boolean agendadaParaNoBanco(OffsetDateTime esperada) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.sol_solicitacao WHERE sol_nr_id = ? AND sol_dt_agendamento = ?",
                Integer.class, solicitacao, esperada) == 1;
    }

    // ---------- reagendamento ----------

    @Test
    void rf14_reagendarMantemAgendadaSobeAVersaoEAcrescentaUmaLinhaSemApagarOAgendamentoAnterior() throws Exception {
        agendar();
        OffsetDateTime nova = dia(5);

        acao("OPERADOR", operadorA, solicitacao, "reagendamento", reagendamento(nova, "  " + MOTIVO + "  ", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.agendadaPara").exists())
                .andExpect(jsonPath("$.historico.length()").value(3))
                // A linha do agendamento original continua como estava, com a primeira data.
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[1].agendamento").exists())
                .andExpect(jsonPath("$.historico[2].situacaoAnterior").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[2].situacaoNova").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[2].motivo").value(MOTIVO))
                .andExpect(jsonPath("$.historico[2].responsavel.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[2].responsavel.id").value(operadorA));

        assertEquals("AGENDADA", situacaoNoBanco());
        assertEquals(2, versaoNoBanco());
        assertEquals(true, agendadaParaNoBanco(nova));
        assertEquals(3, c.contar("his_historico"));
        // As duas datas ficam registradas, cada uma na sua linha de histórico.
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE his_dt_agendamento = ?", Integer.class, primeiraData));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE his_dt_agendamento = ? AND his_tx_motivo = ?",
                Integer.class, nova, MOTIVO));
    }

    @Test
    void rf14_reagendamentosEmSerieSomamUmaVersaoECadaUmDeixaASuaLinha() throws Exception {
        agendar();
        OffsetDateTime segunda = dia(4);
        OffsetDateTime terceira = dia(6);

        comoOperadorA("reagendamento", reagendamento(segunda, "Primeiro motivo", 1)).andExpect(status().isOk());
        comoOperadorA("reagendamento", reagendamento(terceira, "Segundo motivo", 2)).andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(3))
                .andExpect(jsonPath("$.historico.length()").value(4));

        assertEquals(true, agendadaParaNoBanco(terceira));
        assertEquals(3, versaoNoBanco());
        assertEquals(List.of("Primeiro motivo", "Segundo motivo"), jdbc.queryForList(
                "SELECT his_tx_motivo FROM coleta.his_historico WHERE his_tx_motivo IS NOT NULL ORDER BY his_nr_id", String.class));
    }

    @Test
    void rf14_reagendarParaAMesmaDataRetorna409MesmoEscritaEmOutroFusoENadaMuda() throws Exception {
        agendar();

        comoOperadorA("reagendamento", reagendamento(primeiraData, MOTIVO, 1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A nova data deve ser diferente da data já marcada."));
        acao("OPERADOR", operadorA, solicitacao, "reagendamento",
                reagendamento(primeiraData.withOffsetSameInstant(ZoneOffset.ofHours(-3)), MOTIVO, 1), "en")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("The new date must be different from the date already set."));

        assertEquals(1, versaoNoBanco());
        assertEquals(2, c.contar("his_historico"));
        assertEquals(true, agendadaParaNoBanco(primeiraData));
    }

    @Test
    void rf14_dataNoPassadoRetorna400NoCampoDataHoraENadaMuda() throws Exception {
        agendar();

        comoOperadorA("reagendamento", reagendamento(dia(-1), MOTIVO, 1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("dataHora"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A data e a hora do agendamento devem ser futuras."));

        assertEquals(1, versaoNoBanco());
        assertEquals(2, c.contar("his_historico"));
        assertEquals(true, agendadaParaNoBanco(primeiraData));
    }

    @Test
    void rf14_reagendamentoSemMotivoOuComCamposInvalidosRetorna400ENadaMuda() throws Exception {
        agendar();

        comoOperadorA("reagendamento", reagendamento(dia(5), null, 1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')].mensagem").value("o motivo é obrigatório"));
        comoOperadorA("reagendamento", reagendamento(dia(5), "     ", 1)).andExpect(status().isBadRequest());
        comoOperadorA("reagendamento", reagendamento(dia(5), "a".repeat(501), 1)).andExpect(status().isBadRequest());
        comoOperadorA("reagendamento", "{\"motivo\": \"Motivo\", \"versao\": 1}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='dataHora')]").exists());
        comoOperadorA("reagendamento", "{\"dataHora\": \"" + dia(5) + "\", \"motivo\": \"Motivo\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[?(@.campo=='versao')]").exists());

        assertEquals(1, versaoNoBanco());
        assertEquals(2, c.contar("his_historico"));
    }

    @Test
    void rf14_reagendarForaDeAgendadaRetorna409ComAMensagemDaSituacaoAtual() throws Exception {
        for (String situacao : List.of("SOLICITADA", "EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            comoOperadorA("reagendamento", reagendamento(dia(5), MOTIVO, 0)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value(
                            "Apenas uma solicitação AGENDADA pode ser reagendada. Situação atual: " + situacao + "."));
        }

        assertEquals(1, c.contar("his_historico"));
    }

    @Test
    void rf14_reagendamentoComVersaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda() throws Exception {
        agendar();

        comoOperadorA("reagendamento", reagendamento(dia(5), MOTIVO, 0)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        assertEquals(1, versaoNoBanco());
        assertEquals(2, c.contar("his_historico"));
    }

    // ---------- tentativa sem sucesso: cenário do enunciado ----------

    @Test
    void rf14_tentativaSemSucessoPreservaATentativaAnteriorEPermiteIniciarEConcluirDepois() throws Exception {
        emRecolhimento();
        OffsetDateTime nova = dia(7);

        comoOperadorA("falha", falha(nova, "  Morador ausente  ", "  " + TENTATIVA + "  ", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.versao").value(3))
                .andExpect(jsonPath("$.historico.length()").value(4))
                // A tentativa (o início em EM_RECOLHIMENTO) continua no histórico, antes da linha da falha.
                .andExpect(jsonPath("$.historico[2].situacaoNova").value("EM_RECOLHIMENTO"))
                .andExpect(jsonPath("$.historico[3].situacaoAnterior").value("EM_RECOLHIMENTO"))
                .andExpect(jsonPath("$.historico[3].situacaoNova").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[3].motivo").value("Morador ausente"))
                .andExpect(jsonPath("$.historico[3].informacoesTentativa").value(TENTATIVA))
                .andExpect(jsonPath("$.historico[3].agendamento").exists())
                .andExpect(jsonPath("$.historico[3].responsavel.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[3].responsavel.id").value(operadorA));

        assertEquals("AGENDADA", situacaoNoBanco());
        assertEquals(true, agendadaParaNoBanco(nova));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM coleta.his_historico WHERE his_tx_informacoes_tentativa = ? AND ope_nr_id = ?",
                Integer.class, TENTATIVA, operadorA));

        // Depois da falha a solicitação volta ao fluxo: inicia de novo e conclui.
        comoOperadorA("inicio", versao(3)).andExpect(status().isOk());
        comoOperadorA("conclusao", "{\"itens\": [{\"materialId\": " + papel + ", \"quantidadeEfetiva\": 2}], \"versao\": 4}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.versao").value(5))
                // criação, agendamento, início, falha, novo início e conclusão
                .andExpect(jsonPath("$.historico.length()").value(6));

        mockMvc.perform(get("/solicitacoes/" + solicitacao).header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", String.valueOf(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historico[3].informacoesTentativa").value(TENTATIVA));
    }

    @Test
    void rf14_naFalhaANovaDataPodeSerAMesmaQueJaEstavaMarcadaPoisASituacaoMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("falha", falha(primeiraData, "Morador ausente", TENTATIVA, 2)).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.versao").value(3));

        assertEquals(true, agendadaParaNoBanco(primeiraData));
        assertEquals(4, c.contar("his_historico"));
    }

    @Test
    void rf14_falhaSemInformacoesDaTentativaOuComCamposInvalidosRetorna400ENadaMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("falha", falha(dia(7), "Morador ausente", null, 2)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='informacoesTentativa')].mensagem")
                        .value("as informações da tentativa são obrigatórias"));
        comoOperadorA("falha", falha(dia(7), "Morador ausente", "     ", 2)).andExpect(status().isBadRequest());
        comoOperadorA("falha", falha(dia(7), "Morador ausente", "a".repeat(501), 2)).andExpect(status().isBadRequest());
        comoOperadorA("falha", falha(dia(7), null, TENTATIVA, 2)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='motivo')]").exists());
        comoOperadorA("falha", falha(dia(7), "     ", TENTATIVA, 2)).andExpect(status().isBadRequest());
        comoOperadorA("falha", "{\"motivo\": \"Morador ausente\", \"informacoesTentativa\": \"" + TENTATIVA + "\", \"versao\": 2}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[?(@.campo=='dataHora')]").exists());
        acao("OPERADOR", operadorA, solicitacao, "falha", falha(dia(7), "Morador ausente", null, 2), "en")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='informacoesTentativa')].mensagem")
                        .value("the attempt information is required"));

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(2, versaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
    }

    @Test
    void rf14_falhaComDataNoPassadoRetorna400NoCampoDataHoraENadaMuda() throws Exception {
        emRecolhimento();

        comoOperadorA("falha", falha(dia(-1), "Morador ausente", TENTATIVA, 2)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("dataHora"));

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
    }

    @Test
    void rf14_falhaForaDeEmRecolhimentoRetorna409EVersaoAntigaRetorna409DeVersao() throws Exception {
        for (String situacao : List.of("SOLICITADA", "AGENDADA", "CONCLUIDA", "RECUSADA", "CANCELADA")) {
            jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = ? WHERE sol_nr_id = ?", situacao, solicitacao);

            comoOperadorA("falha", falha(dia(7), "Motivo", TENTATIVA, 0)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value(
                            "A tentativa sem sucesso só pode ser registrada em uma solicitação EM_RECOLHIMENTO. Situação atual: "
                                    + situacao + "."));
        }
        jdbc.update("UPDATE coleta.sol_solicitacao SET sol_tx_situacao = 'EM_RECOLHIMENTO' WHERE sol_nr_id = ?", solicitacao);
        comoOperadorA("falha", falha(dia(7), "Motivo", TENTATIVA, 9)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));

        assertEquals(1, c.contar("his_historico"));
    }

    // ---------- quem pode (403) e o que enxerga (404) ----------

    @Test
    void rf14_operadorDeOutroPontoRecebe404NasDuasAcoesENadaMuda() throws Exception {
        emRecolhimento();

        acao("OPERADOR", operadorB, solicitacao, "falha", falha(dia(7), "Motivo", TENTATIVA, 2))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        acao("OPERADOR", operadorB, solicitacao, "reagendamento", reagendamento(dia(7), "Motivo", 2))
                .andExpect(status().isNotFound());
        acao("OPERADOR", operadorA, 999999, "reagendamento", reagendamento(dia(7), "Motivo", 2))
                .andExpect(status().isNotFound());

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
    }

    @Test
    void rf14_moradorAdministradorSemAtorEOperadorInexistenteOuInativoRecebem403() throws Exception {
        emRecolhimento();
        String falha = falha(dia(7), "Motivo", TENTATIVA, 2);
        String reagendamento = reagendamento(dia(7), "Motivo", 2);

        for (String[] caminho : new String[][]{{"falha", falha}, {"reagendamento", reagendamento}}) {
            acao("MORADOR", maria, solicitacao, caminho[0], caminho[1]).andExpect(status().isForbidden());
            acao("ADMINISTRADOR", null, solicitacao, caminho[0], caminho[1]).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
            mockMvc.perform(post("/solicitacoes/" + solicitacao + "/" + caminho[0]).contentType(MediaType.APPLICATION_JSON)
                    .content(caminho[1])).andExpect(status().isForbidden());
            acao("OPERADOR", 999999, solicitacao, caminho[0], caminho[1]).andExpect(status().isForbidden());
        }
        jdbc.update("UPDATE coleta.ope_operador SET ope_tx_situacao = 'INATIVO' WHERE ope_nr_id = ?", operadorA);
        comoOperadorA("falha", falha).andExpect(status().isForbidden());

        assertEquals("EM_RECOLHIMENTO", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
    }

    @Test
    void rf14_operadorAtivoReagendaEIRegistraFalhaMesmoComOPontoInativo() throws Exception {
        emRecolhimento();
        jdbc.update("UPDATE coleta.poc_ponto_coleta SET poc_tx_situacao = 'INATIVO'");

        comoOperadorA("falha", falha(dia(7), "Motivo", TENTATIVA, 2)).andExpect(status().isOk());
        comoOperadorA("reagendamento", reagendamento(dia(9), "Motivo", 3)).andExpect(status().isOk());
    }

    // ---------- estado final ----------

    @Test
    void rf14_solicitacaoCanceladaNaoPodeSerReagendadaNemRegistrarFalha() throws Exception {
        agendar();
        acao("MORADOR", maria, solicitacao, "cancelamento", "{\"motivo\": \"Não estarei em casa\", \"versao\": 1}")
                .andExpect(status().isOk());

        comoOperadorA("reagendamento", reagendamento(dia(5), MOTIVO, 2)).andExpect(status().isConflict());
        comoOperadorA("falha", falha(dia(5), MOTIVO, TENTATIVA, 2)).andExpect(status().isConflict());

        assertEquals("CANCELADA", situacaoNoBanco());
        assertEquals(3, c.contar("his_historico"));
    }

    // ---------- concorrência (RN06, ADR 0009) ----------

    private List<Integer> dispararSimultaneos(List<Callable<Integer>> tentativas, CountDownLatch largada) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tentativas.size());
        List<Future<Integer>> futuros = new ArrayList<>();
        for (Callable<Integer> tentativa : tentativas) {
            futuros.add(pool.submit(tentativa));
        }
        largada.countDown();
        List<Integer> codigos = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            codigos.add(futuro.get());
        }
        pool.shutdown();
        return codigos;
    }

    @Test
    void rf14_reagendamentosSimultaneosNaMesmaVersaoSomenteUmVenceEAVersaoSobeUmaVez() throws Exception {
        agendar();
        CountDownLatch largada = new CountDownLatch(1);
        List<Callable<Integer>> tentativas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            OffsetDateTime data = dia(10 + i);
            tentativas.add(() -> {
                largada.await();
                return comoOperadorA("reagendamento", reagendamento(data, "Motivo " + data, 1))
                        .andReturn().getResponse().getStatus();
            });
        }

        List<Integer> codigos = dispararSimultaneos(tentativas, largada);

        assertEquals(1, codigos.stream().filter(codigo -> codigo == 200).count(), "só um reagendamento usa a versão 1");
        assertEquals(3, codigos.stream().filter(codigo -> codigo == 409).count(), "os demais recebem 409, nunca 500");
        assertEquals(2, versaoNoBanco());
        assertEquals(3, c.contar("his_historico"), "criação, agendamento e um único reagendamento");
    }

    @Test
    void rf14_falhasSimultaneasNaMesmaVersaoSomenteUmaVence() throws Exception {
        emRecolhimento();
        CountDownLatch largada = new CountDownLatch(1);
        List<Callable<Integer>> tentativas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            OffsetDateTime data = dia(10 + i);
            tentativas.add(() -> {
                largada.await();
                return comoOperadorA("falha", falha(data, "Morador ausente", TENTATIVA + " " + data, 2))
                        .andReturn().getResponse().getStatus();
            });
        }

        List<Integer> codigos = dispararSimultaneos(tentativas, largada);

        assertEquals(1, codigos.stream().filter(codigo -> codigo == 200).count(), "só uma falha usa a versão 2");
        assertEquals(3, codigos.stream().filter(codigo -> codigo == 409).count(), "as demais recebem 409, nunca 500");
        assertEquals("AGENDADA", situacaoNoBanco());
        assertEquals(3, versaoNoBanco());
        assertEquals(4, c.contar("his_historico"), "criação, agendamento, início e uma única falha");
    }
}
