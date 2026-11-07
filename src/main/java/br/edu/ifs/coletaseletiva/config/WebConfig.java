package br.edu.ifs.coletaseletiva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Locale PORTUGUES_BRASIL = Locale.forLanguageTag("pt-BR");

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AtorArgumentResolver());
    }

    /**
     * Escolhe o idioma da resposta pelo cabeçalho Accept-Language (ADR 0023). Suporta pt-BR e en;
     * sem o cabeçalho, ou com outro idioma, usa pt-BR. O nome do método precisa ser
     * "localeResolver" para o Spring MVC usá-lo.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolvedor = new AcceptHeaderLocaleResolver();
        resolvedor.setSupportedLocales(List.of(PORTUGUES_BRASIL, Locale.ENGLISH));
        resolvedor.setDefaultLocale(PORTUGUES_BRASIL);
        return resolvedor;
    }
}
