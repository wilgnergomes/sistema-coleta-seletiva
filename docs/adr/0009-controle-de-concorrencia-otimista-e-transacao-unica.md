# 9. Controle de concorrência otimista e transação única nas solicitações

**Data:** 2026-10-12

**Status:** Proposta

## Contexto

A RN06 exige que a criação com itens e as mudanças de situação com histórico ocorram em uma única transação e que, se duas atualizações concorrentes usarem a mesma versão, a segunda seja rejeitada com conflito, evitando perda silenciosa de dados. O RNF03 define 409 para conflitos de regra ou de versão, e o RNF08 exige teste automatizado de concorrência e um teste de integração com rollback.

Foram consideradas as seguintes alternativas: bloqueio pessimista da linha (`SELECT ... FOR UPDATE`), que impede a concorrência mas mantém linhas travadas durante a transação e não detecta que o cliente trabalhou com dados antigos entre duas requisições; controle por timestamp de atualização, que tem resolução limitada e depende do relógio; não controlar, o que contraria a RN06; e bloqueio otimista com coluna de versão numérica. Escolheu-se o bloqueio otimista, porque o volume é baixo, os conflitos são raros e ele detecta também a leitura desatualizada do cliente.

## Decisão

1. `sol_solicitacao` tem a coluna `sol_nr_versao` (inteiro, `NOT NULL`, padrão 0, não negativa), mapeada com `@Version` do JPA.
2. Os endpoints que alteram a situação recebem a versão lida pelo cliente. Se ela for diferente da versão atual, a operação é rejeitada. Se duas transações disputarem a mesma linha, o JPA lança a exceção de bloqueio otimista. Os dois casos resultam em resposta 409 com mensagem clara, tratada no handler global.
3. Cada operação de criação e de mudança de situação é um único método de serviço transacional: grava a solicitação, os itens (quando houver) e o registro de histórico, ou nada. Qualquer falha provoca rollback.

## Consequências

- **Positivas:** nenhuma perda silenciosa de atualização; nenhuma linha fica travada durante a transação; o comportamento atende ao RNF03 e à RN06; a coluna é simples de testar.
- **Negativas:** o cliente precisa guardar e reenviar a versão; em caso de conflito, o usuário refaz a operação com dados atualizados.
- **Cuidados:** o contrato da API e a documentação OpenAPI devem expor a versão nas respostas e exigi-la nas ações. A coluna usa tipo numérico, e não texto, porque o `@Version` precisa de um valor que o Hibernate incremente. Os testes de concorrência e de rollback (RNF08) precisam existir antes de considerar a RN06 atendida.
