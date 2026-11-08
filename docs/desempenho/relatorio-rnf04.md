# Relatório final do RNF04: desempenho das consultas

**Data:** 2026-11-08 · **Issues:** #58 e #33 · **Meta:** 95% das consultas em até 2.000 ms (p95), em teste local com 1.000 solicitações, registrando máquina, ferramenta e resultados.

**Conclusão: a meta é atendida com ampla folga antes e depois da V3** (pior p95 geral: 10,44 ms). A V3 criou sete índices que o PostgreSQL já passou a usar com 1.000 solicitações e que, no volume ampliado de 100.000, reduziram as consultas de milissegundos a décimos de milissegundo. **Ponta a ponta (HTTP, k6), a diferença antes/depois não é distinguível da variação entre execuções.**

Este relatório separa duas coisas que **não se misturam**:

1. **Medição oficial do RNF04:** exatamente 1.000 solicitações, k6 contra a aplicação, antes e depois da V3 (seções 2 a 5).
2. **Experimento de escalabilidade:** 100.000 solicitações, em banco descartável, só para avaliar planos e justificar índices; não é medição de desempenho da aplicação (seção 6).

## 1. Ambiente e método

| Item | Valor |
|---|---|
| Máquina | AMD Ryzen 7 5700X3D, 31,9 GB de RAM, Windows 11 Pro (10.0.26300) |
| Java / aplicação | OpenJDK 21.0.12.1; JAR empacotado (`java -jar`), sem DevTools, sem log SQL, sem perfil `demo` |
| Banco | PostgreSQL 17.11 em container Docker 29.8.1, banco dedicado `coleta_desempenho` |
| Ferramenta | k6 v1.8.2 (`grafana/k6:1.8.2`, digest `sha256:00fdcebafbc86cb0137d429529b1caa401a02e09025f02fc74866fa6e2b004d8`), em container → `host.docker.internal:8080` |
| Dataset | `carga-dataset.sql` **corrigido** (ponto, morador e situação independentes): 1.000 solicitações, 3.000 itens, 2.850 históricos, 200 moradores, 20 pontos |
| Roteiro | `k6-consultas.js` **inalterado** entre as duas medições: 17 variantes (5 RF06, 10 RF09, 2 RF15) |
| Fases | aquecimento (1 VU, 40 iterações, fora dos resultados); principal (1 VU, 200 iterações = 200 requisições por tipo, 600 medidas); complementar (10 VUs, 30 s) |

**Comparação válida.** As duas medições usam o **mesmo** banco (o mesmo dataset de 1.000 solicitações, sem recarga entre elas), o mesmo roteiro, a mesma versão do k6, a mesma máquina e o mesmo método. A única diferença é a migração V3 (de V2 para V3). A medição do dataset **original** (antes da correção) está em [`historico-dataset-original/`](historico-dataset-original/resultados-dataset-original.md) e **não entra nesta comparação**.

- **Antes da V3:** [`resultados-iniciais.md`](resultados-iniciais.md), evidências em [`evidencias/`](evidencias).
- **Depois da V3:** esta página, evidências em [`evidencias-pos-v3/`](evidencias-pos-v3).

## 2. Resultado: fase principal (1 VU, 600 requisições medidas)

| Consulta | p95 antes (ms) | p95 depois (ms) | média antes → depois (ms) |
|---|---|---|---|
| RF06 pesquisa de pontos | 6,88 | 7,21 | 5,08 → 5,33 |
| RF09 listagem de solicitações | 7,69 | 7,54 | 4,91 → 5,01 |
| RF15 resumo gerencial | 5,35 | 5,62 | 4,45 → 4,55 |
| **Geral** | **7,05** | **7,17** | 4,81 → 4,96 |

Depois da V3: 1.200 de 1.200 verificações aprovadas, 0 falhas HTTP, todos os limiares p95 < 2.000 ms aprovados (geral, por consulta e nas 17 variantes). p95 máximo de uma variante: 8,86 ms (RF09 ADMINISTRADOR, página sem filtro).

