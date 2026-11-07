package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.EnderecoDTO;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.service.EnderecoService;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/moradores/{moradorId}/enderecos")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService service;

    @PostMapping
    public ResponseEntity<EnderecoDTO> criar(@Parameter(hidden = true) Ator ator,
                                             @PathVariable Integer moradorId,
                                             @Valid @RequestBody EnderecoForm formulario) {
        EnderecoDTO criado = service.criar(ator, moradorId, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(local).body(criado);
    }

    @GetMapping
    public List<EnderecoDTO> listar(@Parameter(hidden = true) Ator ator, @PathVariable Integer moradorId) {
        return service.listar(ator, moradorId);
    }

    @PutMapping("/{id}")
    public EnderecoDTO atualizar(@Parameter(hidden = true) Ator ator,
                                 @PathVariable Integer moradorId,
                                 @PathVariable Integer id,
                                 @Valid @RequestBody EnderecoForm formulario) {
        return service.atualizar(ator, moradorId, id, formulario);
    }
}
