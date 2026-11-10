package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.form.MoradorForm;
import br.edu.ifs.coletaseletiva.service.MoradorService;
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

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.PAGE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.SIZE_DESCRICAO;

@RestController
@RequestMapping("/moradores")
@RequiredArgsConstructor
public class MoradorController {

    private final MoradorService service;

    @Tag(name = "Moradores")
    @Operation(summary = "Cadastrar um novo morador", description = """
            Cria um morador. **Operação pública**: não exige `X-Perfil`.

            * O e-mail é guardado em minúsculas, sem espaços nas pontas, e não pode repetir (`409`).
            * Todo morador nasce `ATIVO`. O campo `situacao`, se enviado, é **ignorado** na criação; só um valor fora de `ATIVO` e `INATIVO` é rejeitado (`400`).
            """)
    @ApiResponse(responseCode = "201", description = "Morador criado. O cabeçalho `Location` aponta para o novo recurso.", content = @Content(schema = @Schema(implementation = MoradorDTO.class)), headers = @Header(name = "Location", description = "Endereço do recurso criado."))
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping
    public ResponseEntity<MoradorDTO> criar(@Valid @RequestBody MoradorForm formulario) {
        MoradorDTO criado = service.criar(formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    @Tag(name = "Moradores")
    @Operation(summary = "Listar moradores", description = """
            Devolve uma página de moradores. **Só o administrador** pode listar; morador e operador recebem `403`.
            """)
    @ApiResponse(responseCode = "200", description = "Página de moradores.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public Pagina<MoradorDTO> listar(
            @Parameter(hidden = true) Ator ator,
            @Parameter(description = PAGE_DESCRICAO)
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @Parameter(description = SIZE_DESCRICAO)
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        return service.listar(ator, page, size);
    }

    @Tag(name = "Moradores")
    @Operation(summary = "Consultar um morador", description = """
            Devolve os dados de um morador.

            **Quem pode:** o administrador (qualquer morador) e o próprio morador (somente o seu cadastro). Operador recebe `403`.
            """)
    @ApiResponse(responseCode = "200", description = "Morador encontrado.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Morador inexistente.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping("/{id}")
    public MoradorDTO buscar(@Parameter(hidden = true) Ator ator, @Parameter(description = "Identificador do morador.", example = "1") @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @Tag(name = "Moradores")
    @Operation(summary = "Atualizar um morador", description = """
            Substitui nome, e-mail e telefone do morador (representação completa).

            **Quem pode:** o administrador e o próprio morador.

            * `situacao` é opcional: se omitida, a situação atual é mantida.
            * **Só o administrador altera a situação** (por exemplo, inativar um morador). Outro perfil que envie uma situação diferente da atual recebe `403`.
            * O e-mail segue as mesmas regras do cadastro: normalizado e único (`409`).
            """)
    @ApiResponse(responseCode = "200", description = "Morador atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Morador inexistente.")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado por outro morador.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping("/{id}")
    public MoradorDTO atualizar(@Parameter(hidden = true) Ator ator,
                                @Parameter(description = "Identificador do morador.", example = "1") @PathVariable Integer id,
                                @Valid @RequestBody MoradorForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}