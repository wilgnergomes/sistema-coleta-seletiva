package br.edu.ifs.coletaseletiva.dto;

public record EnderecoPontoDTO(
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String uf,
        String cep,
        String complemento
) {}
