# 24. Bloqueio pessimista da linha do ponto de coleta para a RN05

**Data:** 2026-11-08

**Status:** Aceita

## Contexto

A RN05 exige que a inativação de um ponto de coleta e a remoção de um vínculo de material sejam bloqueadas enquanto houver solicitações abertas afetadas. As solicitações abertas são SOLICITADA, AGENDADA e EM_RECOLHIMENTO (matriz, decisão do achado C-01). Essa regra se relaciona com a RN01, que exige ponto ativo e material aceito no momento de criar ou corrigir uma solicitação.

As duas regras, juntas, formam uma invariante: **não pode haver solicitação aberta em ponto inativo, nem com item de um material cujo vínculo com o ponto foi removido.** Cada operação sozinha a respeita, mas as concorrentes não. Sem coordenação, duas transações no isolamento padrão do PostgreSQL (READ COMMITTED) podem validar ao mesmo tempo e confirmar ambas:

| Tempo | Criação (ou correção) de solicitação | Inativação do ponto (ou remoção do vínculo) |
|---|---|---|
| 1 | Lê o ponto ATIVO e o vínculo existente | Conta as abertas e encontra zero |
| 2 | Grava a solicitação SOLICITADA | Grava o ponto INATIVO (ou apaga o vínculo) |
| 3 | Confirma | Confirma |

O resultado viola a RN05 e a RN01 ao mesmo tempo. O ADR 0009 já cobre a concorrência **sobre a mesma solicitação** (bloqueio otimista com `@Version`), mas aqui a disputa é entre **linhas diferentes** (a solicitação nova e o ponto ou o vínculo), e a versão da solicitação não ajuda: a solicitação nova ainda nem existe quando a outra operação conta.

Antes da decisão, um teste de estresse reproduziu a violação já na primeira rodada: um ponto inativado com quatro solicitações abertas criadas ao mesmo tempo.

## Decisão

1. O ponto de coleta é lido com **bloqueio pessimista de escrita** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`, que gera `SELECT ... FOR UPDATE` na linha de `poc_ponto_coleta`) por `PontoColetaRepository.findComBloqueioById`. O bloqueio dura até o fim da transação, que já é o limite de cada operação (`@Transactional`).
2. Quatro operações adquirem o bloqueio **antes de validar e de gravar**:
   - criação de solicitação (`SolicitacaoService.criar`);
   - correção de solicitação (`SolicitacaoService.corrigir`);
   - atualização do ponto, que inclui a inativação (`PontoColetaService.atualizar`);
   - remoção de vínculo (`MaterialAceitoService.remover`).
3. **Ordem de aquisição.** O bloqueio da linha do ponto é o **único bloqueio explícito** do projeto e é sempre o **primeiro** que a transação adquire, antes de qualquer outra linha que ela possa travar (solicitação, vínculo, itens). Regra para código futuro: *quem precisa travar o ponto, trava antes de qualquer outra linha.* Como há um só recurso explícito e ele vem primeiro, não existe ciclo de espera entre essas operações.
4. **Estado lido depois do bloqueio.** O estado do ponto usado nas validações é lido **depois** de adquirir o bloqueio. Na correção, a solicitação é lida sem bloqueio, mas o ponto é carregado apenas pelo método com bloqueio; o `getId()` do ponto associado não o inicializa. Em READ COMMITTED, quem espera o bloqueio enxerga, ao recebê-lo, o que a outra transação confirmou. Um teste determinístico cobre exatamente esse caso (correção que esperava e encontra o ponto inativado).
5. A RN05 em si usa duas contagens: solicitações abertas do ponto (inativação) e solicitações abertas do ponto com item do material (remoção de vínculo). Em ambos os casos a resposta é 409 `CONFLITO_REGRA`, com mensagem internacionalizada que informa a quantidade (e o nome do material na remoção).
6. As **transições de situação** (agendar, recusar, cancelar, iniciar, concluir, reagendar, falha) **não** adquirem o bloqueio do ponto: não criam solicitação aberta nova e continuam protegidas pelo `@Version` da própria solicitação (ADR 0009).
7. **Sem tempo máximo de espera nesta etapa.** O isolamento permanece READ COMMITTED, o padrão.

## Alternativas consideradas

| Alternativa | Avaliação |
|---|---|
| **Bloqueio pessimista da linha do ponto (escolhida)** | Sem mudança de esquema, usa a transação que já existe, serializa só as operações do mesmo ponto e é idiomática no JPA |
| `@Version` no ponto, com `OPTIMISTIC_FORCE_INCREMENT` na criação | Exige migração, faz toda solicitação gravar no ponto e a remoção de vínculo ainda precisaria alterar o ponto para colidir |
| Isolamento `SERIALIZABLE` | O PostgreSQL abortaria uma das transações, exigindo tratar falhas de serialização e repetir a operação; efeito global em todas as operações |
| Bloqueio por aviso (`pg_advisory_xact_lock`) | Funciona, mas exige SQL nativo e foge do JPA sem ganho sobre a escolhida |
| Aceitar a corrida e documentar o risco | Deixa o sistema violar uma regra obrigatória do professor (RN05) e a RN01 |

## Consequências

- **Positivas:** a invariante da RN05 e da RN01 vale também sob concorrência; nenhuma mudança de esquema nem de contrato (o 409 já estava descrito); as leituras (RF06, RF09, RF15) não bloqueiam nada e não afetam a medição do RNF04.
- **Negativas:** a criação e a correção de solicitações **do mesmo ponto** passam a ser serializadas, por transações curtas; entre pontos diferentes não há contenção. Uma operação espera enquanto a dona do bloqueio estiver ativa.
- **Cuidados:**
  - ler o ponto sem bloqueio nas quatro operações reabre a corrida; o teste `Rn05ConcorrenciaIntegracaoTest` cobre as duas ordens possíveis de cada par, com uma transação externa que segura o bloqueio e confirma a operação concorrente, e cobre o estresse com várias threads e os invariantes;
  - o PostgreSQL faz um `INSERT` com chave estrangeira tomar um bloqueio leve (`FOR KEY SHARE`) na linha do ponto, que conflita com o `FOR UPDATE`. Isso só faz o `FOR UPDATE` esperar a outra transação terminar, e ninguém espera por quem já está esperando, então não há ciclo;
  - um tempo máximo de espera, com resposta 409 de "tente novamente", fica como melhoria opcional em `docs/melhorias-pendentes.md`;
  - uma operação nova que crie solicitação aberta ou altere as condições do ponto (situação ou vínculos) deve seguir a regra de ordem do item 3.

Relacionado: RN01, RN05, RF04, RF05, RF08, Issue #57, ADR 0009, ADR 0013, ADR 0019, ADR 0023.
