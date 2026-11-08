package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.ResumoGerencialDTO;
import br.edu.ifs.coletaseletiva.service.ResumoGerencialService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/resumo-gerencial")
@RequiredArgsConstructor
public class ResumoGerencialController {

    private final ResumoGerencialService service;

    /** RF15: indicadores para o administrador. {@code de} e {@code ate} (AAAA-MM-DD) são opcionais e inclusivos. */
    @GetMapping
    public ResumoGerencialDTO resumir(
            @Parameter(hidden = true) Ator ator,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.resumir(ator, de, ate);
    }
}
