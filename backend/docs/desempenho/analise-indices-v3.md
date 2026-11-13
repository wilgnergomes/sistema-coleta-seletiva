# Índices candidatos da V3: análise para o checkpoint (Issue #33)

**Status: aprovada em 2026-11-08 e implementada em `V3__criar_indices.sql`** (7 índices: I1, I2, I3, I4, I7, I9 e I10). Os três descartados (I5, I6 e I8) valem para o cenário atual; não é uma proibição permanente. O resultado da medição oficial depois da V3 está em [`relatorio-rnf04.md`](relatorio-rnf04.md).

## Como foi avaliado

- A medição oficial do RNF04 (1.000 solicitações, [`resultados-iniciais.md`](resultados-iniciais.md)) mostrou `Seq Scan` em todas as consultas, em menos de 1 ms: neste volume o planejador **não usaria** nenhum índice. Por isso a justificativa vem de um **volume ampliado e isolado**, que nunca se mistura com a medição oficial.
- O volume ampliado é o mesmo dataset reproduzido 99 vezes: **100.000 solicitações, 300.000 itens e 285.000 históricos** (`carga-volume-ampliado.sql`, determinístico), em um banco descartável (`coleta_desempenho_xl`).
- `explain-indices-v3.sql` roda `EXPLAIN (ANALYZE, BUFFERS)` das consultas reais (as do k6 e as de detalhe, RN05 e ADR 0013), **cria os índices candidatos dentro de uma transação que termina em `ROLLBACK`** e roda tudo de novo. O banco fica intacto e o procedimento é reproduzível. Saída completa em [`evidencias/indices-v3-volume-ampliado.txt`](evidencias/indices-v3-volume-ampliado.txt).
- Tempos de execução do PostgreSQL (ms), sem a aplicação. Uma máquina, uma execução; vale a ordem de grandeza e o plano escolhido.
- **Atenção à leitura da tabela:** os candidatos I1 a I8 foram criados **em conjunto**. Os planos e tempos da coluna "depois" mostram o resultado com todos presentes, não a contribuição isolada de cada índice (quando dois índices poderiam atender a mesma consulta, o planejador escolheu um deles). A atribuição de cada ganho a um índice específico é a mais provável pelo plano escolhido, não um teste isolado.

## Candidatos, consulta beneficiada e resultado

| # | Índice (tabela) | Consulta que beneficia | Antes → depois (ms) | Plano depois | Recomendação |
|---|---|---|---|---|---|
| I1 | `sol_solicitacao (poc_nr_id, sol_dt_criacao DESC, sol_nr_id DESC)` | RF09 do **OPERADOR** (contexto fixo do ponto) e filtro `pontoId` do administrador; RN05 `countByPontoIdAndSituacaoIn` | operador página 5,4 → 0,04; operador por período 6,3 → 0,06; operador por situação 6,9 → 0,06 (contagem 6,7 → 2,2); ADMIN por ponto 5,5 → 0,08 (contagem 5,0 → 0,7); RN05 abertas do ponto 5,5 → 2,0 | `Index Scan` na ordem do `ORDER BY`, sem sort | **Incluir** |
| I2 | `sol_solicitacao (mor_nr_id, sol_dt_criacao DESC, sol_nr_id DESC)` | RF09 do **MORADOR** | sem filtro 4,3 → 0,09 (contagem 4,3 → 0,12); por situação 5,8 → 1,1 | `Index Scan` / `Index Only Scan` | **Incluir** |
| I3 | `sol_solicitacao (sol_dt_criacao DESC, sol_nr_id DESC)` | RF09 do administrador **sem filtro** e **por período**; RF15 solicitações por situação **com período** | sem filtro, página 13,6 → 0,28; por período, página 8,8 → 0,07 (contagem 7,0 → 2,3); RF15 por período 9,5 → 5,5 | `Index Scan` na ordem; `Bitmap`/`Index Only Scan` na contagem | **Incluir** |
| I4 | `sol_solicitacao (sol_tx_situacao, sol_dt_criacao DESC, sol_nr_id DESC)` | RF09 do administrador **por situação** | CONCLUIDA (45%) 12,6 → 0,09; EM_RECOLHIMENTO (5%) 6,9 → 0,09 (contagem 6,0 → 0,8) | `Index Scan` na ordem | **Incluir** |
| I5 | `sol_solicitacao (sol_dt_conclusao) WHERE sol_tx_situacao = 'CONCLUIDA'` (parcial) | RF15 quantidades recolhidas **com período** | 27,1 → 21,7 (−20%). Sem período: sem ganho | `Bitmap Heap Scan` | **Descartar** (ganho pequeno; a consulta agrega dezenas de milhares de itens de qualquer forma) |
| I6 | `iso_item_solicitacao (mat_nr_id)` | ADR 0013 `contarItensDeSolicitacao`; RN05 com material | ADR 0013 10,8 → 4,0; RN05 com material 10,1 → 10,5 (sem ganho) | `Index Only Scan` só na contagem do ADR 0013 | **Descartar por ora** (só serve à troca de unidade de material, operação administrativa rara; nenhuma consulta de RF06/RF09/RF15 usa) |
| I7 | `his_historico (sol_nr_id)` | Detalhe da solicitação: `findBySolicitacaoIdOrderById` | 8,2 → 0,04 | `Index Scan` | **Incluir** (veja a observação sobre o ADR 0007) |
| I8 | `mac_material_aceito (mat_nr_id)` | ADR 0013 `contarVinculosComPontos`; RF06 por material | 0,03 → 0,03 (101 linhas; o `EXISTS` do RF06 já usa a PK `(poc_nr_id, mat_nr_id)`) | `Seq Scan` mantido | **Descartar** |
| I9 | `poc_ponto_coleta (lower(poc_tx_municipio))` | RF06 por município | com 20.020 pontos sintéticos: 6,6 → 0,04 | `Bitmap Heap Scan` no índice de expressão | **Incluir** |
| I10 | `poc_ponto_coleta (lower(poc_tx_bairro))` | RF06 por bairro | com 20.020 pontos sintéticos: 6,6 → 0,03 | `Bitmap Heap Scan` no índice de expressão | **Incluir** |