**Leitura:** as diferenças (de −0,15 a +0,33 ms) têm o tamanho do ruído de uma execução de 1 VU; **não há ganho nem perda distinguíveis ponta a ponta**. É o esperado: com 1.000 linhas o banco gasta décimos de milissegundo, e o tempo medido é quase todo da aplicação (HTTP, Hibernate, JSON).

## 3. Resultado: fase complementar (10 VUs, 30 s) e todas as tentativas

A fase foi executada **4 vezes antes e 4 vezes depois**; todas estão preservadas. Em cada medição vale a **primeira tentativa aprovada**, escolhida antes de olhar os números (regra definida na primeira medição).

| Medição | Tentativa | Resultado | Requisições (req/s) | Verificações falhas | p95 geral (ms) | Observação |
|---|---|---|---|---|---|---|
| Antes | 1 | aprovada (**usada**) | 46.805 (1.558) | 0 | 10,44 | |
| Antes | 2 | reprovada | 3.725 (113) | 20 | 5,76 | `dial: i/o timeout` |
| Antes | 3 | reprovada | 37.322 (685) | 20 | 6,38 | `dial: i/o timeout` |
| Antes | 4 | aprovada | 57.632 (1.919) | 0 | 7,36 | |
| Depois | 1 | reprovada | 38.903 (724) | 16 | 10,41 | `dial: i/o timeout` |
| Depois | 2 | aprovada (**usada**) | 57.926 (1.928) | 0 | 7,23 | |
| Depois | 3 | aprovada | 12.863 (428) | 0 | 5,33 | vazão baixa, sem falhas e sem timeout |
| Depois | 4 | aprovada | 57.797 (1.924) | 0 | 7,22 | |

Evidências: [`evidencias/tentativas-complementar/`](evidencias/tentativas-complementar) e [`evidencias-pos-v3/tentativas-complementar/`](evidencias-pos-v3/tentativas-complementar).

Tentativas usadas, p95 em ms (antes → depois): RF06 9,53 → 6,30; RF09 12,02 → 8,39; RF15 9,50 → 6,25; **geral 10,44 → 7,23**.

**Leitura:** a tentativa usada depois da V3 é menor que a usada antes, **mas isso não pode ser atribuído à V3**. O p95 geral variou de 5,33 a 10,44 ms **entre as tentativas de uma mesma medição**, e a vazão variou de 113 a 1.928 req/s; a faixa dos dois conjuntos se sobrepõe (antes: 5,76 a 10,44; depois: 5,33 a 10,41). Não há evidência de diferença causada pela V3 na fase complementar.

### Instabilidade: fatos e hipótese

- **Fatos:** nas fases complementares de todas as medições feitas com o roteiro definitivo (6 + 4 + 4 = 14 tentativas), 6 reprovaram por `dial: i/o timeout` (de 8 a 10 mensagens de timeout e de 16 a 20 verificações reprovadas por tentativa), sem nenhum erro registrado pela aplicação; as respostas recebidas foram rápidas. A fase principal (1 VU) **nunca** falhou. A tentativa 3 depois da V3 teve vazão de 428 req/s sem falhas, sem causa identificada.
- **Hipótese não comprovada:** a causa pode estar na rede do Docker Desktop (`host.docker.internal`), na configuração do servidor ou no roteiro; nenhuma foi descartada nem confirmada. **Não se atribui causa.** Mesmo ocorrendo antes e depois da V3, nada indica ligação com ela. A investigação continua na Issue #58.

## 4. Banco de dados: tempo de execução por consulta (EXPLAIN, 1.000 solicitações)

As 26 consultas de `explain-consultas.sql`, depois de `ANALYZE`, antes ([`evidencias/explain-inicial.txt`](evidencias/explain-inicial.txt)) e depois da V3 ([`evidencias-pos-v3/explain-pos-v3.txt`](evidencias-pos-v3/explain-pos-v3.txt)):

