package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.form.VinculoForm;
import br.edu.ifs.coletaseletiva.service.MaterialAceitoService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pontos-coleta/{pontoId}/materiais/{materialId}")
@RequiredArgsConstructor
public class MaterialAceitoController {

    private final MaterialAceitoService service;

    /** PUT é idempotente: repetir a mesma chamada deixa o mesmo estado. Devolve 200 ao criar e ao atualizar. */
    @PutMapping
    public MaterialAceitoDTO vincular(@Parameter(hidden = true) Ator ator,
                                      @PathVariable Integer pontoId,
                                      @PathVariable Integer materialId,
                                      @Valid @RequestBody VinculoForm formulario) {
        return service.vincular(ator, pontoId, materialId, formulario);
    }

    @DeleteMapping
    public ResponseEntity<Void> remover(@Parameter(hidden = true) Ator ator,
                                        @PathVariable Integer pontoId,
                                        @PathVariable Integer materialId) {
        service.remover(ator, pontoId, materialId);
        return ResponseEntity.noContent().build();
    }
}
