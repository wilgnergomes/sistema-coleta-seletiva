package br.edu.ifs.coletaseletiva.dto;

public record EnderecoDTO(
        Integer id,
        Integer moradorId,
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String uf,
        String cep,
        String complemento
) {}
