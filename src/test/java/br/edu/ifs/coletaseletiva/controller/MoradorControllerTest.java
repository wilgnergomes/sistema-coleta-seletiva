package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MoradorAtualizacaoForm;
import br.edu.ifs.coletaseletiva.form.MoradorCriacaoForm;
import br.edu.ifs.coletaseletiva.service.MoradorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada HTTP do RF01: só o controller, o tratador de erros e a leitura dos
 * cabeçalhos do ator. O service é simulado; aqui interessam status, cabeçalhos e JSON.
 */
@WebMvcTest(MoradorController.class)
class MoradorControllerTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoradorService service;

    private static MoradorDTO maria() {
        return new MoradorDTO(1, "Maria", "maria@exemplo.com", null, SituacaoCadastro.ATIVO);
    }

    // ---------- POST /moradores ----------

    @Test
    void rf01_postValidoRetorna201ComLocationEPublico() throws Exception {
        when(service.criar(any(MoradorCriacaoForm.class))).thenReturn(maria());

        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/moradores/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
    }

    @Test
    void rf01_postSemNomeEComEmailInvalidoRetorna400ComOsCampos() throws Exception {
        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "email": "isto-nao-e-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    void rf01_postComJsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void rf01_postComEmailRepetidoRetorna409() throws Exception {
        when(service.criar(any(MoradorCriacaoForm.class)))
                .thenThrow(new ConflitoRegraException("erro.morador.email-repetido"));

        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));
    }

    @Test
    void rf01_postQueColideComOUniqueDoBancoRetorna409GenericoEnaoMensagemTecnica() throws Exception {
        // Corrida: o service não viu o e-mail repetido, mas o banco barrou na gravação.
        when(service.criar(any(MoradorCriacaoForm.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uq_mor_email"));

        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("Os dados informados violam uma regra de integridade."));
    }

    @Test
    void rf01_postComEmailCercadoDeEspacosEhAceitoEChegaAparadoAoService() throws Exception {
        // O contrato (OpenAPI e ADR 0021) diz que o e-mail é normalizado: "sem espaços nas pontas".
        // O form apara os espaços; as minúsculas continuam por conta do service.
        when(service.criar(any(MoradorCriacaoForm.class))).thenReturn(maria());

        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "  Maria@Exemplo.com  "}
                                """))
                .andExpect(status().isCreated());

        verify(service).criar(new MoradorCriacaoForm("Maria", "Maria@Exemplo.com", null));
    }

    @Test
    void rf01_postComEmailAusenteOuSoDeEspacosRetorna400() throws Exception {
        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists());

        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "     "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    void rf01_postSemJsonRetorna415() throws Exception {
        mockMvc.perform(post("/moradores")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Maria"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---------- GET /moradores ----------

    @Test
    void rf01_getListaComoAdministradorRetornaOEnvelopeDePaginacao() throws Exception {
        when(service.listar(ADMINISTRADOR, 0, 20))
                .thenReturn(new Pagina<>(List.of(maria()), 0, 20, 1L, 1));

        mockMvc.perform(get("/moradores").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void rf01_getListaRepassaPageESizeDaUrlParaOService() throws Exception {
        when(service.listar(ADMINISTRADOR, 2, 10))
                .thenReturn(new Pagina<>(List.of(), 2, 10, 21L, 3));

        mockMvc.perform(get("/moradores?page=2&size=10").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(3));
    }

    @Test
    void rf01_getListaSemCabecalhoDePerfilRetorna403() throws Exception {
        mockMvc.perform(get("/moradores"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(service);
    }

    @Test
    void rf01_getListaComPerfilInvalidoRetorna403() throws Exception {
        mockMvc.perform(get("/moradores").header("X-Perfil", "CHEFE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void rf01_getListaDeMoradorSemUsuarioIdRetorna403() throws Exception {
        mockMvc.perform(get("/moradores").header("X-Perfil", "MORADOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void rf01_getListaNegadaPeloServiceRetorna403() throws Exception {
        when(service.listar(new Ator(Perfil.MORADOR, 1), 0, 20))
                .thenThrow(new AcessoNegadoException("erro.morador.listar-somente-administrador"));

        mockMvc.perform(get("/moradores")
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void rf01_getListaComSizeAcimaDe100Retorna400() throws Exception {
        mockMvc.perform(get("/moradores?size=101").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[?(@.campo=='size')]").exists());

        verify(service, never()).listar(any(), anyInt(), anyInt());
    }

    @Test
    void rf01_getListaComSizeZeroOuPageNegativaRetorna400() throws Exception {
        mockMvc.perform(get("/moradores?size=0").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/moradores?page=-1").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------- GET /moradores/{id} ----------

    @Test
    void rf01_getPorIdComoAdministradorRetorna200() throws Exception {
        when(service.buscar(ADMINISTRADOR, 1)).thenReturn(maria());

        mockMvc.perform(get("/moradores/1").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    void rf01_getPorIdInexistenteRetorna404() throws Exception {
        when(service.buscar(ADMINISTRADOR, 99)).thenThrow(new NaoEncontradoException("erro.morador.nao-encontrado"));

        mockMvc.perform(get("/moradores/99").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void rf01_getPorIdQueNaoEhNumeroRetorna400() throws Exception {
        mockMvc.perform(get("/moradores/abc").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void rf01_getPorIdDeOutroMoradorRetorna403() throws Exception {
        when(service.buscar(new Ator(Perfil.MORADOR, 1), 2))
                .thenThrow(new AcessoNegadoException("erro.morador.acesso-proprio-cadastro"));

        mockMvc.perform(get("/moradores/2")
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    // ---------- PUT /moradores/{id} ----------

    @Test
    void rf01_putValidoRetorna200() throws Exception {
        MoradorDTO atualizado = new MoradorDTO(1, "Maria Silva", "maria@exemplo.com", null, SituacaoCadastro.ATIVO);
        when(service.atualizar(new Ator(Perfil.MORADOR, 1), 1,
                new MoradorAtualizacaoForm("Maria Silva", "maria@exemplo.com", null, null)))
                .thenReturn(atualizado);

        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "MORADOR")
                        .header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria Silva", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    void rf01_putComEmailCercadoDeEspacosEhAceitoEChegaAparadoAoService() throws Exception {
        when(service.atualizar(any(Ator.class), any(Integer.class), any(MoradorAtualizacaoForm.class)))
                .thenReturn(maria());

        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "  Maria@Exemplo.com  "}
                                """))
                .andExpect(status().isOk());

        verify(service).atualizar(ADMINISTRADOR, 1,
                new MoradorAtualizacaoForm("Maria", "Maria@Exemplo.com", null, null));
    }

    @Test
    void rf01_putComEmailAusenteRetorna400() throws Exception {
        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='email')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    void rf01_putComSituacaoInexistenteRetorna400ComValoresAceitos() throws Exception {
        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com", "situacao": "SUSPENSO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));

        verifyNoInteractions(service);
    }

    @Test
    void rf01_putSemCabecalhoDePerfilRetorna403() throws Exception {
        mockMvc.perform(put("/moradores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "maria@exemplo.com"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void rf01_putComEmailDeOutroMoradorRetorna409() throws Exception {
        when(service.atualizar(any(Ator.class), any(Integer.class), any(MoradorAtualizacaoForm.class)))
                .thenThrow(new ConflitoRegraException("erro.morador.email-repetido"));

        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria", "email": "joao@exemplo.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"));
    }

    // ---------- fora do contrato ----------

    @Test
    void rf01_deleteNaoExisteEPorIssoRetorna405() throws Exception {
        mockMvc.perform(delete("/moradores/1").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(service);
    }

    @Test
    void rf01_erroInesperadoRetorna500SemVazarDetalhes() throws Exception {
        when(service.buscar(ADMINISTRADOR, 1)).thenThrow(new IllegalStateException("detalhe interno"));

        mockMvc.perform(get("/moradores/1").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Erro interno. Tente novamente mais tarde."));
    }
}
