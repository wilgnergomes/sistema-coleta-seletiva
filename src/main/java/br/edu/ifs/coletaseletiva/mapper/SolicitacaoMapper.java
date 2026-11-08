package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.HistoricoDTO;
import br.edu.ifs.coletaseletiva.dto.ItemDTO;
import br.edu.ifs.coletaseletiva.dto.ResponsavelDTO;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoDetalheDTO;
import br.edu.ifs.coletaseletiva.dto.SolicitacaoResumoDTO;
import br.edu.ifs.coletaseletiva.entity.Historico;
import br.edu.ifs.coletaseletiva.entity.ItemSolicitacao;
import br.edu.ifs.coletaseletiva.entity.Solicitacao;
import br.edu.ifs.coletaseletiva.enumeration.TipoResponsavel;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SolicitacaoMapper {

    public SolicitacaoDetalheDTO paraDetalhe(Solicitacao s, List<Historico> historico) {
        return paraDetalhe(s, historico, s.getVersao());
    }

    /** Permite informar a versão que o cliente vai ver, quando o incremento só se completa no commit. */
    public SolicitacaoDetalheDTO paraDetalhe(Solicitacao s, List<Historico> historico, Integer versao) {
        return new SolicitacaoDetalheDTO(
                s.getId(),
                s.getProtocolo(),
                s.getMorador().getId(),
                s.getPonto().getId(),
                s.getEnderecoCopia(),
                s.getSituacao(),
                s.getCriadaEm(),
                s.getAgendadaPara(),
                s.getConcluidaEm(),
                s.getObservacoes(),
                versao,
                s.getItens().stream()
                        .sorted(Comparator.comparing(i -> i.getMaterial().getId()))
                        .map(this::paraItem)
                        .toList(),
                historico.stream().map(this::paraHistorico).toList()
        );
    }

    /** Só lê colunas da própria solicitação e os ids das relações, sem carregar itens nem histórico. */
    public SolicitacaoResumoDTO paraResumo(Solicitacao s) {
        return new SolicitacaoResumoDTO(
                s.getId(),
                s.getProtocolo(),
                s.getMorador().getId(),
                s.getPonto().getId(),
                s.getEnderecoCopia(),
                s.getSituacao(),
                s.getCriadaEm(),
                s.getAgendadaPara(),
                s.getConcluidaEm(),
                s.getObservacoes(),
                s.getVersao()
        );
    }

    private ItemDTO paraItem(ItemSolicitacao i) {
        return new ItemDTO(i.getMaterial().getId(), i.getMaterial().getNome(), i.getUnidadeCopia(),
                i.getQuantidadeEstimada(), i.getQuantidadeEfetiva());
    }

    private HistoricoDTO paraHistorico(Historico h) {
        ResponsavelDTO responsavel = (h.getOperador() != null)
                ? new ResponsavelDTO(TipoResponsavel.OPERADOR, h.getOperador().getId())
                : new ResponsavelDTO(TipoResponsavel.MORADOR, h.getMorador().getId());
        return new HistoricoDTO(h.getId(), h.getSituacaoAnterior(), h.getSituacaoNova(), h.getData(), responsavel,
                h.getMotivo(), h.getAgendamento(), h.getInformacoesTentativa());
    }
}
