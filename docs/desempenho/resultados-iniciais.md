# RNF04 — medição inicial (sem os índices da V3)

**Data:** 2026-11-08 · **Issue:** #58 (etapa 1, repetida na #33) · **Meta:** p95 ≤ 2.000 ms com 1.000 solicitações · **Resultado: meta atendida, com folga de mais de 150 vezes.** Comparação com a medição depois da V3: [`relatorio-rnf04.md`](relatorio-rnf04.md).

Esta é a linha de base **antes** dos índices da V3 (Issue #33), medida com o **dataset corrigido**. O esquema é o da V1 mais a V2 (só o tamanho da cópia do endereço), sem índices além das chaves e das restrições `UNIQUE`.

> **Dataset corrigido e medição repetida.** A primeira medição usou um dataset em que cada ponto e cada morador tinham uma única situação, o que distorcia a seletividade dos filtros. A distribuição foi corrigida (ponto, morador e situação independentes; mesmas proporções globais) e a medição inicial foi **repetida do zero** com o mesmo roteiro e o mesmo método. Os resultados da base original ficam em [`historico-dataset-original/`](historico-dataset-original/resultados-dataset-original.md) **apenas como histórico: não são comparados com os desta página**, porque os filtros agora têm outra seletividade. A comparação válida para a V3 é entre esta página e a medição final.

## Ambiente

| Item | Valor |
|---|---|
| Máquina | AMD Ryzen 7 5700X3D, 31,9 GB de RAM |
| Sistema | Windows 11 Pro (10.0.26300) |
| Java | OpenJDK 21.0.12.1 LTS, JAR empacotado (`java -jar`), sem DevTools, sem log SQL, sem perfil `demo` |
| Banco | PostgreSQL 17.11 em container Docker (`coleta-seletiva-db`), banco dedicado `coleta_desempenho`, recarregado com `limpar-dataset.sql` + `carga-dataset.sql` |
| Docker | 29.8.1 |
| k6 | **v1.8.2** (`grafana/k6:1.8.2`), digest `sha256:00fdcebafbc86cb0137d429529b1caa401a02e09025f02fc74866fa6e2b004d8` |
| Rede | k6 em container → `host.docker.internal:8080` (aplicação no host) |
| Dataset | 1.000 solicitações, 3.000 itens, 2.850 históricos, 200 moradores, 20 pontos, 8 materiais |

O JAR abriu 10 conexões JDBC em `coleta_desempenho`, e o `setup()` do k6 recusa medir se o banco não tiver exatamente o dataset. O banco de desenvolvimento ficou sem nenhum registro `PERF`.

## Dataset corrigido

| Propriedade | Antes (original) | Agora |
|---|---|---|
| Situações distintas por ponto | 1 | **6** (nenhuma passa de 27 das 50 solicitações do ponto) |
| Situações distintas por morador | 1 | **2 a 5** |
| Pontos distintos por morador | 1 | **5** |
| Solicitações por ponto / por morador | 50 / 5 | 50 / 5 (igual) |
| Distribuição global por situação | 450/150/150/100/100/50 | 450/150/150/100/100/50 (igual) |

Verificado por teste (`DatasetDesempenhoIntegracaoTest`): por exemplo, "operador por situação" devolve uma parte das 50 do ponto (nem zero, nem todas) e o filtro ponto + situação não é mais equivalente ao filtro por ponto.

## Método

Aplicação e dataset fixos; o roteiro `k6-consultas.js` não foi alterado (17 variantes: 5 de RF06, 10 de RF09 e 2 de RF15). Fases em sequência (veja o [README](README.md)): **aquecimento** (1 VU, 40 iterações, fora dos resultados), **principal** (1 VU, 200 iterações = 200 requisições de cada tipo, 600 medidas) e **complementar** (10 VUs, 30 s). Os ids são descobertos no `setup()`. Cada requisição verifica status 200 e conteúdo.

## Resultados

### Principal — 1 VU, 200 iterações por tipo (600 requisições medidas)

| Consulta | média | mediana | p90 | **p95** | máx. |
|---|---|---|---|---|---|
| RF06 pesquisa de pontos | 5,08 ms | 4,88 ms | 6,49 ms | **6,88 ms** | 10,39 ms |
| RF09 listagem de solicitações | 4,91 ms | 4,63 ms | 7,05 ms | **7,69 ms** | 9,60 ms |
| RF15 resumo gerencial | 4,45 ms | 4,30 ms | 5,12 ms | **5,35 ms** | 13,08 ms |
| **Geral** | 4,81 ms | 4,57 ms | 6,42 ms | **7,05 ms** | 13,08 ms |

Verificações: 1.200 de 1.200 (100%). Falhas HTTP: 0. Todos os limiares p95 < 2.000 ms aprovados, inclusive os das 17 variantes.

**p95 por variante (principal):**

| Variante | p95 | Variante | p95 |
|---|---|---|---|
| RF06 município | 6,02 ms | RF09 ADMIN situação | 7,37 ms |
| RF06 bairro | 6,18 ms | RF09 ADMIN ponto | 7,36 ms |
| RF06 material | 7,94 ms | RF09 ADMIN período | 7,53 ms |
| RF06 combinado | 5,79 ms | RF09 ADMIN página sem filtro | 8,89 ms |
| RF06 paginação | 6,91 ms | RF09 MORADOR sem filtro / situação / período | 4,54 / 4,54 / 4,89 ms |
| RF15 geral | 5,20 ms | RF09 OPERADOR sem filtro / situação / período | 7,81 / 5,14 / 5,31 ms |
| RF15 período | 5,47 ms | | |

### Complementar — 10 VUs, 30 s (tentativa 1, a primeira aprovada)

| Consulta | média | mediana | p90 | **p95** | máx. |
|---|---|---|---|---|---|
| RF06 | 5,66 ms | 5,14 ms | 8,16 ms | **9,53 ms** | 25,24 ms |
| RF09 | 6,42 ms | 5,77 ms | 10,19 ms | **12,02 ms** | 37,94 ms |
| RF15 | 6,03 ms | 5,52 ms | 8,21 ms | **9,50 ms** | 25,34 ms |
| **Geral** | 6,04 ms | 5,46 ms | 8,90 ms | **10,44 ms** | 37,94 ms |

15.600 iterações, 46.805 requisições (≈ 1.558 req/s), 93.600 verificações (100% aprovadas), 0 falhas HTTP. O pior p95 de uma variante foi 16,26 ms (RF09 ADMINISTRADOR, página sem filtro, 100 itens por página).

Dados brutos: [`k6-principal-inicial.json`](evidencias/k6-principal-inicial.json) e [`k6-complementar-inicial.json`](evidencias/k6-complementar-inicial.json), com o p95 de cada variante.

### Tentativas da fase complementar (registro completo)

| Tentativa | Resultado | Requisições (req/s) | Verificações falhas | p95 geral | Observação |
|---|---|---|---|---|---|
| **1** | **aprovada** | 46.805 (1.558) | 0 | 10,44 ms | **a usada nas tabelas (a primeira aprovada)** |
| 2 | reprovada | 3.725 (113) | 20 | 5,76 ms | `dial: i/o timeout` |
| 3 | reprovada | 37.322 (685) | 20 | 6,38 ms | `dial: i/o timeout` |
| 4 | aprovada | 57.632 (1.919) | 0 | 7,36 ms | |

Arquivos em [`evidencias/tentativas-complementar/`](evidencias/tentativas-complementar). As duas reprovações foram **apenas timeouts na abertura de conexão** entre o container do k6 e o host (10 requisições perdidas em cada); a aplicação não registrou erro, as respostas recebidas foram rápidas e a fase principal nunca falhou. **A causa é hipótese não comprovada** (rede do Docker Desktop, configuração do servidor ou o roteiro), acompanhada na Issue #58 e em `docs/melhorias-pendentes.md`. Esta é a mesma instabilidade já vista na medição original, de modo que ela não está ligada ao dataset. **Atualização (Issue #58, 2026-11-08):** a instabilidade foi diagnosticada e contornada; veja a seção 3.1 do [`relatorio-rnf04.md`](relatorio-rnf04.md). Os números e as tentativas desta página não foram alterados.

## EXPLAIN (ANALYZE, BUFFERS)

`explain-consultas.sql` (as 16 instruções reais do Hibernate, mais as variantes de material, combinado, paginação e contextos de morador e operador) foi executado depois de um `ANALYZE` ([`evidencias/explain-inicial.txt`](evidencias/explain-inicial.txt); SQL real em [`evidencias/sql-real-inicial.txt`](evidencias/sql-real-inicial.txt), as mesmas consultas, pois só os dados mudaram). Todas as páginas vieram do cache.

| Consulta | Plano | Tempo de execução |
|---|---|---|
| RF06 município, nome+bairro, material, combinado, paginação | `Seq Scan` em `poc_ponto_coleta` (20 linhas) e, no material/combinado, subconsulta `EXISTS` sobre `mac_material_aceito` (101 linhas) | 0,02 a 0,07 ms |
| RF09 ADMINISTRADOR (situação, ponto, período, sem filtro) | `Seq Scan` em `sol_solicitacao` + `top-N heapsort` | 0,06 a 0,30 ms |
| RF09 MORADOR e OPERADOR (sem filtro, situação, período) | `Seq Scan` em `sol_solicitacao` com `mor_nr_id` ou `poc_nr_id` como condição fixa | 0,07 a 0,09 ms |
| RF15 solicitações por situação (com e sem período) | `Seq Scan` + `HashAggregate` | 0,10 a 0,21 ms |
| RF15 quantidades recolhidas (com e sem período) | `Seq Scan` nas 3 tabelas + hash join + sort | 0,60 a 0,94 ms |

(O `Index Scan using pk_...` que aparece em alguns planos é só o `min(...)` usado no script para achar o id do ator.)

## Conclusões

1. **A meta do RNF04 é atendida com ampla margem**: o pior p95 geral, em carga concorrente de 10 VUs, foi 10,44 ms (qualquer tentativa), contra 2.000 ms. Os tempos de banco somam menos de 1 ms por consulta; o restante é o custo da aplicação (HTTP, Hibernate, JSON).
2. **Todas as consultas usam `Seq Scan`**, inclusive com a seletividade realista do dataset corrigido. Neste volume é o plano correto: `sol_solicitacao` ocupa 22 páginas de 8 KB e `poc_ponto_coleta`, 1 página. A decisão sobre os índices da V3 está em [`analise-indices-v3.md`](analise-indices-v3.md), com os candidatos avaliados em um volume ampliado e isolado, que **não** faz parte desta medição oficial.

## Limitações

- Uma máquina e uma sequência de execuções; os números variam entre execuções (a primeira medição, com o dataset original, já mostrou isso). O que importa é a ordem de grandeza e o plano.
- Aplicação e banco no mesmo computador, via rede local do Docker; um ambiente de produção terá latência de rede adicional.
- O dataset é sintético. A distribuição de situações e datas é plausível, não derivada de dados reais.
- A fase complementar sofria de instabilidade intermitente do ambiente. Diagnosticada e contornada na Issue #58 (veja a seção 3.1 do [`relatorio-rnf04.md`](relatorio-rnf04.md)); esta medição foi feita antes do contorno.
