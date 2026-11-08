package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;

import java.util.List;

public record PontoColetaDTO(
        Integer id,
        String nome,
        String contato,
        String horarios,
        boolean recolhimentoDomiciliar,
        SituacaoCadastro situacao,
        EnderecoPontoDTO endereco,
        List<MaterialAceitoDTO> materiaisAceitos
) {}
