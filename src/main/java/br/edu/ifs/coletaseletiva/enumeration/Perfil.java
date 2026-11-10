package br.edu.ifs.coletaseletiva.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Perfil de quem age, informado no cabeçalho `X-Perfil`: `ADMINISTRADOR`, `MORADOR` ou `OPERADOR`.
        """)
public enum Perfil {
    MORADOR,
    OPERADOR,
    ADMINISTRADOR
}