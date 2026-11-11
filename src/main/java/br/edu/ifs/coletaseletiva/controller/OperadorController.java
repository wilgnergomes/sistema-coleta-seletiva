package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.OperadorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.form.OperadorForm;
import br.edu.ifs.coletaseletiva.service.OperadorService;
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

@Tag(name = "Operadores")
@RestController
@RequestMapping("/operadores")
@RequiredArgsConstructor
public class OperadorController {

    private final OperadorService service;

    @Operation(summary = "Cadastrar um operador", description = """
            Cria um operador e o vincula a um ponto de coleta. **Só o administrador**.

            Cada operador pertence a **um único ponto**. O e-mail é guardado em minúsculas e não pode repetir (`409`). `situacao` é opcional (padrão `ATIVO`).
            """)
    @ApiResponse(responseCode = "201", description = "Operador criado. O cabeçalho `Location` aponta para o novo recurso.", content = @Content(schema = @Schema(implementation = OperadorDTO.class)), headers = @Header(name = "Location", description = "Endereço do recurso criado."))
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Ponto de coleta inexistente.")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping
    public ResponseEntity<OperadorDTO> criar(@Parameter(hidden = true) Ator ator,
                                             @Valid @RequestBody OperadorForm formulario) {
        OperadorDTO criado = service.criar(ator, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    @Operation(summary = "Listar operadores", description = """
            Devolve uma página de operadores. **Só o administrador**. Pode filtrar por ponto de coleta.
            """)
    @ApiResponse(responseCode = "200", description = "Página de operadores.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public Pagina<OperadorDTO> listar(
            @Parameter(hidden = true) Ator ator,
            @Parameter(description = "Só operadores deste ponto de coleta. Um id inexistente devolve lista vazia.", example = "1")
            @RequestParam(required = false) Integer pontoId,
            @Parameter(description = PAGE_DESCRICAO)
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @Parameter(description = SIZE_DESCRICAO)
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        return service.listar(ator, pontoId, page, size);
    }

    @Operation(summary = "Consultar um operador", description = """
            Devolve os dados de um operador. **Só o administrador**.
            """)
    @ApiResponse(responseCode = "200", description = "Operador encontrado.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Operador inexistente.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping("/{id}")
    public OperadorDTO buscar(@Parameter(hidden = true) Ator ator, @Parameter(description = "Identificador do operador.", example = "1") @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @Operation(summary = "Atualizar, mover ou inativar um operador", description = """
            Substitui os dados do operador (representação completa). **Só o administrador**.

            Permite trocar o ponto de coleta (`pontoId`) e inativar o operador (`situacao`). O e-mail continua único (`409`).
            """)
    @ApiResponse(responseCode = "200", description = "Operador atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Operador ou ponto de coleta inexistente.")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado por outro operador.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping("/{id}")
    public OperadorDTO atualizar(@Parameter(hidden = true) Ator ator,
                                 @Parameter(description = "Identificador do operador.", example = "1") @PathVariable Integer id,
                                 @Valid @RequestBody OperadorForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}
