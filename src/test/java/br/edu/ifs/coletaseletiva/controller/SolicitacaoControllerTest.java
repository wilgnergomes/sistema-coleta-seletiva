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
import br.edu.ifs.coletaseletiva.form.SolicitacaoCorrecaoForm;
import br.edu.ifs.coletaseletiva.form.SolicitacaoForm;
import br.edu.ifs.coletaseletiva.service.SolicitacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitacaoController.class)
class SolicitacaoControllerTest {

    private static final String VALIDO = """
            {"pontoId": 1, "enderecoId": 3, "observacoes": "Caixas na calçada",
             "itens": [{"materialId": 1, "quantidadeEstimada": 5.5}, {"materialId": 2, "quantidadeEstimada": 10}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitacaoService service;

    private static SolicitacaoDetalheDTO detalhe() {
        return new SolicitacaoDetalheDTO(9, "SOL-20261107-ABC234", 1, 1, "Rua A, 1 - Centro - Lagarto/SE - CEP 49400000",
                SituacaoSolicitacao.SOLICITADA, OffsetDateTime.now(), null, null, "Caixas na calçada", 0,
                List.of(new ItemDTO(1, "Papel", Unidade.KG, new BigDecimal("5.5"), null)), List.of());
    }

    private org.springframework.test.web.servlet.ResultActions postar(String corpo, String... idioma) throws Exception {
        var requisicao = post("/solicitacoes").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    @Test
    void rf08_postValidoRetorna201ComLocationEDetalhe() throws Exception {
        when(service.criar(any(Ator.class), any(SolicitacaoForm.class))).thenReturn(detalhe());

        postar(VALIDO)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/solicitacoes/9"))
                .andExpect(jsonPath("$.protocolo").value("SOL-20261107-ABC234"))
                .andExpect(jsonPath("$.situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.itens[0].unidade").value("KG"));
    }

    @Test
    void rf08_semItensComQuantidadeInvalidaOuSemCamposObrigatoriosRetorna400() throws Exception {
        postar("{\"pontoId\": 1, \"enderecoId\": 3, \"itens\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens')].mensagem").value("informe pelo menos um item"));
        postar("{\"pontoId\": 1, \"enderecoId\": 3, \"itens\": [{\"materialId\": 1, \"quantidadeEstimada\": -2}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='itens[0].quantidadeEstimada')].mensagem").value("deve ser maior que zero"));
        postar("{\"itens\": [{\"materialId\": 1, \"quantidadeEstimada\": 1}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='pontoId')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='enderecoId')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    void rf08_validacaoSaiEmInglesComAcceptLanguage() throws Exception {
        postar("{\"pontoId\": 1, \"enderecoId\": 3, \"itens\": []}", "en")
                .andExpect(jsonPath("$.erros[?(@.campo=='itens')].mensagem").value("provide at least one item"));
    }

    @Test
    void rf08_violacoesDeRegraRetornam409404E403() throws Exception {
        when(service.criar(any(Ator.class), any(SolicitacaoForm.class)))
                .thenThrow(new ConflitoRegraException("erro.solicitacao.material-nao-aceito", "Metal"))
                .thenThrow(new NaoEncontradoException("erro.endereco.nao-encontrado"))
                .thenThrow(new AcessoNegadoException("erro.solicitacao.somente-morador"));

        postar(VALIDO).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("O ponto de coleta não aceita o material Metal."));
        postar(VALIDO).andExpect(status().isNotFound());
        postar(VALIDO).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(post("/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf08_getPorIdRetorna200E404() throws Exception {
        when(service.buscar(any(Ator.class), any(Integer.class))).thenReturn(detalhe())
                .thenThrow(new NaoEncontradoException("erro.solicitacao.nao-encontrada"));

        mockMvc.perform(get("/solicitacoes/9").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9));
        mockMvc.perform(get("/solicitacoes/9").header("X-Perfil", "ADMINISTRADOR").header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Request not found."));
    }

    @Test
    void rf08_putExigeVersaoERetorna409ComCodigoDeVersaoQuandoDesatualizada() throws Exception {
        String semVersao = "{\"enderecoId\": 3, \"itens\": [{\"materialId\": 1, \"quantidadeEstimada\": 1}]}";
        mockMvc.perform(put("/solicitacoes/9").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(semVersao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='versao')].mensagem").value("a versão é obrigatória"));

        when(service.corrigir(any(Ator.class), any(Integer.class), any(SolicitacaoCorrecaoForm.class)))
                .thenThrow(new ConflitoVersaoException("erro.solicitacao.versao-desatualizada"));
        mockMvc.perform(put("/solicitacoes/9").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semVersao.replace("}]}", "}], \"versao\": 0}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }
}
