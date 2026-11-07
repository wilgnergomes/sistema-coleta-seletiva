
---
name: revisao
description: Verifica implementações, testes, compilação, requisitos e alterações antes de considerar uma tarefa concluída ou preparar um Pull Request.
disable-model-invocation: true
---

# Revisão e Verificação de Implementações

## Objetivo

Garantir que nenhuma funcionalidade seja considerada
concluída sem evidências verificáveis.

Princípio fundamental:
Nunca afirmar que algo funciona apenas porque
o código parece correto.

## 1. Identificar o que precisa ser verificado

Antes de declarar uma tarefa concluída:

- Identifique os critérios de aceitação.
- Consulte os requisitos relacionados.
- Verifique os ADRs aplicáveis.
- Consulte a OpenAPI quando necessário.
- Identifique os testes e verificações apropriados.

## 2. Verificar com evidências

Para cada afirmação de sucesso:

1. Identifique qual verificação comprovaria a afirmação.
2. Determine o comando ou procedimento adequado.
3. Execute-o somente quando autorizado.
4. Examine o resultado completo e os erros apresentados.
5. Confirme se o resultado realmente sustenta a afirmação.
6. Relate as evidências e eventuais limitações.

Não declare que um teste passou sem verificar
sua execução e seu resultado.

Não reutilize resultados antigos como comprovação
do estado atual.

## 3. Verificações técnicas

Quando aplicável, verifique:

### Testes
- Testes unitários.
- Testes de integração.
- Cenários de erro.
- Testes de regressão.

### Compilação
- Resultado da compilação.
- Erros e avisos relevantes.
- Dependências necessárias.

### Código
- Alterações realizadas.
- Possíveis erros de lógica.
- Compatibilidade com a arquitetura.
- Tratamento de exceções.
- Alterações fora do escopo.

### Requisitos
- Critérios de aceitação da Issue.
- Requisitos funcionais aplicáveis.
- Contratos definidos na OpenAPI.
- Decisões registradas nos ADRs.
- Matriz de rastreabilidade.

## 4. Regressões

Ao corrigir um problema:

- Identifique o comportamento incorreto original.
- Verifique se ele deixou de ocorrer.
- Investigue se a correção afetou outros comportamentos.
- Utilize testes de regressão quando apropriado.

Quando houver um teste criado especificamente
para detectar o defeito, verifique se ele realmente
detectaria o comportamento incorreto.

## 5. Revisão das alterações

Analise o diff do Git.

Verifique:
- Quais arquivos foram alterados.
- Se as mudanças pertencem à Issue.
- Se existem alterações acidentais.
- Se alguma modificação anterior foi sobrescrita.
- Se há arquivos sensíveis ou credenciais expostos.

Não modifique arquivos apenas por identificar
possíveis melhorias. Primeiro apresente os problemas.

## 6. Antes de um Pull Request

Verifique:

1. Critérios de aceitação atendidos.
2. Testes pertinentes executados.
3. Resultado da compilação, quando aplicável.
4. Alterações revisadas.
5. Documentação pertinente consistente.
6. Pendências e limitações identificadas.

Não faça commit, push, merge ou abra Pull Requests
sem autorização do estudante.

## 7. Relatório final

Apresente:

- O que foi verificado.
- Quais testes foram executados.
- Quais testes passaram ou falharam.
- Quais requisitos foram atendidos.
- Problemas encontrados.
- O que ainda precisa ser validado.
- Estado real da implementação.

Classifique cada item como:

- Verificado.
- Falhou.
- Não verificado.
- Não aplicável.

Nunca confunda "não verificado" com "aprovado".

## 8. Aprendizado

Ao encontrar problemas:

- Explique o motivo técnico.
- Mostre como identificar a causa.
- Apresente alternativas de correção.
- Relacione o problema aos conceitos de programação.

A revisão deve também contribuir para o aprendizado
do estudante.
