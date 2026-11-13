package br.edu.ifs.coletaseletiva.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.TransformedResource;

import java.nio.charset.StandardCharsets;

/**
 * Ajuste visual do Swagger UI. O estilo padrão do código em linha (as partes em destaque dentro dos textos) tem
 * margem vertical maior que a altura da linha e cobre o texto da linha de cima. O springdoc não tem propriedade para
 * CSS próprio, então este transformador acrescenta uma regra ao {@code swagger-initializer.js}, que ele já
 * personaliza, mantendo todo o comportamento padrão.
 */
@Configuration
public class SwaggerUiConfig {

    private static final String ESTILO = """
            .swagger-ui .markdown p, .swagger-ui .markdown li, .swagger-ui .renderedMarkdown p,
            .swagger-ui .renderedMarkdown li, .swagger-ui .markdown td, .swagger-ui .renderedMarkdown td {
              line-height: 1.8;
            }
            .swagger-ui .markdown code, .swagger-ui .renderedMarkdown code {
              padding: 0 6px;
              line-height: inherit;
              display: inline;
              box-decoration-break: clone;
              -webkit-box-decoration-break: clone;
            }
            """;

    private static final String SCRIPT = "\n;(function () {\n"
            + "  var estilo = document.createElement('style');\n"
            + "  estilo.textContent = `" + ESTILO + "`;\n"
            + "  document.head.appendChild(estilo);\n"
            + "})();\n";

    @Bean
    SwaggerIndexTransformer indexComEstilo(SwaggerUiConfigProperties configuracao,
                                           SwaggerUiOAuthProperties oauth,
                                           SwaggerWelcomeCommon boasVindas,
                                           ObjectMapperProvider objectMapperProvider) {
        SwaggerIndexPageTransformer padrao =
                new SwaggerIndexPageTransformer(configuracao, oauth, boasVindas, objectMapperProvider);
        return (requisicao, recurso, cadeia) -> {
            Resource transformado = padrao.transform(requisicao, recurso, cadeia);
            if (!recurso.getURL().toString().contains("swagger-initializer.js")) {
                return transformado;
            }
            String js = new String(transformado.getInputStream().readAllBytes(), StandardCharsets.UTF_8) + SCRIPT;
            return new TransformedResource(transformado, js.getBytes(StandardCharsets.UTF_8));
        };
    }
}
