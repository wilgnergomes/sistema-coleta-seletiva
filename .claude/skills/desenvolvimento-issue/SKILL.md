
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

Não desenvolva diretamente no `main` e não crie Worktrees
sem autorização.

## 5. Fluxo de Git e GitHub

Siga o fluxo permanente registrado no `CLAUDE.md`:

1. Com a Issue escolhida, atualize o `main` local (`git switch main`
   e `git pull`) e crie a branch da Issue a partir dele
   (`tipo/nome-curto`, por exemplo `feat/cadastro-morador`).
2. Desenvolva, teste e faça commits organizados (`tipo: descrição`),
   um assunto por commit.
3. Faça push da branch e abra o PR para `main` com o template do
   repositório e `Closes #N`.
4. Revise o PR (critérios de aceite, testes, diff) e apresente um
   resumo ao estudante.
5. **Aguarde a autorização explícita do estudante para o merge.**
6. Depois do merge, confirme que a Issue foi encerrada, atualize o
   `main`, exclua a branch (local e remota) após confirmar a
   integração e que não há trabalho não preservado, e deixe o `main`
   limpo antes da próxima Issue.

Preserve alterações preexistentes. Você pode executar sem pedir
permissão: `git switch`, `git pull`, criação de branch, `git add`,
`git commit` e `git push` da branch da Issue, e abertura do PR.

Peça autorização antes de:

- fazer merge de PR;
- fechar Issue ou PR fora do fluxo (o normal é o `Closes #N`);
- `git stash`, `git reset`, `git clean`, `rebase`, push forçado e
  qualquer outra operação que possa perder trabalho;
- formalizar uma nova decisão arquitetural (ADR).

Se houver alterações locais inesperadas, conflitos ou risco de perda
de trabalho, interrompa e informe o estudante antes de prosseguir.

Nunca inclua em commits `.claude/settings.local.json`, credenciais
ou arquivos sensíveis.

## 6. Verificação

Antes de considerar a Issue concluída:

- Execute os testes essenciais (requisitos, regras críticas e
  funcionamento principal) e corrija os problemas reais.
- Verifique os critérios de aceitação.
- Revise o diff.
- Consulte a matriz de rastreabilidade.
- Identifique pendências. As que decidirmos não corrigir agora são registradas em `backend/docs/melhorias-pendentes.md`,
  sem duplicar e sem corrigir automaticamente (regra permanente do `CLAUDE.md`), de preferência no PR da Issue.
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
