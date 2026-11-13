package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoResumoDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.form.FiltroSolicitacao;
import br.edu.ifs.coletaseletiva.form.SolicitacaoCorrecaoForm;
import br.edu.ifs.coletaseletiva.form.SolicitacaoForm;
import br.edu.ifs.coletaseletiva.service.SolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.PAGE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SIZE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.DE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.ATE_DESCRICAO;

@Tag(name = "Solicitações")
@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoService service;

    @Operation(summary = "Criar uma solicitação de recolhimento", description = """
            Cria um pedido de recolhimento em um ponto de coleta. **Só o morador ativo**, e sempre para si mesmo (o morador vem do cabeçalho, nunca do corpo); administrador e operador recebem `403`.

            O ponto precisa estar ativo, fazer recolhimento domiciliar e aceitar cada material (ativo, sem repetir e dentro do limite). O endereço precisa ser do morador. A solicitação nasce `SOLICITADA`, com `versao` 0, protocolo gerado e histórico inicial; qualquer falha não grava nada.
            """)
    @ApiResponse(responseCode = "201", description = "Solicitação criada. O cabeçalho `Location` aponta para o novo recurso.", content = @Content(schema = @Schema(implementation = SolicitacaoDetalheDTO.class)), headers = @Header(name = "Location", description = "Endereço do recurso criado."))
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Ponto, material ou endereço inexistente (ou de outro morador).")
    @ApiResponse(responseCode = "409", description = "Ponto ou materiais incompatíveis com a solicitação.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping
    public ResponseEntity<SolicitacaoDetalheDTO> criar(@Parameter(hidden = true) Ator ator,
                                                       @Valid @RequestBody SolicitacaoForm formulario) {
        SolicitacaoDetalheDTO criada = service.criar(ator, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(local).body(criada);
    }

    /** Consulta com filtros opcionais e paginação (RF09); o resultado respeita o contexto do ator. */
    @Operation(summary = "Consultar solicitações", description = """
            Lista as solicitações **do contexto do ator**: o morador vê as suas; o operador, as do seu ponto; o administrador, todas. Os filtros só restringem o resultado, nunca o ampliam.

            Ordem: da mais recente para a mais antiga. O período usa a **data de criação**. A lista traz o resumo; itens e histórico ficam em `GET /solicitacoes/{id}`.
            """)
    @ApiResponse(responseCode = "200", description = "Página de solicitações.")
    @ApiResponse(responseCode = "400", description = "Parâmetro inválido (por exemplo, `ate` anterior a `de`).")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public Pagina<SolicitacaoResumoDTO> listar(
            @Parameter(hidden = true) Ator ator,
            @Parameter(description = "Só solicitações nesta situação.")
            @RequestParam(required = false) SituacaoSolicitacao situacao,
            @Parameter(description = "Só solicitações deste ponto de coleta.", example = "1")
            @RequestParam(required = false) Integer pontoId,
            @Parameter(description = DE_DESCRICAO)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = ATE_DESCRICAO)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @Parameter(description = PAGE_DESCRICAO)
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @Parameter(description = SIZE_DESCRICAO)
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {
        return service.listar(ator, new FiltroSolicitacao(situacao, pontoId, de, ate), page, size);
    }

    /** Detalhe com itens e histórico, visível ao dono, ao operador do ponto e ao administrador. */
    @Operation(summary = "Consultar os detalhes de uma solicitação", description = """
            Traz itens, situação, datas e o **histórico completo**. **Quem pode:** o morador dono, o operador do ponto e o administrador. Fora desse contexto a resposta é `404`.
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação encontrada.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação inexistente ou fora do contexto do ator.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping("/{id}")
    public SolicitacaoDetalheDTO buscar(@Parameter(hidden = true) Ator ator, @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @Operation(summary = "Corrigir itens e endereço antes do agendamento", description = """
            Troca endereço, observações e itens de uma solicitação. **Só o morador dono**, e **só enquanto está `SOLICITADA`**. As regras de compatibilidade da criação são conferidas de novo. Exige a `versao` lida pelo cliente.
            """)
    @ApiResponse(responseCode = "200", description = "Solicitação corrigida.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Solicitação, endereço ou material inexistente.")
    @ApiResponse(responseCode = "409", description = "Fora de `SOLICITADA`, versão desatualizada ou itens incompatíveis.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping("/{id}")
    public SolicitacaoDetalheDTO corrigir(@Parameter(hidden = true) Ator ator,
                                          @Parameter(description = "Identificador da solicitação.", example = "1") @PathVariable Integer id,
                                          @Valid @RequestBody SolicitacaoCorrecaoForm formulario) {
        return service.corrigir(ator, id, formulario);
    }
}
