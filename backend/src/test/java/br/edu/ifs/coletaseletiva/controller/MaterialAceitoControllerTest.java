package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.VinculoForm;
import br.edu.ifs.coletaseletiva.service.MaterialAceitoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialAceitoController.class)
class MaterialAceitoControllerTest {

    private static final String URL = "/pontos-coleta/1/materiais/2";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialAceitoService service;

    private static MaterialAceitoDTO vinculo() {
        return new MaterialAceitoDTO(2, "Vidro", Unidade.KG, "Lavar", "Apenas limpas", new BigDecimal("50.500"));
    }

    private org.springframework.test.web.servlet.ResultActions putAdmin(String corpo, String... idioma) throws Exception {
        var requisicao = put(URL).header("X-Perfil", "ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (idioma.length > 0) {
            requisicao.header("Accept-Language", idioma[0]);
        }
        return mockMvc.perform(requisicao);
    }

    @Test
    void rf05_putValidoRetorna200ComOMaterialAceito() throws Exception {
        when(service.vincular(any(Ator.class), any(Integer.class), any(Integer.class), any(VinculoForm.class)))
                .thenReturn(vinculo());

        putAdmin("{\"observacoes\": \"Apenas limpas\", \"limite\": 50.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materialId").value(2))
                .andExpect(jsonPath("$.nomeMaterial").value("Vidro"))
                .andExpect(jsonPath("$.unidade").value("KG"))
                .andExpect(jsonPath("$.limite").value(50.5));
    }

    @Test
    void rf05_corpoVazioEhAceitoPoisOsDoisCamposSaoOpcionais() throws Exception {
        when(service.vincular(any(Ator.class), any(Integer.class), any(Integer.class), any(VinculoForm.class)))
                .thenReturn(vinculo());

        putAdmin("{}").andExpect(status().isOk());
    }

    @Test
    void rf05_limiteZeroNegativoOuComMuitasCasasEhRejeitado() throws Exception {
        putAdmin("{\"limite\": 0}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='limite')].mensagem").value("deve ser maior que zero"));
        putAdmin("{\"limite\": -5}").andExpect(status().isBadRequest());
        putAdmin("{\"limite\": 1.2345}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='limite')].mensagem")
                        .value("deve ter no máximo 9 dígitos inteiros e 3 casas decimais"));
        putAdmin("{\"limite\": 1000000000}").andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rf05_mensagemDoLimiteSaiEmInglesComAcceptLanguage() throws Exception {
        putAdmin("{\"limite\": 0}", "en").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='limite')].mensagem").value("must be greater than zero"));
    }

    @Test
    void rf05_observacoesMuitoLongasSaoRejeitadas() throws Exception {
        putAdmin("{\"observacoes\": \"" + "a".repeat(501) + "\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='observacoes')]").exists());
    }

    @Test
    void rf05_semPermissaoRetorna403EPontoOuMaterialInexistenteRetorna404() throws Exception {
        when(service.vincular(any(Ator.class), any(Integer.class), any(Integer.class), any(VinculoForm.class)))
                .thenThrow(new AcessoNegadoException("erro.vinculo.somente-administrador"))
                .thenThrow(new NaoEncontradoException("erro.ponto.nao-encontrado"));

        mockMvc.perform(put(URL).header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        putAdmin("{}").andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf05_deleteRetorna204EVinculoInexistenteRetorna404() throws Exception {
        doThrow(new NaoEncontradoException("erro.vinculo.nao-encontrado"))
                .when(service).remover(any(Ator.class), any(Integer.class), any(Integer.class));

        mockMvc.perform(delete("/pontos-coleta/1/materiais/3").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Vínculo entre o ponto de coleta e o material não encontrado."));
    }

    @Test
    void rf05_deleteValidoRetorna204SemCorpo() throws Exception {
        mockMvc.perform(delete(URL).header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isNoContent());
    }
}
