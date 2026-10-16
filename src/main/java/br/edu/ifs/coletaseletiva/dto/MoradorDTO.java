package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;

public record MoradorDTO(
        Integer id,
        String nome,
        String email,
        String fone,
        SituacaoCadastro situacao
) {}