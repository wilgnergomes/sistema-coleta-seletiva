package br.edu.ifs.coletaseletiva.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = """
        Situação de um cadastro (morador, material, ponto de coleta ou operador): `ATIVO` está em uso; `INATIVO` foi desativado, sem ser apagado.
        """)
public enum SituacaoCadastro {
    ATIVO,
    INATIVO
}
