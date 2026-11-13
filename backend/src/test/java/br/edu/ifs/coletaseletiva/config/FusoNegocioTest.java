package br.edu.ifs.coletaseletiva.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** O fuso de negócio (ADR 0014) decide onde um dia começa, e é configurável. */
class FusoNegocioTest {

    @Test
    void rf09_diaComecaAsTresHorasUtcEmSaoPaulo() {
        FusoNegocio fuso = new FusoNegocio("America/Sao_Paulo");

        OffsetDateTime inicio = fuso.inicioDoDia(LocalDate.of(2026, 11, 1));

        assertEquals(OffsetDateTime.parse("2026-11-01T03:00:00Z").toInstant(), inicio.toInstant());
        assertEquals("-03:00", inicio.getOffset().getId());
    }

    @Test
    void rf09_outroFusoMudaOInicioDoDiaSemAlterarOCodigo() {
        FusoNegocio utc = new FusoNegocio("UTC");

        assertEquals(OffsetDateTime.parse("2026-11-01T00:00:00Z").toInstant(),
                utc.inicioDoDia(LocalDate.of(2026, 11, 1)).toInstant());
    }

    @Test
    void rf09_fusoDesconhecidoFalhaNaInicializacao() {
        assertThrows(RuntimeException.class, () -> new FusoNegocio("Marte/Olimpo"));
    }
}
