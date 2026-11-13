package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.OperadorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.OperadorForm;
import br.edu.ifs.coletaseletiva.service.OperadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperadorController.class)
class OperadorControllerTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);
    private static final String VALIDO = "{\"nome\": \"Carla Nunes\", \"email\": \"carla@exemplo.com\", \"pontoId\": 1}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperadorService service;

    private static OperadorDTO operador() {
        return new OperadorDTO(7, "Carla Nunes", "carla@exemplo.com", 1, SituacaoCadastro.ATIVO);
    }

    @Test
    void rf07_postValidoRetorna201ComLocation() throws Exception {
        when(service.criar(any(Ator.class), any(OperadorForm.class))).thenReturn(operador());

        mockMvc.perform(post("/operadores").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/operadores/7"))
                .andExpect(jsonPath("$.pontoId").value(1));
    }

    @Test
    void rf07_emailComEspacosChegaAparadoAoService() throws Exception {
        when(service.criar(any(Ator.class), any(OperadorForm.class))).thenReturn(operador());

        mockMvc.perform(post("/operadores").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Carla\", \"email\": \"  Carla@Exemplo.com \", \"pontoId\": 1}"))
                .andExpect(status().isCreated());

        verify(service).criar(ADMINISTRADOR, new OperadorForm("Carla", "Carla@Exemplo.com", 1, null));
    }

    @Test
    void rf07_dadosInvalidosRetornam400ComOsCampos() throws Exception {
        mockMvc.perform(post("/operadores").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"\", \"email\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='pontoId')].mensagem").value("o ponto de coleta é obrigatório"));

        verifyNoInteractions(service);
    }

    @Test
    void rf07_mensagensDeValidacaoSaemEmIngles() throws Exception {
        mockMvc.perform(post("/operadores").header("X-Perfil", "ADMINISTRADOR").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.erros[?(@.campo=='pontoId')].mensagem").value("the collection point is required"))
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("the name is required"));
    }

    @Test
    void rf07_emailRepetidoRetorna409EPontoInexistenteRetorna404() throws Exception {
        when(service.criar(any(Ator.class), any(OperadorForm.class)))
                .thenThrow(new ConflitoRegraException("erro.operador.email-repetido"))
                .thenThrow(new NaoEncontradoException("erro.ponto.nao-encontrado"));

        mockMvc.perform(post("/operadores").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe um operador cadastrado com este e-mail."));
        mockMvc.perform(post("/operadores").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void rf07_semPermissaoOuSemCabecalhoRetorna403() throws Exception {
        when(service.listar(any(Ator.class), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new AcessoNegadoException("erro.operador.somente-administrador"));

        mockMvc.perform(get("/operadores").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(get("/operadores")).andExpect(status().isForbidden());
    }

    @Test
    void rf07_listaRepassaOFiltroDePontoEAPaginacao() throws Exception {
        when(service.listar(ADMINISTRADOR, 2, 1, 5)).thenReturn(new Pagina<>(List.of(operador()), 1, 5, 6L, 2));

        mockMvc.perform(get("/operadores?pontoId=2&page=1&size=5").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.conteudo[0].email").value("carla@exemplo.com"));
    }

    @Test
    void rf07_paginacaoOuPontoIdInvalidosRetornam400() throws Exception {
        mockMvc.perform(get("/operadores?size=101").header("X-Perfil", "ADMINISTRADOR")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/operadores?pontoId=abc").header("X-Perfil", "ADMINISTRADOR")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rf07_getPorIdEPutRetornam200E404QuandoNaoExiste() throws Exception {
        when(service.buscar(ADMINISTRADOR, 7)).thenReturn(operador());
        when(service.buscar(ADMINISTRADOR, 99)).thenThrow(new NaoEncontradoException("erro.operador.nao-encontrado"));
        when(service.atualizar(any(Ator.class), any(Integer.class), any(OperadorForm.class))).thenReturn(operador());

        mockMvc.perform(get("/operadores/7").header("X-Perfil", "ADMINISTRADOR")).andExpect(status().isOk());
        mockMvc.perform(get("/operadores/99").header("X-Perfil", "ADMINISTRADOR")).andExpect(status().isNotFound());
        mockMvc.perform(put("/operadores/7").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk());
    }
}
