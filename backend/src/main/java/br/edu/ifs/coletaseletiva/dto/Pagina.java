package br.edu.ifs.coletaseletiva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record Pagina<T>(
        @Schema(description = "Itens da página.")
        List<T> conteudo,
        @Schema(description = "Número da página atual, começando em 0.", example = "0")
        int pagina,
        @Schema(description = "Quantidade de itens por página.", example = "20")
        int tamanho,
        @Schema(description = "Total de itens em todas as páginas.")
        long totalElementos,
        @Schema(description = "Total de páginas.")
        int totalPaginas
) {

    public static <T> Pagina<T> de(Page<T> pagina) {
        return new Pagina<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages()
        );
    }
}