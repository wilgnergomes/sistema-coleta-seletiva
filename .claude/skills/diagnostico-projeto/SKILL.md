
---
name: diagnostico-projeto
description: Investiga o estado de um projeto de software sem modificar arquivos, código, branches ou informações do GitHub.
disable-model-invocation: true
---

# Diagnóstico do Projeto

## Objetivo

Realizar uma análise do projeto para identificar seu estado
atual antes de iniciar qualquer implementação.

Esta Skill é exclusivamente de investigação.

## 1. Estado do Git

Investigue:

- Branch atual.
- Alterações pendentes.
- Arquivos modificados.
- Arquivos não rastreados.
- Últimos commits.
- Repositório remoto.

Utilize comandos somente de leitura, como git status,
git diff, git log e git remote.

Não descarte nem altere nenhuma modificação existente.

## 2. Código

Identifique:

- Linguagens e tecnologias.
- Arquitetura e organização.
- Principais componentes.
- Funcionalidades existentes.
- Testes disponíveis.

Investigue especialmente os componentes relacionados
à funcionalidade solicitada.

Não considere algo concluído somente pela existência
de um arquivo.

## 3. Documentação

Localize e consulte os documentos pertinentes:

- Enunciado original da atividade.
- ADRs.
- OpenAPI.
- Matriz de rastreabilidade.
- Documentos da pasta docs.
- Registros de continuidade.

Identifique divergências entre as documentações, documentação e código.

## 4. GitHub

Quando o GitHub CLI estiver disponível e autenticado,
consulte apenas informações de leitura:

- Issues abertas.
- Descrições e comentários.
- Dependências.
- Pull Requests existentes.
- Estado dos PRs relevantes.

Não crie, edite ou feche Issues ou Pull Requests.

## 5. Restrições obrigatórias

Durante o diagnóstico:

- Não criar arquivos.
- Não editar documentos.
- Não modificar código.
- Não executar instalações.
- Não alterar branches.
- Não fazer commits ou push.
- Não fazer merge.
- Não utilizar git reset, clean ou stash.
- Não executar testes ou comandos que alterem
  o estado do projeto.

Não exiba conteúdos de senhas, tokens ou arquivos
de credenciais.

## 6. Relatório

Apresente:

1. Estado atual do Git.
2. Alterações pendentes.
3. Resumo da implementação.
4. Funcionalidades incompletas.
5. Documentação encontrada.
6. Situação das Issues.
7. Pull Requests relacionados.
8. Problemas e inconsistências.
9. Próximos passos recomendados.

Diferencie fatos confirmados de hipóteses.

Não implemente nenhuma solução durante esta etapa.