Resumo da escolha recomendada: **7 índices** (I1, I2, I3, I4, I7, I9, I10) e **3 descartados** (I5, I6, I8).

## Observações

1. **Custo de escrita:** inserir 1.000 solicitações levou **45,8 ms com os candidatos I1 a I5 presentes em `sol_solicitacao` e 30,0 ms sem eles**: **+15,8 ms em valores absolutos (+53% em termos relativos)**, ou cerca de 0,016 ms a mais por inserção. Esse teste envolveu os cinco candidatos de `sol_solicitacao` avaliados, **incluindo o I5, que foi descartado**, e não incluiu o `idx_his_sol`. Portanto **não representa o custo isolado dos sete aprovados** (quatro deles em `sol_solicitacao`: I1 a I4). Como o I5 é o único parcial e o aprovado tem um índice a menos, o custo real da V3 em `sol_solicitacao` tende a ser um pouco menor que o medido; não foi medido separadamente. As solicitações são criadas e mudam de situação em volume baixo, então o custo continua pequeno diante do ganho nas listagens.
2. **ADR 0007 versus a consulta real.** O ADR 0007 recomenda `his_historico (sol_nr_id, his_dt_data)`. A consulta real ordena por `his_nr_id`, então o índice proposto é só `(sol_nr_id)`: ele resolve a busca por solicitação, e as poucas linhas (2 a 6) são ordenadas depois sem custo. É um refinamento da recomendação, não uma mudança de decisão.
3. **I4 e I1:** para o operador filtrando por situação, o planejador escolhe I1 (filtra a situação sobre as 5.000 linhas do ponto) em vez de I4; ambos continuam justificados por consultas diferentes (administrador × operador).
4. **Pontos (I9 e I10):** a tabela real tem 20 linhas e o planejador continua usando `Seq Scan` nela; o ganho só aparece com muitos pontos (20.020 sintéticos, descartados no `ROLLBACK`). Eles se justificam porque o RF06 filtra exatamente por `lower(município)` e `lower(bairro)` e a rede de pontos tende a crescer; o custo é mínimo (pontos mudam raramente). A busca por nome com `LIKE '%texto%'` **não** é atendida por índice B-tree e fica fora desta V3 (exigiria `pg_trgm`, como já registrado em `docs/melhorias-pendentes.md`).
5. **O que não muda:** `RF09 sem filtro: contagem` (≈ 11 ms) e `RF15` por situação sem período (≈ 20 ms) continuam lendo a tabela inteira com 100.000 linhas, porque contam todas as linhas; nenhum índice resolve isso, e continuam muito abaixo de 2 s.
6. **Impacto na medição oficial (previsão e resultado):** a previsão era de que, com 1.000 solicitações, o planejador continuaria no `Seq Scan`. **Não foi o que aconteceu:** depois da V3 ele passou a usar os índices de `sol_solicitacao` já com 1.000 linhas (veja o relatório final). A previsão estava errada; o resultado medido é o que vale.

## Decisões tomadas (2026-11-08)

1. Aprovados os 7 índices (I1, I2, I3, I4, I7, I9 e I10), com as definições da tabela. Descartados I5, I6 e I8 para o cenário atual.
2. Os índices ficam só em `V3__criar_indices.sql`, com um comentário SQL relacionando cada um à consulta ou ao requisito beneficiado. O DER unificado continua sem índices de desempenho.
3. Teste `IndicesV3IntegracaoTest`: confere em `pg_indexes` a tabela, as colunas, a ordem e as expressões dos 7 índices e que os 3 descartados não existem, pelos nomes específicos, sem impedir índices legítimos futuros.
