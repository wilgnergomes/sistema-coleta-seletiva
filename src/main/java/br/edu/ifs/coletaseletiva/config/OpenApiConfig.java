package br.edu.ifs.coletaseletiva.config;

import br.edu.ifs.coletaseletiva.dto.RespostaErro;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuração do que o Swagger UI exibe (springdoc): texto geral da API, grupos, esquemas de identificação do ator,
 * corpo dos erros e dois ajustes aplicados a toda operação (segurança e idioma). A documentação de
 * cada endpoint fica nas anotações do próprio controller. O contrato {@code docs/api/openapi.yaml} continua sendo a
 * fonte da verdade (ADR 0017).
 */
@Configuration
public class OpenApiConfig {

    /** Textos de parâmetros que se repetem em vários endpoints. */
    public static final String PAGE_DESCRICAO = "Número da página, começando em 0.";
    public static final String SIZE_DESCRICAO = "Quantidade de itens por página, de 1 a 100.";
    public static final String DE_DESCRICAO = "Data inicial do período, **inclusiva**, no formato AAAA-MM-DD "
            + "(fuso America/Sao_Paulo). Opcional.";
    public static final String ATE_DESCRICAO = "Data final do período, **inclusiva**, no formato AAAA-MM-DD "
            + "(fuso America/Sao_Paulo). Opcional; não pode ser anterior a `de`.";
    public static final String SITUACAO_CADASTRO_DESCRICAO = "Filtra pela situação do cadastro. **Só tem efeito para o "
            + "administrador**; para os demais a consulta devolve sempre apenas registros `ATIVO`.";

    /** Textos de campos de formulário que se repetem em vários corpos. */
    public static final String VERSAO_DESCRICAO = "Versão da solicitação lida pelo cliente (campo `versao` da última "
            + "resposta). Se estiver desatualizada, a ação é rejeitada com `409` (`CONFLITO_VERSAO`).";
    public static final String MOTIVO_DESCRICAO = "Justificativa obrigatória, com até 500 caracteres. Os espaços das "
            + "pontas são removidos.";
    public static final String SITUACAO_CADASTRO_CORPO = "Situação do cadastro: `ATIVO` ou `INATIVO`. Opcional; "
            + "se omitida, vale `ATIVO` no cadastro e a situação atual é mantida na atualização.";

    private static final String JSON = "application/json";

    /** Corpo e exemplos de cada status de erro, copiados para as respostas que o controller declara só com a descrição. */
    private final Map<String, ApiResponse> errosPadrao = respostasDeErro();
    private static final String PERFIL = "perfil";
    private static final String USUARIO_ID = "usuarioId";

    private static final String DESCRICAO_GERAL = """
            API REST do **Sistema de Coleta Seletiva** (Programação Web I, IFS). Moradores pedem o recolhimento de \
            materiais recicláveis em pontos de coleta, e os operadores dos pontos agendam, recolhem e concluem os pedidos.

            ## Como começar

            1. Abra um grupo (por exemplo **Materiais**) e escolha uma operação.
            2. Se ela tiver o cadeado, clique em **Authorize** (no alto da página) e informe quem está agindo \
            (veja *Como simular o ator*). Consultas públicas, sem cadeado, funcionam sem isso.
            3. Clique em **Try it out**, preencha os parâmetros e o JSON de exemplo e clique em **Execute**.

            Uma ordem boa para testar: cadastrar um morador (`POST /moradores`, público) → cadastrar um endereço dele \
            → como administrador, cadastrar um material, um ponto de coleta e um operador → vincular o material ao \
            ponto → como morador, criar uma solicitação → como operador, agendar, iniciar e concluir.

            ## Como simular o ator

            Esta API **não tem autenticação**: não há senha nem token. O ator é escolhido por dois cabeçalhos, só para \
            demonstração acadêmica (ADR 0019). O botão **Authorize** preenche os dois.

            | Perfil (`X-Perfil`) | `X-Usuario-Id` |
            |---|---|
            | `ADMINISTRADOR` | não precisa (é ignorado) |
            | `MORADOR` | obrigatório: id de um morador existente e ativo |
            | `OPERADOR` | obrigatório: id de um operador existente e ativo |

            As permissões variam por operação e estão descritas em cada uma. Sem cabeçalho, ou com um perfil sem \
            permissão, a resposta é `403`.

            ## Convenções

            * **Erros:** sempre no formato `{ "codigo", "mensagem", "erros" }`. `erros` lista os campos inválidos.
            * **Paginação:** as listas aceitam `page` (começa em 0, padrão 0) e `size` (1 a 100, padrão 20) e devolvem \
            `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.
            * **Concorrência:** as ações sobre uma solicitação exigem a `versao` que o cliente leu. Versão \
            desatualizada resulta em `409` com código `CONFLITO_VERSAO`.
            * **Idioma:** as mensagens saem em português do Brasil (padrão) ou em inglês, conforme o cabeçalho \
            `Accept-Language` (`pt-BR` ou `en`).
            * **Datas:** `AAAA-MM-DD` nos filtros de período; data e hora com deslocamento nos agendamentos, por \
            exemplo `2026-12-15T09:00:00-03:00`.
            """;

