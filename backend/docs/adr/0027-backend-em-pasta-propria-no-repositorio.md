# 27. Backend em pasta própria no repositório

**Data:** 2026-11-12

**Status:** Aceita (decisão aprovada pelo estudante em 2026-11-13)

## Contexto

Até aqui o repositório continha um único projeto: o `pom.xml`, o Maven Wrapper, o código em `src/`, o `compose.yaml`, o `.env` e a documentação em `docs/` ficavam todos na raiz, junto com arquivos que valem para o repositório inteiro (`.github/`, `.gitignore`, `.gitattributes`, `CLAUDE.md`, `.claude/`, `CONTRIBUTING.md`). O ADR 0015 descreve essa organização física ao situar o `compose.yaml` e o `.env` "na raiz" e ao executar a aplicação "a partir da raiz do projeto".

O roadmap prevê uma interface web na segunda unidade. Com tudo na raiz, não há lugar natural para uma segunda aplicação, e fica difícil distinguir o que é do backend e o que é configuração do repositório. A reorganização foi feita a pedido do estudante na branch `refactor/backend-em-pasta-propria`, sem mudança funcional. Este ADR registra a decisão.

Dois fatos técnicos pesaram:

- O Maven, o Spring Boot e os testes resolvem caminhos relativos a partir do diretório de execução. O `application.properties` importa o `.env` com `spring.config.import=optional:file:.env[.properties]`, e `OpenApiDocumentacaoTest` e `DatasetDesempenhoIntegracaoTest` leem `docs/api/openapi.yaml` e `docs/desempenho/*.sql` por caminho relativo. O Maven executa os testes na pasta do `pom.xml`.
- Sem um nome declarado, o Docker Compose dá ao projeto o nome da pasta do `compose.yaml`, e é desse nome que derivam os volumes. O `compose.yaml` já declara `name: coleta-seletiva`, então o volume existente se chama `coleta-seletiva_coleta_pgdata`.

Alternativas consideradas:

1. **Manter tudo na raiz.** Não exige mudança, mas mistura arquivos do backend com os do repositório e não acomoda uma segunda aplicação.
2. **Mover só o código (`src/`, `pom.xml`, wrapper) e deixar `docs/`, `compose.yaml` e `.env` na raiz.** A raiz continua carregada. Os testes que leem `docs/` quebrariam ou precisariam de caminhos como `../docs`, e o `.env` deixaria de ser encontrado pela aplicação executada em `backend/`.
3. **Projeto Maven multimódulo, com um `pom.xml` agregador na raiz.** Permite rodar o Maven da raiz, mas cria um segundo projeto Maven e uma estrutura de build que nenhuma necessidade atual justifica. A interface web não seria um módulo Maven.
4. **Pasta `backend/` com tudo o que é exclusivo do backend; na raiz, só o que é global (escolhida).** Separa as responsabilidades, mantém os caminhos relativos válidos e não altera o build.

## Decisão

1. Tudo o que é exclusivo do backend fica em `backend/`: o código e os testes (`src/`), o `pom.xml`, o Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`), as configurações do Spring Boot e os scripts Flyway (`src/main/resources`, incluindo `db/migration` e `db/seed`), o `compose.yaml`, o `.env` (não versionado), o `.env.example`, a documentação técnica (`docs/`, com ADRs, contrato OpenAPI, diagramas, requisitos, auditorias e desempenho) e o `README.md` técnico.
2. Na raiz ficam apenas os itens de responsabilidade global: `.git/`, `.github/`, `.gitignore`, `.gitattributes`, `CLAUDE.md`, `.claude/`, `CONTRIBUTING.md` e um `README.md` geral, que apresenta o repositório e aponta para `backend/README.md`.
3. Os comandos do Maven, a execução da aplicação e os comandos do Docker Compose são feitos a partir de `backend/`. É nessa pasta que o Maven encontra o `pom.xml` e o `.mvn/`, que a aplicação encontra o `.env` e que os testes resolvem `docs/...`. Os caminhos citados na documentação do backend são relativos a `backend/`.
4. O Docker Compose mantém o nome de projeto fixo `coleta-seletiva`, declarado no `compose.yaml`. Por isso o container `coleta-seletiva-db` e o volume `coleta-seletiva_coleta_pgdata` continuam os mesmos depois da mudança de pasta, e os dados existentes são preservados.
5. Não há mudança funcional: o código, os packages, o contrato da API, as migrações, os seeds, as dependências e o comportamento da aplicação não são alterados. O projeto continua sendo um único projeto Maven (ADR 0001), sem multimódulo.
6. Os padrões do `.gitignore` sem barra (como `target/` e `.env`) já valem em qualquer pasta. Os padrões ancorados no caminho recebem o prefixo `backend/`.
7. Esta decisão atualiza a organização física descrita no ADR 0015 (localização do `compose.yaml` e do `.env` e diretório de execução, decisões 1, 3 e 5), sem invalidar suas decisões técnicas sobre Docker e PostgreSQL: imagem, container, volume, variáveis, porta e leitura do `.env` continuam valendo.

## Consequências

- **Positivas:** a raiz fica limpa e mostra o que é do repositório e o que é do backend; uma futura interface web pode ganhar sua própria pasta sem misturar arquivos; o histórico dos arquivos movidos é preservado, porque o Git reconhece as movimentações; nenhum caminho relativo do build, da aplicação ou dos testes precisou mudar.
- **Negativas:** todos os comandos exigem estar em `backend/`, e executar o Maven ou a aplicação da raiz falha (sem `pom.xml`, ou com placeholder `DB_PASSWORD` não resolvido); quem já tinha o projeto aberto na IDE pela raiz precisa reabri-lo em `backend/` ou ajustar o diretório de trabalho; o `.env` local precisa ser movido à mão para `backend/`.
- **Cuidados:** não remover o `name: coleta-seletiva` do `compose.yaml`. Sem ele, o projeto passaria a se chamar `backend` e o Compose criaria um volume novo e vazio, e o banco pareceria ter perdido os dados. Workflows de CI que vierem a ser criados devem usar `working-directory: backend`. Documentos históricos, como auditorias e o ADR 0015, citam a organização anterior e devem ser lidos como registro da época.
- **Relações:** atualiza a organização física descrita nas decisões 1, 3 e 5 do ADR 0015, que permanece aceito e como registro histórico. É compatível com o ADR 0001 (Maven) e com o ADR 0022 (seed no perfil `demo`), cujos scripts continuam no classpath.
