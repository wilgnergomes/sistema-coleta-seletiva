
---
name: desenvolvimento-issue
description: Gerencia o desenvolvimento de software por Issues do GitHub, uma tarefa por vez, respeitando documentação, planejamento, aprendizagem e controle de alterações.
disable-model-invocation: true
---

# Desenvolvimento por Issue

## Objetivo

Desenvolver o projeto utilizando as Issues do GitHub
como unidade de trabalho.

Trabalhe somente em uma Issue por ciclo.

## 1. Seleção da Issue

Consulte as Issues abertas e considere:

- Prioridade.
- Dependências.
- Bloqueios.
- Critérios de aceitação.
- Relação com os requisitos da atividade.
- Planejamento do projeto.

Não selecione tarefas arbitrariamente.

Apresente a Issue escolhida e justifique a seleção.

## 2. Investigação

Antes da implementação:

- Leia a descrição completa da Issue.
- Consulte os comentários relevantes.
- Identifique os arquivos envolvidos.
- Consulte os ADRs aplicáveis.
- Consulte o OpenAPI quando necessário.
- Verifique a matriz de rastreabilidade.
- Analise os padrões existentes no código.

## 3. Planejamento

Apresente:

- Objetivo.
- Requisitos relacionados.
- Conceitos técnicos envolvidos.
- Arquivos que serão modificados.
- Etapas da implementação.
- Testes necessários.
- Critérios de aceitação.

O plano deve ser compreensível para o estudante.

Aguarde a aprovação antes de iniciar alterações relevantes.

## 4. Implementação

- Utilize a Skill tutoria durante o desenvolvimento.
- Trabalhe em pequenas etapas.
- Preserve os padrões existentes.
- Faça alterações somente relacionadas à Issue.
- Evite funcionalidades especulativas.
- Não faça refatorações fora do escopo.

Não crie Worktrees nem altere a branch atual
sem autorização.

## 5. Controle do Git

Preserve alterações preexistentes.

Não execute automaticamente:

- git checkout ou git switch.
- git stash.
- git reset.
- git clean.
- git commit.
- git push.
- merge ou rebase.

Não feche Issues nem Pull Requests sem autorização.

## 6. Verificação

Antes de considerar a Issue concluída:

- Execute os testes pertinentes, após autorização.
- Verifique os critérios de aceitação.
- Revise o diff.
- Consulte a matriz de rastreabilidade.
- Identifique pendências.
- Utilize a Skill /revisao para verificar o trabalho antes de considerá-lo concluído.

Não declare sucesso sem evidências.

## 7. Continuidade

Ao terminar ou interromper a tarefa, registre:

- Issue trabalhada.
- Implementações realizadas.
- Arquivos modificados.
- Testes e resultados.
- Decisões tomadas.
- Pendências.
- Próximos passos.

Não avance automaticamente para outra Issue.
