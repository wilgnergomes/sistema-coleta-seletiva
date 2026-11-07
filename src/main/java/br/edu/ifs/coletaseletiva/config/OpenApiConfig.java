package br.edu.ifs.coletaseletiva.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("API do Sistema de Coleta Seletiva").version("v1"))
                .components(new Components()
                        .addSecuritySchemes("perfil", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Perfil")
                                .description("MORADOR, OPERADOR ou ADMINISTRADOR"))
                        .addSecuritySchemes("usuarioId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Usuario-Id")
                                .description("Id do morador ou operador (dispensado para ADMINISTRADOR)")))
                .addSecurityItem(new SecurityRequirement().addList("perfil").addList("usuarioId"));
    }
}