package br.edu.ifs.coletaseletiva.dto;

import br.edu.ifs.coletaseletiva.enumeration.TipoResponsavel;
import io.swagger.v3.oas.annotations.media.Schema;

public record ResponsavelDTO(
        @Schema(description = "`MORADOR` ou `OPERADOR`.")
        TipoResponsavel tipo,
        @Schema(description = "Id do morador ou do operador.", example = "1")
        Integer id
) {}
