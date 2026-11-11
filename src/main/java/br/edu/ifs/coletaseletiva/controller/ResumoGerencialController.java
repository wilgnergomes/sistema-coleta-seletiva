package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.ResumoGerencialDTO;
import br.edu.ifs.coletaseletiva.service.ResumoGerencialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.DE_DESCRICAO;
import static br.edu.ifs.coletaseletiva.config.OpenApiConfig.ATE_DESCRICAO;

@Tag(name = "Resumo gerencial")
@RestController
@RequestMapping("/resumo-gerencial")
@RequiredArgsConstructor
public class ResumoGerencialController {

    private final ResumoGerencialService service;

    /** RF15: indicadores para o administrador. {@code de} e {@code ate} (AAAA-MM-DD) são opcionais e inclusivos. */
    @Operation(summary = "Consultar o resumo gerencial", description = """
            Indicadores para o administrador. **Só o administrador** (os demais perfis, ou a falta de identificação, recebem `403`).

            Dois blocos:

            1. **Solicitações por situação**, filtradas pela data de criação. Traz sempre as seis situações, com total `0` quando não há ocorrência.
            2. **Quantidades recolhidas** por material e unidade, somando só a quantidade **efetiva** de solicitações `CONCLUIDA`, filtradas pela data de conclusão. Unidades diferentes (KG, L, UN) nunca são somadas entre si.

            Sem `de` e `ate`, considera todo o período.
            """)
    @ApiResponse(responseCode = "200", description = "Resumo gerencial.")
    @ApiResponse(responseCode = "400", description = "Data malformada ou `ate` anterior a `de`.")
    @ApiResponse(responseCode = "403", description = "Ator sem permissão ou cabeçalhos de identificação ausentes ou inválidos.")
    @ApiResponse(responseCode = "500", description = "Erro inesperado.")
    @GetMapping
    public ResumoGerencialDTO resumir(
            @Parameter(hidden = true) Ator ator,
            @Parameter(description = DE_DESCRICAO, example = "2026-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @Parameter(description = ATE_DESCRICAO, example = "2026-11-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.resumir(ator, de, ate);
    }
}
