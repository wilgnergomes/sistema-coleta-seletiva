package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.service.PontoColetaService;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/pontos-coleta")
@RequiredArgsConstructor
public class PontoColetaController {

    private final PontoColetaService service;

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

    /** Por enquanto só situação e paginação; nome, município, bairro e material entram no RF06. */
    @GetMapping
    public Pagina<PontoColetaDTO> listar(
            @Parameter(hidden = true) Optional<Ator> ator,
            @RequestParam(required = false) SituacaoCadastro situacao,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        return service.listar(ator, situacao, page, size);
    }

    /** Entrega mínima antecipada do RF06, para o Location do POST apontar para um recurso consultável. */
    @GetMapping("/{id}")
    public PontoColetaDTO buscar(@Parameter(hidden = true) Optional<Ator> ator, @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @PutMapping("/{id}")
    public PontoColetaDTO atualizar(@Parameter(hidden = true) Ator ator,
                                    @PathVariable Integer id,
                                    @Valid @RequestBody PontoColetaForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}
