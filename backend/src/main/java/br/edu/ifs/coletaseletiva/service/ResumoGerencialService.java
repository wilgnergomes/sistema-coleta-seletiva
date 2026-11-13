package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.config.FusoNegocio;
import br.edu.ifs.coletaseletiva.dto.ResumoGerencialDTO;
import br.edu.ifs.coletaseletiva.dto.SituacaoTotalDTO;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ParametroInvalidoException;
import br.edu.ifs.coletaseletiva.repository.ResumoGerencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Resumo gerencial (RF15), só para o administrador. Dois indicadores com datas de referência diferentes (ADR 0014): o
 * número de solicitações por situação usa a data de criação e as quantidades recolhidas usam a data de conclusão. O
 * período chega como datas ({@code de} e {@code ate}, ambas inclusive) e é convertido pelo fuso de negócio em um
 * intervalo semiaberto: do início do dia de {@code de} até o início do dia seguinte a {@code ate}.
 */
@Service
@RequiredArgsConstructor
public class ResumoGerencialService {

    private final ResumoGerencialRepository repository;
    private final FusoNegocio fusoNegocio;

    @Transactional(readOnly = true)
    public ResumoGerencialDTO resumir(Ator ator, LocalDate de, LocalDate ate) {
        if (ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.resumo.somente-administrador");
        }
        if (de != null && ate != null && ate.isBefore(de)) {
            throw new ParametroInvalidoException("ate", "erro.solicitacao.periodo-invalido");
        }

        OffsetDateTime inicio = (de == null) ? null : fusoNegocio.inicioDoDia(de);
        OffsetDateTime fim = (ate == null) ? null : fusoNegocio.inicioDoDia(ate.plusDays(1));

        Map<SituacaoSolicitacao, Long> contagem = repository.contarPorSituacao(inicio, fim);
        // Sempre as seis situações, na ordem do enum; a que não tem ocorrência vale zero.
        List<SituacaoTotalDTO> porSituacao = Arrays.stream(SituacaoSolicitacao.values())
                .map(situacao -> new SituacaoTotalDTO(situacao, contagem.getOrDefault(situacao, 0L)))
                .toList();

        return new ResumoGerencialDTO(porSituacao, repository.somarQuantidadesRecolhidas(inicio, fim));
    }
}
