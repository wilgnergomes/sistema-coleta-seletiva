# Guia de contribuição

Fluxo de trabalho da equipe no repositório. Vale para qualquer alteração, de código ou de documentação.

## Regra principal

A `main` é protegida: **ninguém envia alterações direto para ela**. Todo trabalho acontece em uma branch própria e entra na `main` por **Pull Request**, com merge commit e só com autorização explícita do responsável pelo projeto.

As branches são **preservadas** como referência histórica para estudo: nenhuma é excluída, local ou remota, nem depois do merge. Rebase, squash, reset destrutivo e force push só com autorização explícita.

O frontend segue um fluxo próprio, de branches cumulativas, descrito em [Fluxo do frontend](#fluxo-do-frontend-branches-cumulativas).

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

Com o PR aprovado e o merge autorizado, escolha **Create a merge commit** (não use *Squash and merge* nem *Rebase and merge*), clique em **Merge pull request** e depois em **Confirm merge**. **Não clique em Delete branch**: a branch fica preservada.

### 8. Voltar à `main`

```powershell
git switch main
git pull
```

A branch continua existindo, local e no GitHub. Não rode `git branch -d` nem `git push --delete`.

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

As etapas do frontend usam o padrão `frontend/NN-nome-curto` (ex.: `frontend/01-estrutura`), com os mesmos tipos nas mensagens de commit.

## Fluxo do frontend (branches cumulativas)

O frontend é desenvolvido em etapas, cada uma em uma branch que **nasce do último commit validado da etapa anterior**, e não da `main`:

`main` → `frontend/01-estrutura` → `frontend/02-design-system` → `frontend/03-catalogo-publico` → próximas etapas.

- Só a primeira etapa nasce da `main`.
- Durante o desenvolvimento, as etapas **não são integradas à `main`** e não precisam de PR para ela.
- Cada branch é um snapshot histórico: não altere uma etapa antiga para incluir algo de uma etapa posterior. Uma correção entra na etapa atual.
- Se for preciso um PR intermediário para revisão, use a branch anterior como base e **não faça o merge** desse PR.
- A integração final do frontend à `main` será decidida depois, com autorização explícita.

Criar a próxima etapa a partir da anterior:

```powershell
git switch frontend/01-estrutura
git pull
git switch -c frontend/02-design-system
```

Revisar uma etapa comparando-a com a anterior:

```powershell
git log --oneline frontend/01-estrutura..frontend/02-design-system
git diff frontend/01-estrutura..frontend/02-design-system
```

## Definição de Pronto

O checklist está no template de Pull Request (`.github/pull_request_template.md`). Uma issue só é considerada concluída quando o critério de aceite foi cumprido **e** todos os itens do checklist estão marcados.

## O que não fazer

- Não envie alterações direto para a `main`.
- Não versione credenciais, senhas ou o arquivo `.env`.
- Não rode `git add .` sem antes ler o `git status`.
- Não reescreva histórico de branches que já foram compartilhadas.
- Não exclua branches, locais ou remotas, nem depois do merge.
- Não faça rebase, squash, reset destrutivo ou force push sem autorização explícita.
- Não faça merge sem autorização explícita.

## Problemas comuns

| Situação | O que fazer |
|---|---|
| O `push` foi recusado na `main` | Esperado. Crie uma branch (passo 2) e abra um Pull Request. |
| `git status` mostra arquivos que você não queria | Não faça `git add .`. Adicione só os que importam: `git add caminho/do/arquivo`. |
| Não sabe em qual branch está | `git branch --show-current` |
| A branch ficou desatualizada em relação à `main` | `git switch main`, `git pull`, `git switch sua-branch`, `git merge main`. Não faça isso em etapas já concluídas do frontend, que são snapshots históricos. |
| Dúvida sobre qualquer comando | Pergunte à equipe antes de forçar algo. |
