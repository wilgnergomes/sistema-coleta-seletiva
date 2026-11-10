package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.EnderecoDTO;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.service.EnderecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;


@Tag(name = "Endereços")
@RestController
@RequestMapping("/moradores/{moradorId}/enderecos")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService service;

    @Operation(summary = "Cadastrar um endereço do morador", description = """
            Cria um endereço para o morador da URL.

            **Quem pode:** o próprio morador (ativo) e o administrador. Operador recebe `403`. O dono do endereço é sempre o morador da URL; um `moradorId` no corpo é ignorado.

            O número é informado ou escrito `S/N` (sem número).
            """)
    @ApiResponse(responseCode = "201", description = "Endereço criado. O cabeçalho `Location` aponta para o novo recurso.", content = @Content(schema = @Schema(implementation = EnderecoDTO.class)), headers = @Header(name = "Location", description = "Endereço do recurso criado."))
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Morador inexistente.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PostMapping
    public ResponseEntity<EnderecoDTO> criar(@Parameter(hidden = true) Ator ator,
                                             @Parameter(description = "Identificador do morador dono dos endereços.", example = "1") @PathVariable Integer moradorId,
                                             @Valid @RequestBody EnderecoForm formulario) {
        EnderecoDTO criado = service.criar(ator, moradorId, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    @Operation(summary = "Listar os endereços do morador", description = """
            Devolve todos os endereços do morador da URL. **Quem pode:** o próprio morador e o administrador.
            """)
    @ApiResponse(responseCode = "200", description = "Endereços do morador.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Morador inexistente.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public List<EnderecoDTO> listar(@Parameter(hidden = true) Ator ator, @Parameter(description = "Identificador do morador dono dos endereços.", example = "1") @PathVariable Integer moradorId) {
        return service.listar(ator, moradorId);
    }

    @Operation(summary = "Atualizar um endereço do morador", description = """
            Substitui os dados do endereço (representação completa). O dono do endereço nunca muda.

            **Quem pode:** o próprio morador e o administrador. Um endereço que não pertence ao morador da URL é tratado como inexistente (`404`).
            """)
    @ApiResponse(responseCode = "200", description = "Endereço atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Morador ou endereço inexistente (ou de outro morador).")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping("/{id}")
    public EnderecoDTO atualizar(@Parameter(hidden = true) Ator ator,
                                 @Parameter(description = "Identificador do morador dono do endereço.", example = "1") @PathVariable Integer moradorId,
                                 @Parameter(description = "Identificador do endereço.", example = "1") @PathVariable Integer id,
                                 @Valid @RequestBody EnderecoForm formulario) {
        return service.atualizar(ator, moradorId, id, formulario);
    }
}
