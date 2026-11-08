# RNF04 — medição inicial (sem os índices da V3)

**Data:** 2026-11-08 · **Issue:** #58 (etapa 1) · **Meta:** p95 ≤ 2.000 ms com 1.000 solicitações · **Resultado: meta atendida, com folga de mais de 200 vezes.**

Esta é a linha de base **antes** dos índices da V3 (Issue #33). O esquema é o da V1, sem índices além das chaves e das restrições `UNIQUE`.

## Ambiente

| Item | Valor |
|---|---|
| Máquina | AMD Ryzen 7 5700X3D, 31,9 GB de RAM |
| Sistema | Windows 11 Pro (10.0.26300) |
| Java | OpenJDK 21.0.12.1 LTS, JAR empacotado (`java -jar`), sem DevTools, sem log SQL, sem perfil `demo` |
| Banco | PostgreSQL 17.11 em container Docker (`coleta-seletiva-db`), banco dedicado `coleta_desempenho` |
| Docker | 29.8.1 |
| k6 | **v1.8.2** (`grafana/k6:1.8.2`, go1.26.4, linux/amd64), digest `sha256:00fdcebafbc86cb0137d429529b1caa401a02e09025f02fc74866fa6e2b004d8` |
| Rede | k6 em container → `host.docker.internal:8080` (aplicação no host) |
| Dataset | `docs/desempenho/carga-dataset.sql`: 1.000 solicitações, 3.000 itens, 2.850 históricos, 200 moradores, 20 pontos, 8 materiais |

Confirmação do banco: antes da carga, `pg_stat_activity` mostrou 10 conexões do driver JDBC em `coleta_desempenho` (e nenhuma em `coleta_seletiva`, que só tinha o pgAdmin). O banco de desenvolvimento ficou sem nenhum registro `PERF`.

## Método

Aplicação e dataset fixos; k6 com tags `consulta=RF06|RF09|RF15`; fases em sequência, sempre com a mesma aplicação em execução (veja o [README](README.md)):

1. **Aquecimento**: 1 VU, 40 iterações, fora dos resultados.
2. **Principal**: 1 VU, 200 iterações (200 requisições de cada tipo, 600 no total).
3. **Complementar**: 10 VUs durante 30 s.

Cada requisição verificou status 200 e conteúdo esperado. Variantes por consulta: RF06 = município e nome+bairro; RF09 = situação, ponto, período e página sem filtro; RF15 = geral e por período.

## Resultados

### Principal — 1 VU, 200 iterações por tipo

| Consulta | Requisições | média | mediana | p90 | **p95** | máx. |
|---|---|---|---|---|---|---|
| RF06 pesquisa de pontos | 200 | 2,58 ms | 2,55 ms | 2,73 ms | **2,82 ms** | 4,85 ms |
| RF09 listagem de solicitações | 200 | 3,79 ms | 3,71 ms | 4,74 ms | **5,08 ms** | 5,86 ms |
| RF15 resumo gerencial | 200 | 3,05 ms | 3,09 ms | 3,32 ms | **3,36 ms** | 4,82 ms |
| **Geral** | **600** | 3,14 ms | 3,03 ms | 4,07 ms | **4,59 ms** | 5,86 ms |

Verificações: 1.200 de 1.200 (100%). Falhas HTTP: 0. Limiares p95 < 2.000 ms: todos aprovados.

### Complementar — 10 VUs, 30 s

| Consulta | média | mediana | **p95** | máx. |
|---|---|---|---|---|
| RF06 | 4,54 ms | 4,39 ms | **6,03 ms** | 22,08 ms |
| RF09 | 7,41 ms | 6,97 ms | **11,14 ms** | 36,53 ms |
| RF15 | 5,80 ms | 5,58 ms | **8,02 ms** | 20,58 ms |
| **Geral** | 5,92 ms | 5,53 ms | **9,48 ms** | 36,53 ms |

15.745 iterações, 47.237 requisições (≈ 1.573 req/s), 94.470 verificações (100% aprovadas), 0 falhas HTTP.

Dados brutos: [`evidencias/k6-principal-inicial.json`](evidencias/k6-principal-inicial.json) e [`evidencias/k6-complementar-inicial.json`](evidencias/k6-complementar-inicial.json).

### Intercorrência registrada

Na primeira tentativa da fase complementar, o `setup()` recebeu status 0 (falha de conexão transitória entre o container do k6 e o host) e **abortou sem medir**, como projetado; a API estava saudável (200). A fase foi repetida e os números acima são da repetição bem-sucedida. Na primeira rodada das fases principais, as 2 requisições do `setup()` entravam no p95 geral; passaram a levar `etapa=preparo` e as três fases foram refeitas, então todos os números oficiais usam o script final.

## EXPLAIN (ANALYZE, BUFFERS)

As 16 instruções SQL reais ([`evidencias/sql-real-inicial.txt`](evidencias/sql-real-inicial.txt)) foram analisadas depois de um `ANALYZE` ([`evidencias/explain-inicial.txt`](evidencias/explain-inicial.txt)). Todas as páginas lidas vieram do cache (`shared hit`), sem leitura de disco.

| Consulta | Plano | Tempo de execução | Páginas (buffers) |
|---|---|---|---|
| RF06 por município / nome+bairro | `Seq Scan` em `poc_ponto_coleta` (20 linhas) + sort | 0,03 a 0,06 ms | 1 a 33 |
| RF06 materiais dos pontos da página | `Seq Scan` em `mac_material_aceito` (101) + `mat_material` | 0,05 ms | 58 |
| RF09 página e contagem (situação, ponto, período, sem filtro) | `Seq Scan` em `sol_solicitacao` + `top-N heapsort` | 0,09 a 0,35 ms | 22 por varredura |
| RF15 solicitações por situação (com e sem período) | `Seq Scan` + `HashAggregate` | 0,11 a 0,21 ms | 22 |
| RF15 quantidades recolhidas (com e sem período) | `Seq Scan` nas 3 tabelas + hash join + sort | 0,44 a 0,90 ms | 23 a 46 |

## Conclusões

1. **A meta do RNF04 é atendida com ampla margem**: o pior p95 medido, em carga concorrente de 10 VUs, foi 11,14 ms, contra 2.000 ms. Os tempos de banco somam menos de 1 ms por consulta; o restante é o custo da aplicação (HTTP, Hibernate, JSON).
2. **Todas as consultas usam `Seq Scan`**, e isso é o plano **correto** neste volume: `sol_solicitacao` ocupa 22 páginas de 8 KB (cerca de 176 KB) e `poc_ponto_coleta` 1 página. Para tabelas assim, varrer tudo custa menos que consultar um índice. Os filtros de situação (6 valores, de 5% a 45%) e período (~16% das linhas) também não são seletivos o bastante para um índice vencer.
3. **Consequência para a Issue #33 (V3)**: com 1.000 solicitações, índices em `sol_tx_situacao`, `poc_nr_id` ou `sol_dt_criacao` provavelmente **não serão usados pelo planejador** e não mudarão a medição. A V3 só se justifica por evidência, e esta medição não mostra gargalo. A decisão de quais índices criar (ou de não criá-los) deve ser tomada após o EXPLAIN nos mesmos moldes e, se for o caso, com um volume maior de apoio. Isso será proposto no planejamento da #33.

## Limitações

- Uma máquina, uma sequência de execuções; os números variam entre execuções (a repetição das fases já mostrou diferenças de décimos de milissegundo). O que importa para a comparação é a ordem de grandeza e o plano.
- Os resultados são da aplicação e do banco no mesmo computador, via rede local do Docker; um ambiente de produção terá latência de rede adicional.
- O dataset é sintético. A distribuição de situações e datas é plausível, não derivada de dados reais.
