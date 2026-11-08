package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.form.SolicitacaoCorrecaoForm;
import br.edu.ifs.coletaseletiva.form.SolicitacaoForm;
import br.edu.ifs.coletaseletiva.service.SolicitacaoService;
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

@RestController
@RequestMapping("/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoController {

    private final SolicitacaoService service;

    @PostMapping
    public ResponseEntity<SolicitacaoDetalheDTO> criar(@Parameter(hidden = true) Ator ator,
                                                       @Valid @RequestBody SolicitacaoForm formulario) {
        SolicitacaoDetalheDTO criada = service.criar(ator, formulario);

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();

        return ResponseEntity.created(local).body(criada);
    }

    /** Consulta mínima por id, antecipada do RF09 (a listagem com filtros continua no RF09). */
    @GetMapping("/{id}")
    public SolicitacaoDetalheDTO buscar(@Parameter(hidden = true) Ator ator, @PathVariable Integer id) {
        return service.buscar(ator, id);
    }

    @PutMapping("/{id}")
    public SolicitacaoDetalheDTO corrigir(@Parameter(hidden = true) Ator ator,
                                          @PathVariable Integer id,
                                          @Valid @RequestBody SolicitacaoCorrecaoForm formulario) {
        return service.corrigir(ator, id, formulario);
    }
}
