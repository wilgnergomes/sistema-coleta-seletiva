package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MoradorForm;
import br.edu.ifs.coletaseletiva.service.MoradorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Idioma das mensagens de erro (ADR 0023): português do Brasil por padrão, inglês com
 * Accept-Language: en. O que muda é só o texto; o status e o codigo permanecem os mesmos.
 */
@WebMvcTest(MoradorController.class)
class MensagensIdiomaTest {

    private static final String CORPO_INVALIDO = """
            {"nome": "", "email": "isto-nao-e-email"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoradorService service;

    // ---------- escolha do idioma ----------

    @Test
    void i18n_semCabecalhoAsMensagensSaemEmPortugues() throws Exception {
        mockMvc.perform(post("/moradores").contentType(MediaType.APPLICATION_JSON).content(CORPO_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."))
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("o nome é obrigatório"))
                .andExpect(jsonPath("$.erros[?(@.campo=='email')].mensagem")
                        .value("deve ser um endereço de e-mail válido"));
    }

    @Test
    void i18n_comAcceptLanguageEnAsMensagensSaemEmIngles() throws Exception {
        mockMvc.perform(post("/moradores").header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Invalid data."))
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("the name is required"))
                .andExpect(jsonPath("$.erros[?(@.campo=='email')].mensagem")
                        .value("must be a valid email address"));
    }

    @Test
    void i18n_enUSTambemEscolheOIngles() throws Exception {
        mockMvc.perform(post("/moradores").header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_INVALIDO))
                .andExpect(jsonPath("$.mensagem").value("Invalid data."));
    }

    @Test
    void i18n_idiomaNaoSuportadoCaiNoPortuguesEnaoNoIdiomaDoSistema() throws Exception {
        mockMvc.perform(post("/moradores").header(HttpHeaders.ACCEPT_LANGUAGE, "fr")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_INVALIDO))
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));
    }

    // ---------- validação com argumento: {max}, {value} ----------

    @Test
    void i18n_validacaoComLimiteInsereOValorNaMensagem() throws Exception {
        String nomeLongo = "a".repeat(81);
        String corpo = "{\"nome\": \"" + nomeLongo + "\", \"email\": \"maria@exemplo.com\"}";

        mockMvc.perform(post("/moradores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(jsonPath("$.erros[0].mensagem").value("deve ter no máximo 80 caracteres"));
        mockMvc.perform(post("/moradores").header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(jsonPath("$.erros[0].mensagem").value("must have at most 80 characters"));
    }

    @Test
    void i18n_parametroDePaginacaoInvalidoSaiNoIdiomaPedido() throws Exception {
        mockMvc.perform(get("/moradores?size=101").header("X-Perfil", "ADMINISTRADOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parâmetro inválido."))
                .andExpect(jsonPath("$.erros[0].mensagem").value("deve ser no máximo 100"));
        mockMvc.perform(get("/moradores?size=101").header("X-Perfil", "ADMINISTRADOR")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Invalid parameter."))
                .andExpect(jsonPath("$.erros[0].mensagem").value("must be at most 100"));
    }

    // ---------- exceções de negócio: chave + argumentos ----------

    @Test
    void i18n_conflitoDeRegraTraduzidoPelaChaveDaExcecao() throws Exception {
        when(service.criar(any(MoradorForm.class)))
                .thenThrow(new ConflitoRegraException("erro.morador.email-repetido"));
        String corpo = "{\"nome\": \"Maria\", \"email\": \"maria@exemplo.com\"}";

        mockMvc.perform(post("/moradores").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("Já existe um morador cadastrado com este e-mail."));
        mockMvc.perform(post("/moradores").header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_REGRA"))
                .andExpect(jsonPath("$.mensagem").value("A resident with this email is already registered."));
    }

    @Test
    void i18n_naoEncontradoEAcessoNegadoMantemStatusECodigoNosDoisIdiomas() throws Exception {
        Ator admin = new Ator(Perfil.ADMINISTRADOR, null);
        when(service.buscar(admin, 99)).thenThrow(new NaoEncontradoException("erro.morador.nao-encontrado"));
        when(service.listar(new Ator(Perfil.MORADOR, 1), 0, 20))
                .thenThrow(new AcessoNegadoException("erro.morador.listar-somente-administrador"));

        for (String idioma : new String[]{"pt-BR", "en"}) {
            mockMvc.perform(get("/moradores/99").header("X-Perfil", "ADMINISTRADOR")
                            .header(HttpHeaders.ACCEPT_LANGUAGE, idioma))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
            mockMvc.perform(get("/moradores").header("X-Perfil", "MORADOR").header("X-Usuario-Id", "1")
                            .header(HttpHeaders.ACCEPT_LANGUAGE, idioma))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        }

        mockMvc.perform(get("/moradores/99").header("X-Perfil", "ADMINISTRADOR")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(jsonPath("$.mensagem").value("Resident not found."));
    }

    @Test
    void i18n_cabecalhoDoAtorAusenteUsaArgumentoNaMensagem() throws Exception {
        mockMvc.perform(get("/moradores"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Informe o cabeçalho X-Perfil."));
        mockMvc.perform(get("/moradores").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem").value("Provide the X-Perfil header."));
        mockMvc.perform(get("/moradores").header("X-Perfil", "MORADOR").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(jsonPath("$.mensagem").value("Provide the X-Usuario-Id header."));
    }

    // ---------- mensagens gerais do tratador ----------

    @Test
    void i18n_erroInesperadoTraduzidoSemVazarDetalhes() throws Exception {
        when(service.buscar(new Ator(Perfil.ADMINISTRADOR, null), 1)).thenThrow(new IllegalStateException("detalhe interno"));

        mockMvc.perform(get("/moradores/1").header("X-Perfil", "ADMINISTRADOR")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Internal error. Try again later."));
    }

    @Test
    void i18n_enumInvalidoListaOsValoresAceitosNoIdiomaPedido() throws Exception {
        String corpo = "{\"nome\": \"Maria\", \"email\": \"maria@exemplo.com\", \"situacao\": \"SUSPENSO\"}";

        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"))
                .andExpect(jsonPath("$.mensagem").value("Valor inválido. Valores aceitos: ATIVO, INATIVO."));
        mockMvc.perform(put("/moradores/1")
                        .header("X-Perfil", "ADMINISTRADOR").header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"))
                .andExpect(jsonPath("$.mensagem").value("Invalid value. Accepted values: ATIVO, INATIVO."));
    }
}
