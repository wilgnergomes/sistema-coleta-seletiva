package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.form.AgendamentoForm;
import br.edu.ifs.coletaseletiva.service.TransicaoSolicitacaoService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ações que mudam a situação da solicitação (RF10 a RF14). Cada uma devolve a solicitação atualizada. */
@RestController
@RequestMapping("/solicitacoes/{id}")
@RequiredArgsConstructor
public class TransicaoSolicitacaoController {

    private final TransicaoSolicitacaoService service;

    /** RF10: o operador do ponto agenda o recolhimento. */
    @PostMapping("/agendamento")
    public SolicitacaoDetalheDTO agendar(@Parameter(hidden = true) Ator ator,
                                         @PathVariable Integer id,
                                         @Valid @RequestBody AgendamentoForm formulario) {
        return service.agendar(ator, id, formulario);
    }
}