    private static final String DESCRICAO_PERFIL = """
            Quem está agindo: `ADMINISTRADOR`, `MORADOR` ou `OPERADOR` (maiúsculas ou minúsculas).

            **Não é autenticação**: não existe senha nem token. É uma escolha de usuário para demonstração \
            acadêmica (ADR 0019).""";

    private static final String DESCRICAO_USUARIO_ID = """
            Id do morador ou do operador que está agindo. **Obrigatório** para `MORADOR` e `OPERADOR` (precisa ser \
            um usuário existente e ativo); **não é necessário** para `ADMINISTRADOR`. Deixe em branco nas operações \
            que não pedem ator.""";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API do Sistema de Coleta Seletiva")
                        .version("v1")
                        .description(DESCRICAO_GERAL))
                .tags(List.of(
                        tag("Moradores", "Cadastro de moradores (RF01). O cadastro é público; o restante exige ator."),
                        tag("Endereços", "Endereços de um morador (RF02). Cada endereço pertence a um único morador."),
                        tag("Materiais", "Materiais recicláveis (RF03). Consulta pública; cadastro só do administrador."),
                        tag("Pontos de coleta", "Pontos de coleta (RF04) e sua pesquisa pública (RF06)."),
                        tag("Materiais aceitos", "Quais materiais cada ponto de coleta aceita (RF05)."),
                        tag("Operadores", "Responsáveis pelo recolhimento (RF07). Cada operador pertence a um ponto."),
                        tag("Solicitações", "Pedidos de recolhimento: criar, corrigir e consultar (RF08 e RF09)."),
                        tag("Situação da solicitação",
                                "Ações que mudam a situação da solicitação (RF10 a RF14): agendar, recusar, cancelar, "
                                        + "iniciar, concluir, reagendar e registrar falha."),
                        tag("Resumo gerencial", "Indicadores para o administrador (RF15).")))
                .components(new Components()
                        .addSecuritySchemes(PERFIL, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Perfil")
                                .description(DESCRICAO_PERFIL))
                        .addSecuritySchemes(USUARIO_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Usuario-Id")
                                .description(DESCRICAO_USUARIO_ID))
                        .schemas(ModelConverters.getInstance().readAll(RespostaErro.class)));
    }

    /**
     * Aplica a toda operação o que depende da assinatura do método, espelhando o {@link AtorArgumentResolver}:
     * parâmetro {@code Ator} exige os cabeçalhos; {@code Optional<Ator>} aceita a ausência deles (consulta pública);
     * sem nenhum dos dois, a operação não pede ator. Também declara o cabeçalho de idioma e troca o 200 padrão do
     * springdoc pelo 201 ou 204 declarados no controller.
     */
    @Bean
    public OperationCustomizer documentacaoPorOperacao() {
        return (operation, metodo) -> {
            operation.setSecurity(seguranca(metodo.getMethodParameters()));
            operation.addParametersItem(cabecalhoDeIdioma());
            if (operation.getResponses() != null) {
                if (operation.getResponses().containsKey("201") || operation.getResponses().containsKey("204")) {
                    operation.getResponses().remove("200");
                }
                operation.getResponses().forEach((codigo, resposta) -> {
                    ApiResponse padrao = errosPadrao.get(codigo);
                    if (padrao != null) {
                        // O springdoc põe o tipo de retorno do método em toda resposta sem corpo declarado.
                        resposta.setContent(padrao.getContent());
                    } else if (resposta.getContent() != null && resposta.getContent().containsKey("*/*")) {
                        Content json = new Content();
                        json.addMediaType(JSON, resposta.getContent().get("*/*"));
                        resposta.setContent(json);
                    }
                });
            }
            return operation;
        };
    }

    private List<SecurityRequirement> seguranca(MethodParameter[] parametros) {
        SecurityRequirement atorIdentificado = new SecurityRequirement().addList(PERFIL).addList(USUARIO_ID);
        for (MethodParameter parametro : parametros) {
            if (!Ator.class.equals(parametro.nestedIfOptional().getNestedParameterType())) {
                continue;
            }
            return Optional.class.equals(parametro.getParameterType())
                    ? List.of(new SecurityRequirement(), atorIdentificado)
                    : List.of(atorIdentificado);
        }
        return List.of();
    }

    private HeaderParameter cabecalhoDeIdioma() {
        StringSchema esquema = new StringSchema();
        esquema.setEnum(List.of("pt-BR", "en"));
        esquema.setDefault("pt-BR");
        HeaderParameter cabecalho = new HeaderParameter();
        cabecalho.setName("Accept-Language");
        cabecalho.setRequired(false);
        cabecalho.setSchema(esquema);
        cabecalho.setDescription("Idioma das mensagens de erro: `pt-BR` (padrão) ou `en`. Só muda o texto de "
                + "`mensagem`; o status e o `codigo` são os mesmos.");
        return cabecalho;
    }

    private Map<String, ApiResponse> respostasDeErro() {
        Map<String, ApiResponse> respostas = new LinkedHashMap<>();
        respostas.put("400", erro("Dados ou parâmetros inválidos. `VALIDACAO` lista em `erros` cada campo "
                + "rejeitado; `VALOR_INVALIDO` indica um valor de enum desconhecido.",
                exemplo("Campo inválido", "VALIDACAO", "Dados inválidos.",
                        List.of(Map.of("campo", "email", "mensagem", "deve ser um endereço de e-mail válido"))),
                exemplo("Valor de enum desconhecido", "VALOR_INVALIDO",
                        "Valor inválido. Valores aceitos: ATIVO, INATIVO.", List.of())));
        respostas.put("403", erro("O ator informado não tem permissão para a operação, ou os cabeçalhos "
                + "`X-Perfil` e `X-Usuario-Id` estão ausentes ou inválidos.",
                exemplo("Cabeçalho ausente", "ACESSO_NEGADO", "Informe o cabeçalho X-Perfil.", List.of())));
        respostas.put("404", erro("O recurso não existe ou está fora do contexto do ator.",
                exemplo("Recurso inexistente", "NAO_ENCONTRADO", "Morador não encontrado.", List.of())));
        respostas.put("409", erro("Conflito com uma regra de negócio (`CONFLITO_REGRA`) ou versão da solicitação "
                + "desatualizada (`CONFLITO_VERSAO`).",
                exemplo("Regra de negócio", "CONFLITO_REGRA",
                        "Já existe um morador cadastrado com este e-mail.", List.of()),
                exemplo("Versão desatualizada", "CONFLITO_VERSAO",
                        "O registro foi alterado por outra operação. Recarregue e tente novamente.", List.of())));
        respostas.put("500", erro("Erro inesperado. A mensagem é genérica e não expõe detalhes internos.",
                exemplo("Erro interno", "ERRO_INTERNO", "Erro interno. Tente novamente mais tarde.", List.of())));
        return respostas;
    }

    @SafeVarargs
    private ApiResponse erro(String descricao, Map.Entry<String, Example>... exemplos) {
        MediaType tipo = new MediaType().schema(new Schema<>().$ref("#/components/schemas/RespostaErro"));
        for (Map.Entry<String, Example> exemplo : exemplos) {
            tipo.addExamples(exemplo.getKey(), exemplo.getValue());
        }
        return new ApiResponse().description(descricao).content(new Content().addMediaType(JSON, tipo));
    }

    private Map.Entry<String, Example> exemplo(String titulo, String codigo, String mensagem, List<?> erros) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        corpo.put("erros", erros);
        return Map.entry(codigo, new Example().summary(titulo).value(corpo));
    }

    private static Tag tag(String nome, String descricao) {
        return new Tag().name(nome).description(descricao);
    }
}
