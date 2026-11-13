package br.edu.ifs.coletaseletiva.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/** Unidade de medida do material (ADR 0012): peso (KG), volume (L) ou peça (UN). */
@Schema(description = """
        Unidade de medida do material: `KG` (peso, em quilos), `L` (volume, em litros) ou `UN` (peça, só quantidades inteiras).
        """)
public enum Unidade {
    KG,
    L,
    UN
}
