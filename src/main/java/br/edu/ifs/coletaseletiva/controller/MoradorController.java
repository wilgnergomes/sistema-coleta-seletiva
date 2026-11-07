package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.form.MoradorAtualizacaoForm;
import br.edu.ifs.coletaseletiva.form.MoradorCriacaoForm;
import br.edu.ifs.coletaseletiva.service.MoradorService;
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
@RequestMapping("/moradores")
@RequiredArgsConstructor
public class MoradorController {

    private final MoradorService service;

    @PostMapping
    public ResponseEntity<MoradorDTO> criar(@Valid @RequestBody MoradorCriacaoForm formulario) {
        MoradorDTO criado = service.criar(formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    @GetMapping
    public Pagina<MoradorDTO> listar(
            @Parameter(hidden = true) Ator ator,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "deve ser maior ou igual a 0") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "deve ser maior ou igual a 1")
            @Max(value = 100, message = "deve ser no máximo 100") int size) {

        return service.listar(ator, page, size);
    }

    @GetMapping("/{id}")
    public MoradorDTO buscar(@Parameter(hidden = true) Ator ator, @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @PutMapping("/{id}")
    public MoradorDTO atualizar(@Parameter(hidden = true) Ator ator,
                                @PathVariable Integer id,
                                @Valid @RequestBody MoradorAtualizacaoForm formulario) {
        return service.atualizar(ator, id, formulario);
    }
}