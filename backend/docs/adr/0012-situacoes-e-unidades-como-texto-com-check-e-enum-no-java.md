# 12. Situações e unidades como texto com CHECK no banco e enum no Java

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

O sistema tem domínios fechados de valores: situação de cadastro (morador, material, ponto e operador), situação da solicitação (RN06 e a tabela de transições) e unidade de medida (RN02). Era preciso decidir como representá-los no banco e no código.

Foram consideradas as seguintes alternativas: tipo `ENUM` nativo do PostgreSQL, em que valores só podem ser adicionados, e não removidos, e que exige mapeamento específico no Hibernate; tabelas de domínio, adequadas quando os valores mudam em tempo de execução, o que não ocorre aqui porque a especificação os fixa; e coluna de texto com `CHECK`, representada por enum no Java. Escolheu-se a terceira por ser a mais simples de mapear e de alterar por migração.

## Decisão

1. Cada conceito tem uma coluna `varchar` com `CHECK` de domínio e um enum próprio no Java:
   - situação de cadastro: `ATIVO`, `INATIVO` (`varchar(10)`);
   - situação da solicitação: `SOLICITADA`, `AGENDADA`, `EM_RECOLHIMENTO`, `CONCLUIDA`, `RECUSADA`, `CANCELADA` (`varchar(20)`, também no histórico);
   - unidade: `KG`, `L`, `UN` (`varchar(5)`, no material e na cópia do item).
2. Os enums usam `@Enumerated(EnumType.STRING)`, com nomes idênticos aos valores do `CHECK`, sem acento e em maiúsculas.
3. As transições permitidas da RN06 ficam no enum da solicitação e são aplicadas pelo serviço. O `CHECK` limita apenas os valores, e não as transições.
4. Valor desconhecido na requisição resulta em 400 com os valores aceitos (RNF03). Um valor fora da especificação, como `PENDENTE`, só entra com a respectiva transição definida.

## Consequências

- **Positivas:** mapeamento trivial; alteração de domínio por migração simples; o banco recusa valores inválidos mesmo se o código falhar; a documentação OpenAPI lista os valores do enum.
- **Negativas:** os valores existem em dois lugares (`CHECK` e enum) e podem divergir; novos valores maiores que a coluna exigem também alterar o tamanho.
- **Cuidados:** adicionar um valor exige uma migração nova (nunca editar uma já aplicada), o ajuste do enum e das notas dos diagramas. Um teste de integração que grava cada valor de cada enum no banco detecta divergência. Nunca usar `EnumType.ORDINAL`.
