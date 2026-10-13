# 15. Banco de desenvolvimento com Docker Compose e configuração por `.env`

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

A aplicação precisa de um PostgreSQL para rodar, e o RNF07 exige que o projeto seja reproduzível: README com requisitos, configuração e comandos de execução, além de scripts ou migrações. O RNF06 proíbe versionar senhas, tokens ou credenciais de banco, que devem ser configurados por variáveis de ambiente. Além disso, a máquina de desenvolvimento já tem outro PostgreSQL em container usando a porta padrão 5432, e o banco deste projeto não deve se misturar com ele.

Para o banco, foram consideradas as seguintes alternativas: instalar o PostgreSQL diretamente na máquina, o que depende de cada ambiente e dificulta a reprodução; reaproveitar o container existente, que mistura dados de projetos diferentes; e criar um container próprio com Docker Compose, descrito em um arquivo versionado.

Para a configuração, foram consideradas: credenciais escritas no `application.properties`, o que viola o RNF06; variáveis de ambiente definidas à mão em cada terminal ou IDE, que são fáceis de esquecer e difíceis de documentar; um arquivo `.env` não versionado, lido pelo Docker Compose e pela aplicação, com um `.env.example` como modelo; a dependência `spring-boot-docker-compose`, que sobe o compose junto com a aplicação e dispensa a configuração do datasource, mas esconde o momento em que o banco sobe e para; e uma biblioteca de terceiros para ler `.env`, que acrescenta dependência sem necessidade, já que o próprio Spring Boot importa o arquivo.

## Decisão

1. O banco de desenvolvimento é um serviço `db` no `compose.yaml` da raiz do repositório, com imagem `postgres:17`, container `coleta-seletiva-db`, volume nomeado `coleta_pgdata` para os dados e `healthcheck` com `pg_isready`. O projeto do compose se chama `coleta-seletiva`.
2. A porta do computador é configurável por `DB_PORT` e mapeada para a porta 5432 do container. O valor sugerido no `.env.example` é 5433, para não colidir com um PostgreSQL local na 5432.
3. Os parâmetros são as variáveis `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `DB_PORT`, definidas em um arquivo `.env` na raiz, que não é versionado e consta no `.gitignore`. O `.env.example` é versionado, com valores fictícios, e documenta as variáveis. O compose recusa subir quando `DB_PASSWORD` não está definida.
4. O `application.properties` referencia as variáveis por placeholders (`${DB_PORT}`, `${DB_NAME}`, `${DB_USER}`, `${DB_PASSWORD}`), sem valor padrão para a senha, e importa o `.env` com `spring.config.import=optional:file:.env[.properties]`. Variáveis de ambiente do sistema operacional ou do CI prevalecem sobre o arquivo e dispensam o `.env`.
5. A aplicação é executada manualmente a partir da raiz do projeto, com o banco já no ar (`docker compose up -d`). A dependência `spring-boot-docker-compose` não é adotada por enquanto.
6. O banco nasce vazio. O esquema vem das migrações versionadas, aplicadas na inicialização da aplicação, e nenhum dado é criado pelo compose. As tabelas ficam no schema `coleta` (ADR 0016).
7. O banco de testes automatizados (RNF08) não faz parte desta decisão.

## Consequências

- **Positivas:** o ambiente sobe com dois comandos e é descrito em arquivo versionado (RNF07); nenhuma credencial entra no repositório (RNF06); o banco fica isolado do PostgreSQL já existente; o volume preserva os dados entre reinicializações; a mesma configuração funciona em CI por variáveis de ambiente, sem mudança de código.
- **Negativas:** exige Docker e Docker Compose v2 instalados e em execução; `.env` e `.env.example` podem divergir; as credenciais do container só valem na primeira criação do volume, então trocar a senha depois exige apagar o volume ou alterá-la dentro do banco; o `.env` é lido a partir do diretório de execução, e uma IDE com outro diretório de trabalho não o encontra (o `optional:` esconde a falha, que aparece como placeholder não resolvido); testes que carregam o contexto da aplicação precisam de um datasource disponível.
- **Cuidados:** nunca versionar o `.env`; se isso acontecer, trocar a senha, porque remover o arquivo não apaga o histórico do Git. Cada variável nova deve entrar no `.env.example` e na tabela de variáveis do README. O comando `docker compose down -v` apaga o volume e todos os dados do banco. A versão da imagem é fixada na versão maior; trocá-la exige recriar o volume ou migrar os dados. A decisão sobre o banco dos testes de integração, por exemplo com containers descartáveis, deve ser registrada em outro ADR.
- **Possibilidade futura:** adotar o `spring-boot-docker-compose` caso o passo manual de subir o banco passe a atrapalhar; a mudança afeta apenas as dependências e as properties de conexão.
