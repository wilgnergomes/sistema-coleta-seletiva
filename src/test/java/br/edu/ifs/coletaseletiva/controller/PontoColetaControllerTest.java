package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.EnderecoPontoDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.service.PontoColetaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PontoColetaController.class)
class PontoColetaControllerTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);
    private static final String VALIDO = """
            {"nome": "Cooperativa Verde Vida", "contato": "(79) 3333-0000",
             "horarios": "Segunda a sexta, das 8h às 17h", "recolhimentoDomiciliar": true,
             "endereco": {"logradouro": "Avenida Principal", "numero": "S/N", "bairro": "Distrito Industrial",
                          "municipio": "Lagarto", "uf": "SE", "cep": "49400000"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PontoColetaService service;

    private static PontoColetaDTO ponto() {
        return new PontoColetaDTO(1, "Cooperativa Verde Vida", "(79) 3333-0000", "Segunda a sexta", true,
                SituacaoCadastro.ATIVO,
                new EnderecoPontoDTO("Avenida Principal", "S/N", "Distrito Industrial", "Lagarto", "SE", "49400000", null),
                List.of());
    }

    @Test
    void rf04_postValidoRetorna201ComLocationEMateriaisAceitosVazio() throws Exception {
        when(service.criar(any(Ator.class), any(PontoColetaForm.class))).thenReturn(ponto());

        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/pontos-coleta/1"))
                .andExpect(jsonPath("$.endereco.municipio").value("Lagarto"))
                .andExpect(jsonPath("$.materiaisAceitos.length()").value(0));
    }

    @Test
    void rf04_pontoSemNomeOuSemEnderecoEhRejeitado() throws Exception {
        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contato\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='endereco')].mensagem").value("o endereço é obrigatório"));

        verifyNoInteractions(service);
    }

    @Test
    void rf04_enderecoAninhadoEhValidadoComCaminhoDoCampo() throws Exception {
        String corpo = VALIDO.replace("\"SE\"", "\"se\"").replace("49400000", "123");

        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='endereco.uf')].mensagem").value("deve ter duas letras maiúsculas"))
                .andExpect(jsonPath("$.erros[?(@.campo=='endereco.cep')]").exists());
    }

    @Test
    void rf04_mensagensDoEnderecoAninhadoSaemEmIngles() throws Exception {
        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", "ADMINISTRADOR").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"endereco\": {\"cep\": \"1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("the name is required"))
                .andExpect(jsonPath("$.erros[?(@.campo=='endereco.cep')].mensagem")
                        .value("must have eight digits, without mask"));
    }

    @Test
    void rf04_postSemPermissaoRetorna403() throws Exception {
        when(service.criar(any(Ator.class), any(PontoColetaForm.class)))
                .thenThrow(new AcessoNegadoException("erro.ponto.somente-administrador"));

        mockMvc.perform(post("/pontos-coleta").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(post("/pontos-coleta").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf04_getListaEPublicaEAceitaFiltroDeSituacaoDoAdministrador() throws Exception {
        when(service.listar(Optional.empty(), null, 0, 20)).thenReturn(new Pagina<>(List.of(ponto()), 0, 20, 1L, 1));
        when(service.listar(Optional.of(ADMINISTRADOR), SituacaoCadastro.INATIVO, 0, 20))
                .thenReturn(new Pagina<>(List.of(), 0, 20, 0L, 0));

        mockMvc.perform(get("/pontos-coleta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].nome").value("Cooperativa Verde Vida"));
        mockMvc.perform(get("/pontos-coleta?situacao=INATIVO").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void rf04_situacaoOuPaginacaoInvalidasRetornam400() throws Exception {
        mockMvc.perform(get("/pontos-coleta?situacao=XYZ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
        mockMvc.perform(get("/pontos-coleta?size=101")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rf04_getPorIdPublicoEInexistenteRetorna404() throws Exception {
        when(service.buscar(Optional.empty(), 1)).thenReturn(ponto());
        when(service.buscar(Optional.empty(), 99)).thenThrow(new NaoEncontradoException("erro.ponto.nao-encontrado"));

        mockMvc.perform(get("/pontos-coleta/1")).andExpect(status().isOk());
        mockMvc.perform(get("/pontos-coleta/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Ponto de coleta não encontrado."));
    }

    @Test
    void rf04_putValidoRetorna200() throws Exception {
        when(service.atualizar(any(Ator.class), any(Integer.class), any(PontoColetaForm.class))).thenReturn(ponto());

        mockMvc.perform(put("/pontos-coleta/1").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
