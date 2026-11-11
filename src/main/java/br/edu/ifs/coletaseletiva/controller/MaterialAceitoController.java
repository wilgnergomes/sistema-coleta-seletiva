package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.form.VinculoForm;
import br.edu.ifs.coletaseletiva.service.MaterialAceitoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Materiais aceitos")
@RestController
@RequestMapping("/pontos-coleta/{pontoId}/materiais/{materialId}")
@RequiredArgsConstructor
public class MaterialAceitoController {

    private final MaterialAceitoService service;

    /** PUT é idempotente: repetir a mesma chamada deixa o mesmo estado. Devolve 200 ao criar e ao atualizar. */
    @Operation(summary = "Vincular um material a um ponto, ou atualizar o vínculo", description = """
            Faz o ponto de coleta aceitar o material. **Só o administrador**.

            A operação é **idempotente**: se o vínculo não existe, é criado; se já existe, suas observações e limite são atualizados. Devolve `200` nos dois casos. O mesmo par ponto-material nunca se repete. Ponto e material podem estar inativos.
            """)
    @ApiResponse(responseCode = "200", description = "Vínculo criado ou atualizado.")
    @ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Ponto ou material inexistente.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @PutMapping
    public MaterialAceitoDTO vincular(@Parameter(hidden = true) Ator ator,
                                      @Parameter(description = "Identificador do ponto de coleta.", example = "1") @PathVariable Integer pontoId,
                                      @Parameter(description = "Identificador do material.", example = "1") @PathVariable Integer materialId,
                                      @Valid @RequestBody VinculoForm formulario) {
        return service.vincular(ator, pontoId, materialId, formulario);
    }

    @Operation(summary = "Remover o vínculo entre material e ponto", description = """
            Faz o ponto deixar de aceitar o material. **Só o administrador**.

            É bloqueado com `409` enquanto houver solicitação aberta (`SOLICITADA`, `AGENDADA` ou `EM_RECOLHIMENTO`) **desse ponto contendo item desse material**. Solicitações já encerradas, de outro material ou de outro ponto não bloqueiam.
            """)
    @ApiResponse(responseCode = "204", description = "Vínculo removido (sem corpo na resposta).")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "404", description = "Vínculo inexistente.")
    @ApiResponse(responseCode = "409", description = "Há solicitação aberta com o material neste ponto.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @DeleteMapping
    public ResponseEntity<Void> remover(@Parameter(hidden = true) Ator ator,
                                        @Parameter(description = "Identificador do ponto de coleta.", example = "1") @PathVariable Integer pontoId,
                                        @Parameter(description = "Identificador do material.", example = "1") @PathVariable Integer materialId) {
        service.remover(ator, pontoId, materialId);
        return ResponseEntity.noContent().build();
    }
}
