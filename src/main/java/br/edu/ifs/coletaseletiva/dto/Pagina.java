package br.edu.ifs.coletaseletiva.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record Pagina<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
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