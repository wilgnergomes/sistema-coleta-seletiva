package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.EnderecoDTO;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.service.EnderecoService;
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

@WebMvcTest(EnderecoController.class)
class EnderecoControllerTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);
    private static final String VALIDO = """
            {"logradouro": "Rua das Flores", "numero": "120", "bairro": "Centro",
             "municipio": "Lagarto", "uf": "SE", "cep": "49400000", "complemento": "Casa dos fundos"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnderecoService service;

    private static EnderecoDTO endereco() {
        return new EnderecoDTO(10, 1, "Rua das Flores", "120", "Centro", "Lagarto", "SE", "49400000", "Casa dos fundos");
    }

    @Test
    void rf02_postValidoRetorna201ComLocation() throws Exception {
        when(service.criar(any(Ator.class), any(Integer.class), any(EnderecoForm.class))).thenReturn(endereco());

        mockMvc.perform(post("/moradores/1/enderecos").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/moradores/1/enderecos/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.moradorId").value(1));
    }

    @Test
    void rf02_enderecoIncompletoRetorna400ComOsCampos() throws Exception {
        mockMvc.perform(post("/moradores/1/enderecos").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"logradouro\": \"Rua A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='numero')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='bairro')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='municipio')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='uf')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='cep')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    void rf02_ufMinusculaECepComMascaraSaoRejeitados() throws Exception {
        String corpo = VALIDO.replace("\"SE\"", "\"se\"").replace("49400000", "49400-000");

        mockMvc.perform(post("/moradores/1/enderecos").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='uf')].mensagem").value("deve ter duas letras maiúsculas"))
                .andExpect(jsonPath("$.erros[?(@.campo=='cep')].mensagem").value("deve ter oito dígitos, sem máscara"));
    }

    @Test
    void rf02_mensagensDeValidacaoSaemEmInglesComAcceptLanguage() throws Exception {
        mockMvc.perform(post("/moradores/1/enderecos").header("X-Perfil", "ADMINISTRADOR")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"logradouro\": \"Rua A\", \"cep\": \"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='numero')].mensagem")
                        .value("the number is required (use S/N when there is none)"))
                .andExpect(jsonPath("$.erros[?(@.campo=='cep')].mensagem").value("must have eight digits, without mask"));
    }

    @Test
    void rf02_numeroSemNumeroEComplementoAusenteSaoAceitos() throws Exception {
        when(service.criar(any(Ator.class), any(Integer.class), any(EnderecoForm.class))).thenReturn(endereco());
        String corpo = """
                {"logradouro": "Rua A", "numero": "  s/n ", "bairro": "B", "municipio": "C", "uf": "SE", "cep": "49400000"}
                """;

        mockMvc.perform(post("/moradores/1/enderecos").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        verify(service).criar(ADMINISTRADOR, 1, new EnderecoForm("Rua A", "s/n", "B", "C", "SE", "49400000", null));
    }

    @Test
    void rf02_getListaDevolveArray() throws Exception {
        when(service.listar(ADMINISTRADOR, 1)).thenReturn(List.of(endereco()));

        mockMvc.perform(get("/moradores/1/enderecos").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cep").value("49400000"));
    }

    @Test
    void rf02_getSemPermissaoRetorna403EMoradorInexistenteRetorna404() throws Exception {
        when(service.listar(new Ator(Perfil.MORADOR, 2), 1))
                .thenThrow(new AcessoNegadoException("erro.morador.acesso-proprio-cadastro"));
        when(service.listar(ADMINISTRADOR, 99)).thenThrow(new NaoEncontradoException("erro.morador.nao-encontrado"));

        mockMvc.perform(get("/moradores/1/enderecos").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mockMvc.perform(get("/moradores/99/enderecos").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void rf02_semCabecalhoDePerfilRetorna403() throws Exception {
        mockMvc.perform(get("/moradores/1/enderecos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        verifyNoInteractions(service);
    }

    @Test
    void rf02_putValidoRetorna200EEnderecoDeOutroMoradorRetorna404() throws Exception {
        when(service.atualizar(any(Ator.class), any(Integer.class), any(Integer.class), any(EnderecoForm.class)))
                .thenReturn(endereco())
                .thenThrow(new NaoEncontradoException("erro.endereco.nao-encontrado"));

        mockMvc.perform(put("/moradores/1/enderecos/10").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        mockMvc.perform(put("/moradores/1/enderecos/50").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Endereço não encontrado para este morador."));
    }
}
