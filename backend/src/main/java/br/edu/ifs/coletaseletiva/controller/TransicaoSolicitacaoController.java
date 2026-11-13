package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.form.AgendamentoForm;
import br.edu.ifs.coletaseletiva.form.ConclusaoForm;
import br.edu.ifs.coletaseletiva.form.FalhaForm;
import br.edu.ifs.coletaseletiva.form.MotivoForm;
import br.edu.ifs.coletaseletiva.form.ReagendamentoForm;
import br.edu.ifs.coletaseletiva.form.VersaoForm;
import br.edu.ifs.coletaseletiva.service.TransicaoSolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/** Ações que mudam a situação da solicitação (RF10 a RF14). Cada uma devolve a solicitação atualizada. */
@Tag(name = "Situação da solicitação")
@RestController
@RequestMapping("/solicitacoes/{id}")
@RequiredArgsConstructor
public class TransicaoSolicitacaoController {

    private final TransicaoSolicitacaoService service;

    /** RF10: o operador do ponto agenda o recolhimento. */
    @Operation(summary = "Agendar o recolhimento", description = """
            O operador define a data e a hora do recolhimento. **Só o operador ativo do ponto** da solicitação.

            Só a partir de `SOLICITADA` (passa a `AGENDADA`). A `dataHora` precisa ser posterior ao momento atual. Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/agendamento")
    public SolicitacaoDetalheDTO agendar(@Parameter(hidden = true) Ator ator,
                                         @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                         @Valid @RequestBody AgendamentoForm formulario) {
        return service.agendar(ator, id, formulario);
    }

    /** RF11: o operador do ponto recusa a solicitação, com justificativa. */
    @Operation(summary = "Recusar a solicitação", description = """
            O operador recusa o pedido, com justificativa. **Só o operador ativo do ponto**.

            Só a partir de `SOLICITADA`; `RECUSADA` é uma situação final. O `motivo` é obrigatório (até 500 caracteres). Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/recusa")
    public SolicitacaoDetalheDTO recusar(@Parameter(hidden = true) Ator ator,
                                         @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                         @Valid @RequestBody MotivoForm formulario) {
        return service.recusar(ator, id, formulario);
    }

    /** RF12: o morador dono ou o operador do ponto cancela a solicitação, com justificativa. */
    @Operation(summary = "Cancelar a solicitação", description = """
            Cancela o pedido, com justificativa. **Só o morador dono ou o operador ativo do ponto** (o administrador recebe `403`).

            Só em `SOLICITADA` ou `AGENDADA`; com o recolhimento já iniciado, concluído ou encerrado a resposta é `409`. O `motivo` é obrigatório (até 500 caracteres). Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/cancelamento")
    public SolicitacaoDetalheDTO cancelar(@Parameter(hidden = true) Ator ator,
                                          @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                          @Valid @RequestBody MotivoForm formulario) {
        return service.cancelar(ator, id, formulario);
    }

    /** RF13: o operador do ponto inicia o recolhimento de uma solicitação AGENDADA. */
    @Operation(summary = "Iniciar o recolhimento", description = """
            O operador registra que saiu para recolher. **Só o operador ativo do ponto**.

            Só a partir de `AGENDADA` (passa a `EM_RECOLHIMENTO`); pode ser antes da data marcada. Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/inicio")
    public SolicitacaoDetalheDTO iniciar(@Parameter(hidden = true) Ator ator,
                                         @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                         @Valid @RequestBody VersaoForm formulario) {
        return service.iniciar(ator, id, formulario);
    }

    /** RF13: o operador do ponto conclui o recolhimento, informando a quantidade efetiva de cada item. */
    @Operation(summary = "Concluir o recolhimento", description = """
            O operador informa a quantidade **efetivamente recolhida** de cada item. **Só o operador ativo do ponto**.

            Só a partir de `EM_RECOLHIMENTO` (passa a `CONCLUIDA`). Informe **todos os itens da solicitação, uma vez cada**; use `0` para um item não recolhido, desde que ao menos um item tenha quantidade positiva. Itens em `UN` (unidade) não aceitam fração. A data da conclusão é registrada pelo servidor. Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/conclusao")
    public SolicitacaoDetalheDTO concluir(@Parameter(hidden = true) Ator ator,
                                          @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                          @Valid @RequestBody ConclusaoForm formulario) {
        return service.concluir(ator, id, formulario);
    }

    /** RF14: o operador do ponto reagenda uma solicitação AGENDADA, com justificativa e nova data futura. */
    @Operation(summary = "Reagendar o recolhimento", description = """
            O operador marca uma nova data para uma solicitação já agendada. **Só o operador ativo do ponto**.

            Só a partir de `AGENDADA` (continua `AGENDADA`). Exige o `motivo` e uma nova data futura, diferente da atual. Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/reagendamento")
    public SolicitacaoDetalheDTO reagendar(@Parameter(hidden = true) Ator ator,
                                           @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                           @Valid @RequestBody ReagendamentoForm formulario) {
        return service.reagendar(ator, id, formulario);
    }

    /** RF14: o operador do ponto registra a tentativa sem sucesso e reagenda (EM_RECOLHIMENTO volta a AGENDADA). */
    @Operation(summary = "Registrar tentativa sem sucesso", description = """
            O operador registra que não conseguiu recolher e marca uma nova data. **Só o operador ativo do ponto**.

            Só a partir de `EM_RECOLHIMENTO` (volta a `AGENDADA`). Exige `motivo`, `informacoesTentativa` e uma nova data futura. Exige a `versao` lida pelo cliente: se estiver desatualizada, a resposta é `409` (`CONFLITO_VERSAO`).
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação atualizada, com a nova situação, a nova `versao` e o histórico.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "409", description = "Situação atual não permite a ação, ou versão desatualizada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping("/falha")
    public SolicitacaoDetalheDTO registrarFalha(@Parameter(hidden = true) Ator ator,
                                                @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                                @Valid @RequestBody FalhaForm formulario) {
        return service.registrarFalha(ator, id, formulario);
    }
}
