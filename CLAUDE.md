# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.


## Tutoria durante o desenvolvimento

O estudante quer aprender enquanto o projeto é construído.

- Ao planejar ou implementar uma Issue, siga a metodologia da Skill
  `tutoria` (`.claude/skills/tutoria/SKILL.md`), mesmo que a Skill
  não seja ativada automaticamente.
- Explique os conceitos antes de usá-los, relacione-os ao código e aos
  ADRs do projeto e divida o trabalho em pequenas etapas.
- Dê ao estudante partes adequadas ao seu nível para implementar, sem
  impedir o andamento do trabalho.

## Fluxo permanente de Git e GitHub

Cada Issue segue este fluxo (detalhes e comandos manuais em `CONTRIBUTING.md`):

1. Selecionar uma Issue e verificar seus requisitos e critérios de aceite.
2. Atualizar o `main` local a partir do remoto.
3. Criar uma branch específica para a Issue, partindo do `main` atualizado.
4. Desenvolver a funcionalidade respeitando a arquitetura e os contratos existentes.
5. Executar os testes necessários e corrigir problemas reais.
6. Fazer commits organizados e coerentes com as alterações.
7. Fazer push da branch.
8. Abrir PR para `main`, relacionando a Issue (`Closes #N`), com o template do repositório.
9. Revisar o PR: critérios de aceite, testes e diff.
10. **Aguardar a autorização explícita do estudante para o merge.**
11. Após a aprovação, fazer o merge e confirmar o encerramento da Issue, se todos os critérios foram cumpridos.
12. Atualizar o `main` local.
13. Excluir a branch antiga, local e remota, depois de verificar que a integração foi concluída e que não existem alterações não preservadas.
14. Deixar o `main` atualizado e limpo antes de começar a próxima Issue.

Regras:

- Não desenvolver funcionalidades diretamente no `main`.
- Não acumular branches de trabalho já integradas, salvo com justificativa técnica.
- As operações rotineiras deste fluxo (atualizar o `main`, criar branch, commit, push, abrir PR) podem ser conduzidas sem pedir permissão a cada passo.
- É obrigatório pedir aprovação antes de **merge**, antes de formalizar **novas decisões arquiteturais** (ADR) e antes de operações destrutivas fora do fluxo (`reset`, `clean`, `stash`, `rebase`, push forçado, apagar branch com trabalho não integrado).
- Se houver alterações locais inesperadas, conflitos ou risco de perda de trabalho, interromper a operação e informar o estudante antes de prosseguir.
- Não incluir configurações locais (`.claude/settings.local.json`), credenciais ou arquivos sensíveis nos commits.
- Internacionalização (ADR 0023, infraestrutura da Issue #35): mensagens ao cliente não ficam fixas no código. Formulários usam chaves (`message = "{morador.nome.obrigatorio}"`) e as exceções de negócio (`NegocioException`) carregam chave e argumentos; o texto fica em `messages.properties` (pt-BR) e `messages_en.properties`, com as mesmas chaves nos dois (um teste confere). Cada nova funcionalidade acrescenta suas chaves nos dois idiomas na própria branch, sem reabrir a Issue #35, salvo defeito ou requisito pendente da infraestrutura.

## Registro de pendências não corrigidas

Sempre que identificar um defeito, melhoria, dívida técnica, inconsistência ou limitação que decidirmos
**não corrigir imediatamente**, registre-o em `backend/docs/melhorias-pendentes.md`:

- Antes de registrar, verifique se já consta. Se constar, atualize a entrada quando necessário, sem duplicar.
- Se não constar, escreva uma entrada objetiva com: descrição, impacto, localização no código, prioridade
  sugerida e momento recomendado para a correção.
- Diferencie **defeito confirmado** (comprovado por teste ou pela leitura do código), **melhoria opcional** e
  **hipótese** ainda não verificada.
- Não implemente a correção automaticamente. Ela só entra com a aprovação do estudante, em issue ou PR próprio.
- Item que já tem Issue própria no GitHub é apenas referenciado, e não repetido.
- Inclua a atualização do documento no PR do requisito em andamento, quando fizer sentido (por exemplo, uma
  pendência descoberta durante o requisito). Caso contrário, registre na próxima alteração documental apropriada.
- Isso não é motivo para uma auditoria geral nem para abrir uma tarefa de correção.

## Gestão contínua de decisões arquiteturais (ADRs)

Os ADRs são documentos evolutivos e devem acompanhar
as decisões arquiteturais tomadas durante o projeto.

### Antes de implementar

- Consulte os ADRs aceitos relacionados à tarefa.
- Verifique se a solução proposta respeita essas decisões.
- Identifique decisões importantes que ainda não foram
  documentadas.

### Quando identificar uma nova decisão arquitetural

Considere criar um ADR quando a decisão:

- Introduzir ou modificar um padrão arquitetural.
- Alterar responsabilidades entre componentes ou camadas.
- Estabelecer uma estratégia técnica relevante.
- Modificar contratos ou integrações importantes.
- Envolver alternativas com consequências significativas.
- Afetar futuras implementações ou outras Issues.

Não crie ADRs para alterações triviais ou para
simplesmente descrever funcionalidades implementadas.

### Procedimento obrigatório

Quando identificar a necessidade de um ADR:

1. Informe explicitamente que uma decisão arquitetural
   precisa ser discutida ou registrada.
2. Explique o problema e seu contexto.
3. Apresente as alternativas relevantes.
4. Compare vantagens, desvantagens e consequências.
5. Indique se algum ADR existente já trata do assunto.
6. Recomende uma alternativa com justificativa.
7. Aguarde a aprovação do estudante antes de formalizar
   uma decisão ainda não tomada.

Após aprovação:

8. Crie um ADR com status "Proposta", seguindo o formato,
   a numeração e a localização existentes no projeto.
9. Relacione o ADR aos requisitos e Issues pertinentes.
10. Só marque o ADR como "Aceito" após a decisão ser
    explicitamente aprovada.
11. Atualize a documentação relacionada quando necessário.

### Alterações em ADRs existentes

Não modifique silenciosamente ADRs aceitos.

Se uma decisão vigente precisar ser substituída:

- Explique por que ela deixou de ser adequada.
- Identifique as consequências da mudança.
- Proponha um novo ADR ou siga a convenção de
  substituição já adotada pelo projeto.
- Preserve o histórico das decisões anteriores.

### Continuidade

Ao finalizar uma etapa, informe:

- Novos ADRs propostos ou criados.
- Decisões arquiteturais ainda pendentes.
- ADRs existentes impactados.
- Consequências para outras Issues.

Não considere uma decisão arquitetural aprovada
apenas porque ela já foi implementada no código.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.