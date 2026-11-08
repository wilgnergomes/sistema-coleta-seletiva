package br.edu.ifs.coletaseletiva.enumeration;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situações da solicitação (ADR 0012) e as transições permitidas da RN06.
 *
 * <p>A tabela é do domínio; cada endpoint escolhe quais transições expõe. Por exemplo, {@code AGENDADA -> AGENDADA}
 * existe aqui para o reagendamento (RF14), mas o agendamento (RF10) só parte de SOLICITADA.
 */
public enum SituacaoSolicitacao {
    SOLICITADA,
    AGENDADA,
    EM_RECOLHIMENTO,
    CONCLUIDA,
    RECUSADA,
    CANCELADA;

    /** Situações para as quais esta pode passar. Vazio nas situações finais. */
    public Set<SituacaoSolicitacao> proximas() {
        return switch (this) {
            case SOLICITADA -> EnumSet.of(AGENDADA, RECUSADA, CANCELADA);
            // AGENDADA -> AGENDADA é o reagendamento (RF14), que mantém a situação.
            case AGENDADA -> EnumSet.of(AGENDADA, EM_RECOLHIMENTO, CANCELADA);
            // EM_RECOLHIMENTO -> AGENDADA é o retorno após tentativa sem sucesso (RF14).
            case EM_RECOLHIMENTO -> EnumSet.of(CONCLUIDA, AGENDADA);
            case CONCLUIDA, RECUSADA, CANCELADA -> EnumSet.noneOf(SituacaoSolicitacao.class);
        };
    }

    public boolean podeIrPara(SituacaoSolicitacao destino) {
        return proximas().contains(destino);
    }

    /** CONCLUIDA, RECUSADA e CANCELADA não têm nova transição. */
    public boolean ehFinal() {
        return proximas().isEmpty();
    }

    /**
     * Situações em que a solicitação ainda está aberta (RN05): SOLICITADA, AGENDADA e EM_RECOLHIMENTO, o complemento das
     * finais. Só as abertas bloqueiam a inativação do ponto e a remoção de um vínculo de material.
     */
    public static Set<SituacaoSolicitacao> abertas() {
        EnumSet<SituacaoSolicitacao> abertas = EnumSet.noneOf(SituacaoSolicitacao.class);
        for (SituacaoSolicitacao situacao : values()) {
            if (!situacao.ehFinal()) {
                abertas.add(situacao);
            }
        }
        return abertas;
    }
}