| Grupo | Plano antes | Plano depois | Tempo (soma do grupo, ms) |
|---|---|---|---|
| RF09 ADMINISTRADOR: situação, ponto, período, sem filtro (páginas e contagens) | `Seq Scan` + `top-N heapsort` | `Index Scan` / `Index Only Scan` em `idx_sol_situacao_criacao`, `idx_sol_poc_criacao`, `idx_sol_criacao` | 0,96 → 0,37 |
| RF09 MORADOR e OPERADOR | `Seq Scan` com condição fixa | `Index Scan`, `Index Only Scan` ou `Bitmap` em `idx_sol_mor_criacao` / `idx_sol_poc_criacao` | 0,66 → 0,30 |
| RF15 solicitações por situação | `Seq Scan` + `HashAggregate` | `Index Only Scan` em `idx_sol_situacao_criacao` | 0,31 → 0,22 |
| RF15 quantidades recolhidas | `Seq Scan` nas 3 tabelas | sem mudança relevante | 1,53 → 1,44 |
| RF06 pontos (20 linhas) | `Seq Scan` | `Seq Scan` (a tabela cabe em 1 página) | 0,29 → 0,25 |
| **Soma das 26 consultas** | | | **3,76 → 2,57 ms** |

**Leitura:** **o planejador passou a usar os índices de `sol_solicitacao` já com 1.000 linhas**, ao contrário do que a análise prévia previa (a previsão estava errada). Dentro do banco, a soma cai cerca de 32%, mas em valores absolutos são décimos de milissegundo por consulta, o que explica por que o p95 de ponta a ponta não muda. O RF06 continua em `Seq Scan` porque a tabela de pontos tem 20 linhas; os índices de expressão só são usados com muitos pontos (seção 6). Cada tempo é de uma execução, e os agrupamentos acima somam tempos individuais de execução única.

## 5. Atendimento do RNF04

| Critério | Resultado |
|---|---|
| Paginação com no máximo 100 por página | Já existente (`size` ≤ 100), exercitada no roteiro |
| Índices coerentes com os filtros | Sete índices na V3, um por filtro/ordenação das listagens (RF06, RF09, RF15, detalhe) |
| 95% das consultas em até 2 s com 1.000 solicitações | **Atendido:** pior p95 geral de 10,44 ms, p95 máximo de uma variante de 16,26 ms (antes da V3) e de 10,65 ms (depois da V3, tentativa usada), contra 2.000 ms |
| Máquina, ferramenta e resultados registrados | Sim (seção 1 e arquivos de evidência) |

## 6. Experimento de escalabilidade (100.000 solicitações): **não é a medição oficial**

Em um banco descartável (`coleta_desempenho_xl`), o mesmo dataset foi reproduzido 99 vezes (`carga-volume-ampliado.sql`: 100.000 solicitações, 300.000 itens, 285.000 históricos) e `explain-indices-v3.sql` comparou os planos sem e com os candidatos (criados **em conjunto**, dentro de uma transação que termina em `ROLLBACK`). Análise completa em [`analise-indices-v3.md`](analise-indices-v3.md) e saída em [`evidencias/indices-v3-volume-ampliado.txt`](evidencias/indices-v3-volume-ampliado.txt).

- Ganhos típicos no banco, em ms: operador/ponto/morador/período/situação de ~5 a 13 para ~0,04 a 0,3; histórico do detalhe de 8,2 para 0,04; pesquisa por município/bairro com 20.020 pontos sintéticos de 6,6 para ~0,04.
- Custo de escrita: +15,8 ms (+53%) para 1.000 inserções com os candidatos I1 a I5 presentes (45,8 contra 30,0 ms), **medido com o candidato descartado e sem o índice do histórico, portanto não é o custo isolado dos sete aprovados**.
- **Não há medição de k6 neste volume.** Os números são de planos de execução do banco, não de tempo de resposta da aplicação, e não são comparáveis com os da medição oficial.

## 7. Limitações

- Uma máquina; aplicação e banco no mesmo computador, via rede do Docker. Em produção haverá latência de rede.
- Dataset sintético; os números variam entre execuções (veja a seção 3). O que vale é a ordem de grandeza e o plano.
- Os ganhos de 100.000 linhas não foram medidos ponta a ponta.
- A causa da instabilidade da fase complementar segue desconhecida.
