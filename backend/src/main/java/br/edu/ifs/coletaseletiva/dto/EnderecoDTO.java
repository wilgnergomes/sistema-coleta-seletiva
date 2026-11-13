package br.edu.ifs.coletaseletiva.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EnderecoDTO(
        @Schema(description = "Identificador do endereço.", example = "3")
        Integer id,
        @Schema(description = "Id do morador dono do endereço.", example = "1")
        Integer moradorId,
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String uf,
        String cep,
        String complemento
) {}
