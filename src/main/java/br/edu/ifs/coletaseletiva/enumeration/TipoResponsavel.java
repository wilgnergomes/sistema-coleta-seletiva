package br.edu.ifs.coletaseletiva.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/** Quem fez uma alteração registrada no histórico: um morador ou um operador (ADR 0011). */
@Schema(description = """
        Quem fez uma mudança registrada no histórico: `MORADOR` ou `OPERADOR`.
        """)
public enum TipoResponsavel {
    MORADOR,
    OPERADOR
}
