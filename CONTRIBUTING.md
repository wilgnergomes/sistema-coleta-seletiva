# Guia de contribuição

Fluxo de trabalho da equipe no repositório. Vale para qualquer alteração, de código ou de documentação.

## Regra principal

A `main` é protegida: **ninguém envia alterações direto para ela**. Todo trabalho acontece em uma branch própria e entra na `main` por **Pull Request**.

## Fluxo de trabalho

Todos os comandos são executados em um terminal aberto na pasta do projeto.

### 1. Atualizar a `main`

```powershell
git switch main
git pull
```

### 2. Criar a branch e entrar nela

```powershell
git switch -c tipo/nome-curto
git branch --show-current
```

O segundo comando confirma em qual branch você está.

### 3. Trabalhar e conferir o que mudou

```powershell
git status
```

Leia a lista antes de continuar: só devem aparecer arquivos que você mexeu de propósito. Nunca deve aparecer `.env`, `target/` ou `.idea/`.

### 4. Commit

```powershell
git add .
git commit -m "tipo: descrição curta"
```

### 5. Enviar a branch

```powershell
git push -u origin tipo/nome-curto
```

O `-u` só é necessário no primeiro envio de cada branch.

### 6. Abrir o Pull Request

No GitHub, clique em **Compare & pull request** (ou vá em *Pull requests → New pull request*, com **base: `main`** e **compare: sua branch**). Preencha o template:

- o código do requisito (ex.: `RF08`);
- `Closes #N`, com o número da issue, para ela fechar sozinha no merge;
- o checklist da Definição de Pronto.

### 7. Merge

Com o PR aprovado (se a regra exigir revisão), clique em **Merge pull request** e depois em **Confirm merge**. Em seguida, clique em **Delete branch**.

### 8. Limpar e voltar à `main`

```powershell
git switch main
git pull
git branch -d tipo/nome-curto
```

## Nome de branch e de commit

O prefixo `tipo` é o mesmo nos dois:

| Tipo | Quando usar |
|---|---|
| `feat` | funcionalidade nova |
| `fix` | correção de erro |
| `docs` | documentação, diagramas, ADRs, README |
| `infra` | Docker, banco, configuração, CI |
| `test` | testes |
| `refactor` | reorganização de código sem mudar comportamento |
| `chore` | tarefas de manutenção |

Exemplos de branch: `feat/rf01-moradores`, `docs/er`, `infra/banco`.
Exemplos de commit: `feat: cadastra morador com validação de e-mail`, `docs: adiciona diagrama ER`.

Uma branch por assunto: não misture duas issues na mesma branch.

## Definição de Pronto

O checklist está no template de Pull Request (`.github/pull_request_template.md`). Uma issue só é considerada concluída quando o critério de aceite foi cumprido **e** todos os itens do checklist estão marcados.

## O que não fazer

- Não envie alterações direto para a `main`.
- Não versione credenciais, senhas ou o arquivo `.env`.
- Não rode `git add .` sem antes ler o `git status`.
- Não reescreva histórico de branches que já foram compartilhadas.

## Problemas comuns

| Situação | O que fazer |
|---|---|
| O `push` foi recusado na `main` | Esperado. Crie uma branch (passo 2) e abra um Pull Request. |
| `git status` mostra arquivos que você não queria | Não faça `git add .`. Adicione só os que importam: `git add caminho/do/arquivo`. |
| Não sabe em qual branch está | `git branch --show-current` |
| A branch ficou desatualizada em relação à `main` | `git switch main`, `git pull`, `git switch sua-branch`, `git merge main` |
| Dúvida sobre qualquer comando | Pergunte à equipe antes de forçar algo. |
