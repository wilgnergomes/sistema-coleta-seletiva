package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.FiltroPontoColeta;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.service.PontoColetaService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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

    /** Pesquisa pública (RF06) e listagem administrativa (RF04): filtros opcionais e paginação. */
    @GetMapping
    public Pagina<PontoColetaDTO> listar(
            @Parameter(hidden = true) Optional<Ator> ator,
            @RequestParam(required = false)
            @Size(max = 120, message = "{pesquisa.nome.tamanho-maximo}") String nome,
            @RequestParam(required = false)
            @Size(max = 100, message = "{pesquisa.municipio.tamanho-maximo}") String municipio,
            @RequestParam(required = false)
            @Size(max = 100, message = "{pesquisa.bairro.tamanho-maximo}") String bairro,
            @RequestParam(required = false) Integer materialId,
            @RequestParam(required = false) SituacaoCadastro situacao,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{paginacao.page.minimo}") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{paginacao.size.minimo}")
            @Max(value = 100, message = "{paginacao.size.maximo}") int size) {

        FiltroPontoColeta filtro = new FiltroPontoColeta(nome, municipio, bairro, materialId, situacao);
        return service.listar(ator, filtro, page, size);
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
