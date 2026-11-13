package br.edu.ifs.coletaseletiva.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Fuso de negócio (ADR 0014): é nele que uma data como "2026-11-01" ganha começo e fim. O banco guarda
 * instantes; só a aplicação sabe em que fuso o "dia" do usuário começa. Fica em um único ponto para que,
 * se o fuso mudar, só a propriedade {@code coleta.fuso-negocio} precise ser alterada.
 */
@Component
public class FusoNegocio {

    private final ZoneId zona;

    public FusoNegocio(@Value("${coleta.fuso-negocio:America/Sao_Paulo}") String fuso) {
        this.zona = ZoneId.of(fuso);
    }

    /** Instante em que o dia começa (00h00) no fuso de negócio. */
    public OffsetDateTime inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(zona).toOffsetDateTime();
    }
}
