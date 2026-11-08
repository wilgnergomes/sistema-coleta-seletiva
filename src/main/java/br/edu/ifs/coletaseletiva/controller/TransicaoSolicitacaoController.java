package br.edu.ifs.coletaseletiva.controller;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.form.AgendamentoForm;
import br.edu.ifs.coletaseletiva.form.ConclusaoForm;
import br.edu.ifs.coletaseletiva.form.MotivoForm;
import br.edu.ifs.coletaseletiva.form.VersaoForm;
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

    /** RF11: o operador do ponto recusa a solicitação, com justificativa. */
    @PostMapping("/recusa")
    public SolicitacaoDetalheDTO recusar(@Parameter(hidden = true) Ator ator,
                                         @PathVariable Integer id,
                                         @Valid @RequestBody MotivoForm formulario) {
        return service.recusar(ator, id, formulario);
    }

    /** RF12: o morador dono ou o operador do ponto cancela a solicitação, com justificativa. */
    @PostMapping("/cancelamento")
    public SolicitacaoDetalheDTO cancelar(@Parameter(hidden = true) Ator ator,
                                          @PathVariable Integer id,
                                          @Valid @RequestBody MotivoForm formulario) {
        return service.cancelar(ator, id, formulario);
    }

    /** RF13: o operador do ponto inicia o recolhimento de uma solicitação AGENDADA. */
    @PostMapping("/inicio")
    public SolicitacaoDetalheDTO iniciar(@Parameter(hidden = true) Ator ator,
                                         @PathVariable Integer id,
                                         @Valid @RequestBody VersaoForm formulario) {
        return service.iniciar(ator, id, formulario);
    }

    /** RF13: o operador do ponto conclui o recolhimento, informando a quantidade efetiva de cada item. */
    @PostMapping("/conclusao")
    public SolicitacaoDetalheDTO concluir(@Parameter(hidden = true) Ator ator,
                                          @PathVariable Integer id,
                                          @Valid @RequestBody ConclusaoForm formulario) {
        return service.concluir(ator, id, formulario);
    }
}
