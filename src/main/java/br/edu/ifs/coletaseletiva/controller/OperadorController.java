package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.OperadorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.form.OperadorForm;
import br.edu.ifs.coletaseletiva.service.OperadorService;
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

@RestController
@RequestMapping("/operadores")
@RequiredArgsConstructor
public class OperadorController {

    private final OperadorService service;

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

    @GetMapping
    public Pagina<OperadorDTO> listar(
            @Parameter(hidden = true) Ator ator,
            @RequestParam(required = false) Integer pontoId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        return service.listar(ator, pontoId, page, size);
    }

    @GetMapping("/{id}")
    public OperadorDTO buscar(@Parameter(hidden = true) Ator ator, @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @PutMapping("/{id}")
    public OperadorDTO atualizar(@Parameter(hidden = true) Ator ator,
                                 @PathVariable Integer id,
                                 @Valid @RequestBody OperadorForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}
