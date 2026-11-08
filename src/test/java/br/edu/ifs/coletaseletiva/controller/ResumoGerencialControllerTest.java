package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.QuantidadeRecolhidaDTO;
import br.edu.ifs.coletaseletiva.dto.ResumoGerencialDTO;
import br.edu.ifs.coletaseletiva.dto.SituacaoTotalDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.service.ResumoGerencialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumoGerencialController.class)
class ResumoGerencialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumoGerencialService service;

    @Test
    void rf15_administradorRecebeOsDoisBlocosComOFormatoDoContrato() throws Exception {
        when(service.resumir(any(Ator.class), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)))).thenReturn(
                new ResumoGerencialDTO(
                        List.of(new SituacaoTotalDTO(SituacaoSolicitacao.SOLICITADA, 2),
                                new SituacaoTotalDTO(SituacaoSolicitacao.CONCLUIDA, 4)),
                        List.of(new QuantidadeRecolhidaDTO(1, "Papel", Unidade.KG, new BigDecimal("28.5")),
                                new QuantidadeRecolhidaDTO(3, "Pilhas", Unidade.UN, new BigDecimal("4")))));

        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR")
                        .param("de", "2026-10-01").param("ate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitacoesPorSituacao[0].situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.solicitacoesPorSituacao[0].total").value(2))
                .andExpect(jsonPath("$.quantidadesRecolhidas[0].nomeMaterial").value("Papel"))
                .andExpect(jsonPath("$.quantidadesRecolhidas[0].unidade").value("KG"))
                .andExpect(jsonPath("$.quantidadesRecolhidas[1].unidade").value("UN"));
    }

    @Test
    void rf15_osDoisParametrosSaoOpcionais() throws Exception {
        when(service.resumir(any(Ator.class), eq(null), eq(null)))
                .thenReturn(new ResumoGerencialDTO(List.of(), List.of()));

        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadesRecolhidas").isEmpty());

        verify(service).resumir(any(Ator.class), eq(null), eq(null));
    }

    @Test
    void rf15_dataMalformadaRetorna400NoCampoSemChamarOService() throws Exception {
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR").param("de", "01/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("de"));
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR").param("ate", "amanhã"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[0].campo").value("ate"));

        verifyNoInteractions(service);
    }

    @Test
    void rf15_periodoInvertidoRetorna400NoCampoAteEmPortuguesEInglesAcessoNegadoRetorna403() throws Exception {
        when(service.resumir(any(Ator.class), any(), any()))
                .thenThrow(new ParametroInvalidoException("ate", "erro.solicitacao.periodo-invalido"))
                .thenThrow(new ParametroInvalidoException("ate", "erro.solicitacao.periodo-invalido"))
                .thenThrow(new AcessoNegadoException("erro.resumo.somente-administrador"));

        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR")
                        .param("de", "2026-11-10").param("ate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("ate"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("A data final não pode ser anterior à data inicial."));
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "ADMINISTRADOR").header("Accept-Language", "en")
                        .param("de", "2026-11-10").param("ate", "2026-11-01"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("The end date cannot be earlier than the start date."));
        mockMvc.perform(get("/resumo-gerencial").header("X-Perfil", "OPERADOR").header("X-Usuario-Id", "5"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.mensagem").value("Apenas o administrador pode consultar o resumo gerencial."));
    }

    @Test
    void rf15_semCabecalhosDoAtorRetorna403() throws Exception {
        mockMvc.perform(get("/resumo-gerencial")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
