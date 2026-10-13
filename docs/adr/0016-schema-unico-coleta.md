# 16. Schema único `coleta` para as tabelas do sistema

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

A migração `V1__criar_esquema_incial.sql` cria as nove tabelas do sistema sem indicar em qual schema do PostgreSQL elas ficam, o que as colocaria em `public`. O ADR 0004 dividiu o diagrama ER em duas visões (cadastros e fluxo), o que levantou a dúvida se essa divisão deveria virar dois schemas. Como a V1 ainda não havia sido aplicada em nenhum banco, ela podia ser ajustada sem criar uma migração V2 e sem refazer dados.

Foram consideradas as seguintes alternativas: manter tudo em `public`, o que mistura as tabelas do projeto com qualquer outro objeto do banco; dividir em dois schemas acompanhando as visões do DER, o que exige nomes qualificados nas entidades, no `search_path`, nas consultas nativas e nas chaves estrangeiras entre schemas, para apenas nove tabelas sem permissões distintas por área; e usar um único schema próprio. Escolheu-se a terceira, porque as visões do DER são uma divisão de documentação, e não de propriedade ou de acesso, e os prefixos das tabelas (`mor_`, `sol_`, `iso_`) já as agrupam por assunto.

## Decisão

1. As nove tabelas do sistema ficam em um único schema chamado `coleta`.
2. O schema é configurado na aplicação, e o SQL da V1 permanece sem prefixo nos nomes das tabelas:
   - `spring.flyway.schemas=coleta`: o Flyway cria o schema se necessário, executa as migrações com ele como padrão e guarda nele a tabela `flyway_schema_history`;
   - `spring.jpa.properties.hibernate.default_schema=coleta`: as entidades apontam para o schema sem `@Table(schema = ...)`;
   - `currentSchema=coleta` na URL de conexão: consultas nativas e `JdbcTemplate` também enxergam o schema.
3. As duas visões do DER continuam sendo uma divisão de documentação (ADR 0004) e não correspondem a schemas. Tabelas novas entram no mesmo schema.

## Consequências

- **Positivas:** as tabelas do projeto ficam separadas de `public`; as entidades e as consultas não precisam de nomes qualificados; a configuração é pequena e a V1 permanece legível.
- **Negativas:** a configuração está em três lugares (Flyway, Hibernate e URL) e precisa permanecer coerente; scripts SQL executados à mão, fora da aplicação, precisam de `SET search_path TO coleta` ou de nomes qualificados; se no futuro houver motivo para dois schemas, será necessária uma migração com `ALTER TABLE ... SET SCHEMA`.
- **Cuidados:** conferir após a primeira execução que as tabelas estão em `coleta` e que `public` não recebeu nenhuma (`\dn`, `\dt coleta.*` e `\dt public.*`). Os testes de integração com banco devem usar a mesma configuração de schema. Se o `.dbml` passar a exibir o schema, usar o formato `coleta.nome_da_tabela` nas duas visões. Quem criar um ambiente novo não precisa criar o schema à mão, porque o Flyway o cria.
