package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoResumoDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.form.FiltroSolicitacao;
import br.edu.ifs.coletaseletiva.form.SolicitacaoCorrecaoForm;
import br.edu.ifs.coletaseletiva.form.SolicitacaoForm;
import br.edu.ifs.coletaseletiva.service.SolicitacaoService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;

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

    /** Consulta com filtros opcionais e paginação (RF09); o resultado respeita o contexto do ator. */
    @GetMapping
    public Pagina<SolicitacaoResumoDTO> listar(
            @Parameter(hidden = true) Ator ator,
            @RequestParam(required = false) SituacaoSolicitacao situacao,
            @RequestParam(required = false) Integer pontoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {
        return service.listar(ator, new FiltroSolicitacao(situacao, pontoId, de, ate), page, size);
    }

    /** Detalhe com itens e histórico, visível ao dono, ao operador do ponto e ao administrador. */
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
