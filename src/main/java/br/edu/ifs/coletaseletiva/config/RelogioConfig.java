package br.edu.ifs.coletaseletiva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * O relógio como dependência: o código de negócio pede o "agora" ao {@link Clock}, e os testes entregam um
 * relógio fixo. Assim a regra "data futura" (RF10) pode ser testada na fronteira, sem depender da hora real.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
