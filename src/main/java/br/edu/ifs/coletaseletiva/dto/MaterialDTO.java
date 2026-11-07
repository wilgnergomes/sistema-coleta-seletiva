package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;

public record MaterialDTO(
        Integer id,
        String nome,
        String descricao,
        Unidade unidade,
        String orientacao,
        SituacaoCadastro situacao
) {}
