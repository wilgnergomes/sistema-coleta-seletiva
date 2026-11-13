package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.FiltroPontoColeta;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.service.PontoColetaService;
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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
import java.util.Optional;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.PAGE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SIZE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SITUACAO_CADASTRO_DESCRICAO;

@Tag(name = "Pontos de coleta")
@RestController
@RequestMapping("/pontos-coleta")
@RequiredArgsConstructor
public class PontoColetaController {

    private final PontoColetaService service;

    @Operation(summary = "Cadastrar um ponto de coleta", description = """
            Cria um ponto de coleta com o endereço embutido. **Só o administrador**.

            `situacao` é opcional (padrão `ATIVO`); `recolhimentoDomiciliar` é opcional (padrão `false`). Depois de criar, informe quais materiais o ponto aceita em **Materiais aceitos**.
            """)
    @ApiResponse(responseCode = "201", description = "Ponto criado. O cabeçalho `Location` aponta para o novo recurso.", content = @Content(schema = @Schema(implementation = PontoColetaDTO.class)), headers = @Header(name = "Location", description = "Endereço do recurso criado."))
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping
    public ResponseEntity<PontoColetaDTO> criar(@Parameter(hidden = true) Ator ator,
                                                @Valid @RequestBody PontoColetaForm formulario) {
        PontoColetaDTO criado = service.criar(ator, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    /** Pesquisa pública (RF06) e listagem administrativa (RF04): filtros opcionais e paginação. */
    @Operation(summary = "Pesquisar pontos de coleta", description = """
            **Consulta pública**: funciona sem identificação. Sem ator (ou como morador ou operador) devolve **só pontos `ATIVO`**. O administrador enxerga também os inativos e pode filtrar por `situacao`.

            Os filtros são opcionais e se combinam (E). Textos não diferenciam maiúsculas de minúsculas nem espaços nas pontas, mas **acentos contam**. `nome` procura por trecho; `municipio` e `bairro` exigem valor igual. Um `materialId` inexistente devolve lista vazia.
            """)
    @ApiResponse(responseCode = "200", description = "Página de pontos de coleta.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public Pagina<PontoColetaDTO> listar(
            @Parameter(hidden = true) Optional<Ator> ator,
            @Parameter(description = "Trecho do nome do ponto (até 120 caracteres).", example = "Verde")
            @RequestParam(required = false)
            @Size(max = 120, message = "{pesquisa.nome.tamanho-maximo}") String nome,
            @Parameter(description = "Município, valor exato (até 100 caracteres).", example = "Lagarto")
            @RequestParam(required = false)
            @Size(max = 100, message = "{pesquisa.municipio.tamanho-maximo}") String municipio,
            @Parameter(description = "Bairro, valor exato (até 100 caracteres).", example = "Centro")
            @RequestParam(required = false)
            @Size(max = 100, message = "{pesquisa.bairro.tamanho-maximo}") String bairro,
            @Parameter(description = "Só pontos que aceitam o material com este id.", example = "1")
            @RequestParam(required = false) Integer materialId,
            @Parameter(description = SITUACAO_CADASTRO_DESCRICAO)
            @RequestParam(required = false) SituacaoCadastro situacao,
            @Parameter(description = PAGE_DESCRICAO)
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @Parameter(description = SIZE_DESCRICAO)
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        FiltroPontoColeta filtro = new FiltroPontoColeta(nome, municipio, bairro, materialId, situacao);
        return service.listar(ator, filtro, page, size);
    }

    /** Entrega mínima antecipada do RF06, para o Location do POST apontar para um recurso consultável. */
    @Operation(summary = "Consultar um ponto de coleta", description = """
            **Consulta pública**: funciona sem identificação. Exibe endereço, horários e os materiais aceitos com suas orientações. Um ponto `INATIVO` só é visível para o administrador; para os demais a resposta é `404`.
            """)
    @ApiResponse(responseCode = "200", description = "Ponto encontrado.")
    @ApiResponse(responseCode = "404", description = "Ponto inexistente ou inativo (para quem não é administrador).")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping("/{id}")
    public PontoColetaDTO buscar(@Parameter(hidden = true) Optional<Ator> ator, @Parameter(description = "Identificador do ponto de coleta.", example = "1") @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @Operation(summary = "Atualizar ou inativar um ponto de coleta", description = """
            Substitui os dados do ponto (representação completa). **Só o administrador**.

            A inativação é feita pelo campo `situacao`. **Inativar um ponto que tem solicitação aberta** (`SOLICITADA`, `AGENDADA` ou `EM_RECOLHIMENTO`) é bloqueado com `409`. Reativar e editar outros dados não são bloqueados.
            """)
    @ApiResponse(responseCode = "200", description = "Ponto atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Ponto inexistente.")
    @ApiResponse(responseCode = "409", description = "Ponto com solicitações abertas não pode ser inativado.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping("/{id}")
    public PontoColetaDTO atualizar(@Parameter(hidden = true) Ator ator,
                                    @Parameter(description = "Identificador do ponto de coleta.", example = "1") @PathVariable Integer id,
                                    @Valid @RequestBody PontoColetaForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}
