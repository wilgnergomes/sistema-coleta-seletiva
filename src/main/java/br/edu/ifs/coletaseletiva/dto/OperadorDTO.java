package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;

public record OperadorDTO(
        Integer id,
        String nome,
        String email,
        Integer pontoId,
        SituacaoCadastro situacao
) {}
