# 22. Dados de demonstração (seed) por callback `afterMigrate` no perfil `demo`

**Data:** 2026-11-07

**Status:** Aceita

## Contexto

O RF01 exige dados de demonstração reproduzíveis: moradores fictícios com e-mails distintos e já em minúsculas (matriz de rastreabilidade). O ADR 0019 também pede moradores e operadores conhecidos para servirem de atores nas demonstrações e nos testes. Os testes automatizados criam os próprios dados e não podem depender desse seed.

As migrações do Flyway pertencem ao esquema (ADR 0016). Um seed aplicado como migração entra no histórico do banco e traz problemas, verificados em experimento com o Flyway 12.4.0 e PostgreSQL 17:

- Um seed repetível (`R__*.sql`) aplicado em um perfil e depois não encontrado em outro faz a aplicação **não iniciar**: "Detected applied migration not resolved locally". Evitar isso exige `spring.flyway.ignore-migration-patterns=repeatable:missing,*:future` também fora do perfil de demonstração. A propriedade substitui o padrão `*:future` do Flyway, e por isso precisa repeti-lo. Além disso, um `R__` só roda de novo se o conteúdo mudar, então limpar a tabela não recarrega o seed.
- Um seed versionado com número alto (por exemplo `V999`) **bloqueia migrações futuras**: uma `V2` criada depois (issue #33) falha com "resolved migration not applied: 2" a menos que se habilite `outOfOrder`.

Alternativas consideradas:

1. **Seed repetível `R__` em pasta do perfil, com `repeatable:missing,*:future`.** Fica no histórico e é o caminho convencional, mas exige a propriedade extra em todos os perfis e não se recarrega após limpar os dados.
2. **Callback `afterMigrate`, em pasta carregada só no perfil `demo`.** Não entra no histórico, não exige propriedade extra e recarrega a cada início. Exige SQL idempotente.
3. **Seed versionado (`V999`).** Descartada: bloqueia migrações futuras.
4. **Código Java com `ApplicationRunner` no perfil `demo`.** Reaproveitaria as regras do service, mas coloca código de demonstração no artefato de produção, e a matriz fala em dados já normalizados, o que indica SQL.
5. **Script SQL manual no README.** Não acopla ao Flyway, mas é menos reprodutível e depende de execução manual.

## Decisão

1. Os dados de demonstração ficam em `src/main/resources/db/seed/`, como callbacks do Flyway com o nome `afterMigrate__NNN_<assunto>.sql`. O prefixo numérico tem sempre três dígitos (`010`, `020`...), porque o Flyway executa os callbacks do mesmo evento em ordem alfabética do texto: `10_x` roda antes de `2_x` (verificado).
2. A pasta `db/seed` só é carregada no perfil `demo`, por `spring.flyway.locations` em `application-demo.properties`. Sem o perfil, o seed não existe para o Flyway e não é executado.
3. Todo script de seed é idempotente: insere somente o que ainda não existe (`INSERT ... SELECT ... WHERE NOT EXISTS`, que, ao contrário de `ON CONFLICT DO NOTHING`, não consome valores da identidade a cada reexecução) e usa apenas dados fictícios já normalizados.
4. Um seed que dependa de outro (por exemplo, endereços dependem de moradores) recebe número maior no nome do arquivo.
5. Os testes automatizados não carregam o seed. Apenas `SeedDemoIntegracaoTest` ativa o perfil `demo`, para validar o próprio seed; um teste no perfil padrão confirma que o banco não recebe dados de seed.

## Consequências

- **Positivas:** o perfil padrão não muda e não precisa de configuração extra no Flyway; ligar e desligar o perfil `demo` não impede a aplicação de iniciar; o seed se recompõe a cada início, mesmo após limpar os dados; o seed não interfere nas migrações versionadas futuras (`V2`...).
- **Negativas:** o seed roda a cada início do perfil `demo`; ele não aparece no histórico do Flyway, então não há registro do que foi aplicado; as linhas existentes não são atualizadas se o seed mudar (o script só insere).
- **Cuidados:** o seed está dentro do artefato da aplicação, embora só seja carregado no perfil `demo`; nunca deve ser usado com um banco de produção. Os identificadores dos moradores seguem a ordem de inserção em um banco novo, e por isso demonstrações e testes devem localizar o morador pelo e-mail, e não por um id fixo. Se o seed passar a ter muitos arquivos, a ordem por prefixo numérico deve ser revisada.
