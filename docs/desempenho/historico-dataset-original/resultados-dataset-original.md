# RNF04 — medição inicial com o dataset original (histórico)

> **HISTÓRICO — NÃO COMPARAR COM A BASE CORRIGIDA.** Estes são os resultados da medição inicial feita com o **dataset original**, em que cada ponto e cada morador tinham uma única situação (defeito descrito na seção "Limitações"). O dataset foi corrigido na Issue #33 e a medição inicial foi repetida; os resultados vigentes estão em [`../resultados-iniciais.md`](../resultados-iniciais.md). Os números abaixo ficam preservados como registro e **não devem ser comparados diretamente** com os da base corrigida, porque a seletividade dos filtros é diferente.


**Data:** 2026-11-08 · **Issue:** #58 (etapa 1) · **Meta:** p95 ≤ 2.000 ms com 1.000 solicitações · **Resultado: meta atendida, com folga de mais de 200 vezes.**

Esta é a linha de base **antes** dos índices da V3 (Issue #33). O esquema é o da V1, sem índices além das chaves e das restrições `UNIQUE`.

> **Roteiro definitivo.** Os números deste documento vêm do roteiro `k6-consultas.js` **definitivo** (17 variantes de consulta: 5 de RF06, 10 de RF09 e 2 de RF15). Uma primeira versão do roteiro, com 8 variantes (RF06: município e bairro; RF09: só ADMINISTRADOR; RF15: duas), foi medida antes e **ficou apenas como histórico**, em [`evidencias/historico-roteiro-v1/`](evidencias/historico-roteiro-v1). Ela não é comparável com a V3. A comparação futura deve usar o roteiro definitivo.

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
| Dataset | `docs/desempenho/carga-dataset.sql`: 1.000 solicitações, 3.000 itens, 2.850 históricos, 200 moradores, 20 pontos, 8 materiais (o mesmo dataset da primeira versão do roteiro, sem recarga) |

Confirmação do banco: o JAR abriu 10 conexões JDBC em `coleta_desempenho`, e o `setup()` do k6 recusa medir se o banco não tiver exatamente o dataset. O banco de desenvolvimento ficou sem nenhum registro `PERF`.

## Método

Aplicação e dataset fixos; fases em sequência, sempre com a mesma aplicação em execução (veja o [README](../README.md)):

1. **Aquecimento**: 1 VU, 40 iterações, fora dos resultados.
2. **Principal**: 1 VU, 200 iterações = **200 requisições de cada tipo** (600 medidas).
3. **Complementar**: 10 VUs durante 30 s.

Em cada iteração, cada tipo escolhe uma variante em rodízio; o parâmetro avança a cada volta. Os ids de pontos, materiais, moradores e operadores são **descobertos no `setup()` pelos endpoints** (nunca fixos), então a medição se repete igual depois de limpar e recarregar a base.

| Consulta | Variantes (requisições por variante na fase principal) |
|---|---|
| RF06 pesquisa de pontos (público) | município, bairro, **material**, **filtros combinados** (nome + município + bairro + material), **paginação** em páginas de 5 (páginas 0 a 3 do total de 20), 40 cada |
| RF09 listagem de solicitações | ADMINISTRADOR: situação, ponto, período e página sem filtro; **MORADOR** e **OPERADOR**: sem filtro, por situação e por período; 20 cada |
| RF15 resumo gerencial | geral e por período, 100 cada |

Cada requisição verificou o status 200 e o conteúdo: listas não vazias quando o filtro garante resultado; o ponto esperado presente no filtro combinado; página de 5 itens e total 20 na paginação; sem filtro, o morador vê exatamente suas 5 solicitações e o operador as 50 do seu ponto; nos filtros de situação e período do morador e do operador, formato e limite coerentes (podem legitimamente ficar vazios).

## Resultados

### Principal — 1 VU, 200 iterações por tipo (600 requisições medidas)

| Consulta | média | mediana | p90 | **p95** | máx. |
|---|---|---|---|---|---|
| RF06 pesquisa de pontos | 2,85 ms | 2,70 ms | 3,34 ms | **3,52 ms** | 4,22 ms |
| RF09 listagem de solicitações | 3,11 ms | 2,84 ms | 4,38 ms | **4,80 ms** | 5,96 ms |
| RF15 resumo gerencial | 3,05 ms | 3,07 ms | 3,28 ms | **3,34 ms** | 4,88 ms |
| **Geral** | 3,00 ms | 2,95 ms | 3,67 ms | **4,22 ms** | 5,96 ms |

Verificações: 1.200 de 1.200 (100%). Falhas HTTP: 0. Todos os limiares p95 < 2.000 ms aprovados, inclusive os das 17 variantes.

**p95 por variante (principal):**

| Variante | p95 | Variante | p95 |
|---|---|---|---|
| RF06 município | 2,79 ms | RF09 ADMIN situação | 4,11 ms |
| RF06 bairro | 2,95 ms | RF09 ADMIN ponto | 4,26 ms |
| RF06 material | 3,91 ms | RF09 ADMIN período | 4,27 ms |
| RF06 combinado | 2,65 ms | RF09 ADMIN página sem filtro | 5,90 ms |
| RF06 paginação | 3,39 ms | RF09 MORADOR sem filtro / situação / período | 2,54 / 2,59 / 2,52 ms |
| RF15 geral | 3,40 ms | RF09 OPERADOR sem filtro / situação / período | 4,38 / 2,48 / 2,62 ms |
| RF15 período | 3,07 ms | | |

### Complementar — 10 VUs, 30 s (tentativa 2, a primeira aprovada)

| Consulta | média | mediana | p90 | **p95** | máx. |
|---|---|---|---|---|---|
| RF06 | 4,82 ms | 4,62 ms | 6,21 ms | **6,83 ms** | 15,83 ms |
| RF09 | 5,64 ms | 5,32 ms | 8,34 ms | **9,41 ms** | 22,03 ms |
| RF15 | 5,43 ms | 5,23 ms | 6,66 ms | **7,29 ms** | 16,66 ms |
| **Geral** | 5,30 ms | 5,03 ms | 7,19 ms | **8,12 ms** | 22,03 ms |

17.679 iterações, 53.042 requisições (≈ 1.766 req/s), 106.074 verificações (100% aprovadas), 0 falhas HTTP. O pior p95 de uma variante foi 12,13 ms (RF09 ADMINISTRADOR, página sem filtro, 100 itens por página).

Dados brutos: [`k6-principal-inicial.json`](evidencias/k6-principal-inicial.json) e [`k6-complementar-inicial.json`](evidencias/k6-complementar-inicial.json), com o p95 de cada variante.

### Instabilidade do ambiente na fase complementar (registro completo)

Com o roteiro definitivo, a fase complementar foi executada **6 vezes**. Os resultados, todos preservados em [`evidencias/tentativas-complementar/`](evidencias/tentativas-complementar):

| Tentativa | Resultado | Requisições (req/s) | Verificações falhas | p95 geral | Observação |
|---|---|---|---|---|---|
| 1 | reprovada | 25.088 (672) | 16 | 6,67 ms | `dial: i/o timeout` |
| **2** | **aprovada** | 53.042 (1.766) | 0 | 8,12 ms | **a usada nas tabelas acima (a primeira aprovada)** |
| 3 | reprovada | 15.527 (375) | 20 | 7,86 ms | `dial: i/o timeout` |
| 4 | reprovada | 30.929 (611) | 20 | 8,09 ms | `dial: i/o timeout` |
| 5 | aprovada | 52.358 (1.743) | 0 | 8,20 ms | |
| 6 | aprovada | 36.725 (1.110) | 0 | 7,80 ms | |

Fatos observados nas três reprovações: foram **apenas timeouts na abertura de conexão** entre o container do k6 e o host (`dial: i/o timeout`, 8 a 20 requisições perdidas em cada); a aplicação não registrou nenhum erro; todas as respostas recebidas foram rápidas (p95 entre 6,7 e 8,1 ms em todas as tentativas, aprovadas ou não); e a fase principal, de 1 VU, nunca falhou. O roteiro tratou as falhas como deve: reprovou a medição em vez de ignorá-las. Nas 6 tentativas o p95 geral ficou entre 6,67 e 8,20 ms.

**Causa: hipótese não comprovada.** A suspeita inicial é a rede do Docker Desktop no Windows (`host.docker.internal`) sob carga concorrente, mas **não foi verificada**: o problema também pode estar na configuração do servidor (fila de conexões do Tomcat, pool) ou no próprio roteiro. Nenhuma dessas causas está descartada. A investigação e a estabilidade da medição final são acompanhadas na Issue #58; a entrada em `docs/melhorias-pendentes.md` só aponta para cá.

## EXPLAIN (ANALYZE, BUFFERS)

As instruções SQL reais ([`evidencias/sql-real-inicial.txt`](evidencias/sql-real-inicial.txt)), capturadas do PostgreSQL com `log_statement='all'` à parte da medição, foram analisadas depois de um `ANALYZE` ([`evidencias/explain-inicial.txt`](evidencias/explain-inicial.txt), gerado por `explain-consultas.sql`). Todas as páginas lidas vieram do cache (`shared hit`), sem leitura de disco.

| Consulta | Plano | Tempo de execução | Páginas (buffers) |
|---|---|---|---|
| RF06 por município / nome+bairro | `Seq Scan` em `poc_ponto_coleta` (20 linhas) + sort | 0,03 a 0,06 ms | 1 a 33 |
| RF06 **por material** (`EXISTS` no vínculo) | `Seq Scan` em `poc_ponto_coleta` e `mac_material_aceito` (101 linhas), subconsulta `EXISTS` | 0,05 ms | poucas |
| RF06 **combinado** (nome, município, bairro e material) | `Seq Scan` com os filtros no ponto + `EXISTS` | 0,06 ms | poucas |
| RF06 **paginação** (página e contagem) | `Seq Scan` + sort / `Aggregate` | 0,03 e 0,03 ms | 1 |
| RF06 materiais dos pontos da página | `Seq Scan` em `mac_material_aceito` + `mat_material` | 0,05 ms | 58 |
| RF09 ADMINISTRADOR (situação, ponto, período, sem filtro) | `Seq Scan` em `sol_solicitacao` + `top-N heapsort` | 0,09 a 0,35 ms | 22 por varredura |
| RF09 **MORADOR** (sem filtro / situação / período) | `Seq Scan` em `sol_solicitacao` com `mor_nr_id` como condição fixa | 0,07 / 0,17 / 0,09 ms | 22 |
| RF09 **OPERADOR** (sem filtro / situação / período, com contagem) | `Seq Scan` em `sol_solicitacao` com `poc_nr_id` como condição fixa | 0,07 a 0,11 ms | 22 |
| RF15 solicitações por situação (com e sem período) | `Seq Scan` + `HashAggregate` | 0,11 a 0,21 ms | 22 |
| RF15 quantidades recolhidas (com e sem período) | `Seq Scan` nas 3 tabelas + hash join + sort | 0,44 a 0,90 ms | 23 a 46 |

(O `Index Scan using pk_...` que aparece nos planos de MORADOR e OPERADOR é só o `min(...)` usado no script para achar o id do ator; a consulta da aplicação lê `sol_solicitacao` por varredura.)

## Conclusões

1. **A meta do RNF04 é atendida com ampla margem**: o pior p95 geral, em carga concorrente de 10 VUs, foi 8,20 ms (qualquer tentativa), contra 2.000 ms. Os tempos de banco somam menos de 1 ms por consulta; o restante é o custo da aplicação (HTTP, Hibernate, JSON).
2. **Todas as consultas usam `Seq Scan`**, inclusive a pesquisa por material, os filtros combinados e os contextos de morador e operador. Neste volume é o plano **correto**: `sol_solicitacao` ocupa 22 páginas de 8 KB (cerca de 176 KB) e `poc_ponto_coleta` 1 página; varrer tudo custa menos que consultar um índice.
3. **Consequência para a Issue #33 (V3)**: com 1.000 solicitações, índices em `sol_tx_situacao`, `poc_nr_id`, `mor_nr_id` ou `sol_dt_criacao` provavelmente **não serão usados pelo planejador** e não mudarão a medição. A V3 só se justifica por evidência, e esta medição não mostra gargalo. A decisão de quais índices criar (ou de não criá-los) deve ser tomada após o `EXPLAIN` nos mesmos moldes e, se for o caso, com um volume maior de apoio, o que será proposto no planejamento da #33.

## Limitações

- **Defeito confirmado no dataset: correlação entre ponto, morador e situação.** Diagnóstico e evidência (fonte única; o plano de correção está na Issue #33). Consulta ao banco `coleta_desempenho`: `count(DISTINCT sol_tx_situacao)` por ponto e por morador é **1** em todos (mínimo 1, máximo 1), e `count(DISTINCT poc_nr_id)` por morador também é **1**. Causa: as fórmulas determinísticas de `carga-dataset.sql` (`ponto = (i*7) mod 20`, situação por `i mod 20`, `morador = (i-1) mod 200`) ligam as três dimensões, então as 50 solicitações de um ponto são todas da mesma situação e as 5 de um morador, também do mesmo ponto. Efeitos: "operador por situação" devolve as 50 do ponto ou nenhuma, o filtro por ponto equivale ao filtro por situação, e a **seletividade vista pelo planejador é irreal**. A distribuição global (450/150/150/100/100/50) está correta, e a conclusão atual (tudo é `Seq Scan` e abaixo de 1 ms) não muda. O dataset **não foi alterado** nesta etapa. **Estes resultados são o histórico da base original: depois de corrigida a distribuição, a medição inicial é repetida e os números novos não devem ser comparados diretamente com estes.**
- Uma máquina e uma sequência de execuções; os números variam entre execuções (décimos de milissegundo na fase principal). O que importa para a comparação é a ordem de grandeza e o plano.
- Aplicação e banco no mesmo computador, via rede local do Docker; um ambiente de produção terá latência de rede adicional.
- O dataset é sintético. A distribuição de situações e datas é plausível, não derivada de dados reais.
- A fase complementar sofre de instabilidade intermitente do ambiente (seção acima).
