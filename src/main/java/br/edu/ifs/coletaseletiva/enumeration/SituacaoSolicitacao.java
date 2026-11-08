package br.edu.ifs.coletaseletiva.enumeration;

/** Situações da solicitação (ADR 0012). As transições permitidas (RN06) entram nos RF10 a RF14. */
public enum SituacaoSolicitacao {
    SOLICITADA,
    AGENDADA,
    EM_RECOLHIMENTO,
    CONCLUIDA,
    RECUSADA,
    CANCELADA
}
