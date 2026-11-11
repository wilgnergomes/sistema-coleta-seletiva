package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.MaterialForm;
import br.edu.ifs.coletaseletiva.service.MaterialService;
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

@Tag(name = "Materiais")
@RestController
@RequestMapping("/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService service;

    @Operation(summary = "Cadastrar um material reciclável", description = """
            Cria um material. **Só o administrador** (outros perfis recebem `403`).

            * O nome é único, sem diferenciar maiúsculas de minúsculas (`409` se repetir).
            * `situacao` é opcional e, quando omitida, o material nasce `ATIVO`. O administrador pode cadastrá-lo já `INATIVO`.
            """)
    @ApiResponse(responseCode = "201", description = "Material criado. O cabeçalho `Location` aponta para o novo recurso.", content = @Content(schema = @Schema(implementation = MaterialDTO.class)), headers = @Header(name = "Location", description = "Endereço do recurso criado."))
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "409", description = "Nome de material já cadastrado.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping
    public ResponseEntity<MaterialDTO> criar(@Parameter(hidden = true) Ator ator,
                                             @Valid @RequestBody MaterialForm formulario) {
        MaterialDTO criado = service.criar(ator, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    /** Consulta pública: o ator é opcional (Optional&lt;Ator&gt;). */
    @Operation(summary = "Listar materiais recicláveis", description = """
            **Consulta pública**: funciona sem identificação. Sem ator (ou como morador ou operador) devolve **só materiais `ATIVO`**. Como administrador, enxerga também os inativos e pode filtrar por `situacao`.
            """)
    @ApiResponse(responseCode = "200", description = "Página de materiais.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public Pagina<MaterialDTO> listar(
            @Parameter(hidden = true) Optional<Ator> ator,
            @Parameter(description = SITUACAO_CADASTRO_DESCRICAO)
            @RequestParam(required = false) SituacaoCadastro situacao,
            @Parameter(description = PAGE_DESCRICAO)
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @Parameter(description = SIZE_DESCRICAO)
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        return service.listar(ator, situacao, page, size);
    }

    @Operation(summary = "Consultar um material", description = """
            **Consulta pública**: funciona sem identificação. Um material `INATIVO` só é visível para o administrador; para os demais a resposta é `404`.
            """)
    @ApiResponse(responseCode = "200", description = "Material encontrado.")
    @ApiResponse(responseCode = "404", description = "Material inexistente ou inativo (para quem não é administrador).")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping("/{id}")
    public MaterialDTO buscar(@Parameter(hidden = true) Optional<Ator> ator, @Parameter(description = "Identificador do material.", example = "1") @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @Operation(summary = "Atualizar ou inativar um material", description = """
            Substitui os dados do material (representação completa). **Só o administrador**.

            * A inativação é feita pelo campo `situacao` (`INATIVO`).
            * A `unidade` **não pode mudar** se o material já tem item em solicitação ou vínculo com ponto de coleta (`409`).
            * O nome continua único (`409`).
            """)
    @ApiResponse(responseCode = "200", description = "Material atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Material inexistente.")
    @ApiResponse(responseCode = "409", description = "Nome repetido ou unidade bloqueada.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping("/{id}")
    public MaterialDTO atualizar(@Parameter(hidden = true) Ator ator,
                                 @Parameter(description = "Identificador do material.", example = "1") @PathVariable Integer id,
                                 @Valid @RequestBody MaterialForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}
