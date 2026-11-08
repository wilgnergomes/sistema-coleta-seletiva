package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MaterialForm;
import br.edu.ifs.coletaseletiva.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

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

@WebMvcTest(MaterialController.class)
class MaterialControllerTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);
    private static final String VALIDO = """
            {"nome": "Vidro", "descricao": "Garrafas e potes de vidro", "unidade": "KG",
             "orientacao": "Lavar e remover tampas"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService service;

    private static MaterialDTO vidro() {
        return new MaterialDTO(3, "Vidro", "Garrafas e potes de vidro", Unidade.KG, "Lavar e remover tampas",
                SituacaoCadastro.ATIVO);
    }

    @Test
    void rf03_postValidoRetorna201ComLocation() throws Exception {
        when(service.criar(any(Ator.class), any(MaterialForm.class))).thenReturn(vidro());

        mockMvc.perform(post("/materiais").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/materiais/3"))
                .andExpect(jsonPath("$.unidade").value("KG"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
    }

    @Test
    void rf03_postInvalidoRetorna400ComOsCampos() throws Exception {
        mockMvc.perform(post("/materiais").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='descricao')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='unidade')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='orientacao')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    void rf03_unidadeForaDoEnumRetorna400ComOsValoresAceitos() throws Exception {
        String corpo = VALIDO.replace("\"KG\"", "\"TON\"");

        mockMvc.perform(post("/materiais").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"))
                .andExpect(jsonPath("$.mensagem").value("Valor inválido. Valores aceitos: KG, L, UN."));
    }

    @Test
    void rf03_nomeRepetidoRetorna409EmInglesQuandoPedido() throws Exception {
        when(service.criar(any(Ator.class), any(MaterialForm.class)))
                .thenThrow(new ConflitoRegraException("erro.material.nome-repetido"));

        mockMvc.perform(post("/materiais").header("X-Perfil", "ADMINISTRADOR").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A material with this name already exists."));
    }

    @Test
    void rf03_postSemAtorOuSemPermissaoRetorna403() throws Exception {
        mockMvc.perform(post("/materiais").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());

        when(service.criar(any(Ator.class), any(MaterialForm.class)))
                .thenThrow(new AcessoNegadoException("erro.material.somente-administrador"));
        mockMvc.perform(post("/materiais").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void rf03_getListaFuncionaSemCabecalhosEPassaOAtorVazioAoService() throws Exception {
        when(service.listar(Optional.empty(), null, 0, 20))
                .thenReturn(new Pagina<>(List.of(vidro()), 0, 20, 1L, 1));

        mockMvc.perform(get("/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].nome").value("Vidro"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void rf03_getListaRepassaOAtorEOFiltroDeSituacao() throws Exception {
        when(service.listar(Optional.of(ADMINISTRADOR), SituacaoCadastro.INATIVO, 0, 20))
                .thenReturn(new Pagina<>(List.of(), 0, 20, 0L, 0));

        mockMvc.perform(get("/materiais?situacao=INATIVO").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk());

        verify(service).listar(Optional.of(ADMINISTRADOR), SituacaoCadastro.INATIVO, 0, 20);
    }

    @Test
    void rf03_situacaoInvalidaEPaginacaoInvalidaRetornam400() throws Exception {
        mockMvc.perform(get("/materiais?situacao=XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
        mockMvc.perform(get("/materiais?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        mockMvc.perform(get("/materiais").header("X-Perfil", "CHEFE"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void rf03_getPorIdPublicoEInexistenteRetorna404() throws Exception {
        when(service.buscar(Optional.empty(), 3)).thenReturn(vidro());
        when(service.buscar(Optional.empty(), 99)).thenThrow(new NaoEncontradoException("erro.material.nao-encontrado"));

        mockMvc.perform(get("/materiais/3")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        mockMvc.perform(get("/materiais/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Material não encontrado."));
    }

    @Test
    void rf03_putValidoRetorna200EUnidadeBloqueadaRetorna409() throws Exception {
        when(service.atualizar(any(Ator.class), any(Integer.class), any(MaterialForm.class)))
                .thenReturn(vidro())
                .thenThrow(new ConflitoRegraException("erro.material.unidade-bloqueada"));

        mockMvc.perform(put("/materiais/3").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk());
        mockMvc.perform(put("/materiais/3").header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));
    }
}
