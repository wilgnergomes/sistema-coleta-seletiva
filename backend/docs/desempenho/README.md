# Desempenho (RNF04) — dataset e método de medição

O RNF04 pede que a pesquisa de pontos (RF06), a listagem de solicitações (RF09) e o resumo gerencial (RF15) respondam em até **2 segundos (p95)** com **1.000 solicitações** cadastradas. Esta pasta reúne tudo o que é preciso para repetir a medição, antes e depois dos índices da V3 (Issue #33), sempre com o **mesmo dataset, os mesmos parâmetros e o mesmo método**.

| Arquivo | Para quê |
|---|---|
| [`carga-dataset.sql`](carga-dataset.sql) | Carrega o dataset `PERF` (determinístico, em uma transação) |
| [`limpar-dataset.sql`](limpar-dataset.sql) | Remove somente os registros `PERF` |
| [`k6-consultas.js`](k6-consultas.js) | Script do k6: fases `aquecimento`, `principal` e `complementar` |
| [`explain-consultas.sql`](explain-consultas.sql) | `EXPLAIN (ANALYZE, BUFFERS)` das consultas reais |
| [`resultados-iniciais.md`](resultados-iniciais.md) | Medição oficial **antes** da V3 (dataset corrigido) |
| [`relatorio-rnf04.md`](relatorio-rnf04.md) | **Relatório final**: comparação antes/depois da V3, todas as tentativas e o experimento de escalabilidade |
| [`evidencias-pos-v3/`](evidencias-pos-v3) | Saídas brutas da medição oficial **depois** da V3 (k6, tentativas da fase complementar e `EXPLAIN`) |
| [`evidencias-estabilidade/`](evidencias-estabilidade) | Diagnóstico da instabilidade da fase `complementar` (Issue #58): reprodução, testes isolados, log do Docker Desktop e a validação do contorno em 15 rodadas |
| [`evidencias/`](evidencias) | Saídas brutas da medição **antes** da V3: planos, resumos do k6 (inclusive todas as tentativas da fase complementar), SQL capturado e a avaliação dos índices |
| [`carga-volume-ampliado.sql`](carga-volume-ampliado.sql) | Volume ampliado **isolado** (100.000 solicitações), só para avaliar planos; fora da medição oficial |
| [`explain-indices-v3.sql`](explain-indices-v3.sql) | Compara os planos sem e com os índices candidatos, em transação que termina em `ROLLBACK` |
| [`analise-indices-v3.md`](analise-indices-v3.md) | Candidatos a índice, consulta beneficiada, `EXPLAIN` e recomendação (Issue #33) |
| [`historico-dataset-original/`](historico-dataset-original) | Medição feita com o dataset original (cada ponto e cada morador com uma única situação). Só histórico: **não comparar** com a base corrigida |

Nada disso roda sozinho: o dataset **não** faz parte do seed `demo` e a aplicação nunca o carrega. Os testes de integração (`DatasetDesempenhoIntegracaoTest`) o carregam em um PostgreSQL descartável do Testcontainers.

## O dataset

- **1.000 solicitações** criadas ao longo de 12 meses (nov/2025 a out/2026, fuso America/Sao_Paulo), com 2 a 4 itens cada (3.000 itens) e 2.850 linhas de histórico.
- **200 moradores** (um endereço cada), **20 pontos** ativos em 8 municípios e 5 bairros, **8 materiais** (KG, L e UN), **101 vínculos** (4 a 6 materiais por ponto) e **20 operadores** (um por ponto).
- Situações: CONCLUIDA 450, SOLICITADA 150, AGENDADA 150, RECUSADA 100, CANCELADA 100, EM_RECOLHIMENTO 50.
- **Ponto, morador e situação são independentes**: cada ponto tem as 6 situações e cada morador distribui as 5 solicitações por 5 pontos diferentes (corrigido na Issue #33; o dataset original os correlacionava).
- **Determinístico**: não usa `random()`. Cada carga gera exatamente os mesmos dados, o que torna os números comparáveis entre medições.
- Respeita as regras do sistema (RN01, RN03, ADRs 0008, 0010 e 0011): ponto ativo e domiciliar, materiais aceitos pelo ponto, limites do vínculo, unidade e endereço copiados, histórico coerente com a situação atual e quantidade efetiva só nas concluídas.
- Marcador **PERF**: e-mails `perf.morador.NNN@exemplo.com` e `perf.operador.NN@exemplo.com`, e nomes `PERF ...`. A limpeza e a guarda contra carga repetida usam esse marcador.

A carga recusa uma segunda execução (`O dataset de desempenho já foi carregado...`). Para repetir, rode antes a limpeza. A limpeza apaga na ordem inversa das chaves estrangeiras e preserva qualquer outro dado.

## Como medir (passo a passo)

Pré-requisitos: container `coleta-seletiva-db` em execução, Docker, JDK 21. Os comandos abaixo são executados na pasta `backend/` do repositório. **Use um banco dedicado**, sem tocar no de desenvolvimento.

```bash
# 1. Banco dedicado, no container que já existe (não recria o container)
docker exec coleta-seletiva-db psql -U coleta -d postgres -c "CREATE DATABASE coleta_desempenho"

# 2. Empacota e sobe o JAR apontando para o banco dedicado (sem DevTools, sem log SQL, sem perfil demo).
#    DB_NAME no ambiente tem precedência sobre o .env, que continua intacto.
#    SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS=-1 tira o limite de 100 requisições por conexão do Tomcat, só neste
#    processo: sem ele, o k6 em container precisa abrir ~20 conexões novas por segundo através do Docker Desktop e
#    isso causa travamentos de ~30 s (`dial: i/o timeout`). Não altera application.properties nem o código.
./mvnw -DskipTests package
DB_NAME=coleta_desempenho SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS=-1 java -jar target/coleta-seletiva-0.0.1-SNAPSHOT.jar

# 3. Confira que o JAR está no banco certo (deve listar coleta_desempenho com o driver JDBC)
docker exec coleta-seletiva-db psql -U coleta -d postgres -c \
  "SELECT datname, application_name, count(*) FROM pg_stat_activity WHERE datname LIKE 'coleta%' GROUP BY 1,2"

# 4. Carrega o dataset (uma transação; o Flyway já criou o esquema ao subir o JAR)
docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho -v ON_ERROR_STOP=1 < docs/desempenho/carga-dataset.sql

# 5. k6 em versão fixa (nunca latest). Fases em sequência, com a aplicação já em execução.
docker pull grafana/k6:1.8.2
for FASE in aquecimento principal complementar; do
  docker run --rm -i -e FASE=$FASE -v "$PWD/target/perf:/saida" grafana/k6:1.8.2 \
    run --summary-export=/saida/resumo-$FASE.json - < docs/desempenho/k6-consultas.js
done
```

No Git Bash do Windows, prefixe o `docker run` com `MSYS_NO_PATHCONV=1` e use `$(pwd -W)` no volume. O k6 acessa a aplicação em `http://host.docker.internal:8080` (altere com `-e BASE_URL=...`). Para repetir só a fase `complementar` (por exemplo, 15 vezes), rode apenas o `docker run` dela, sempre com a aplicação subida com a variável do passo 2, e **registre todas as tentativas**.

### O que o script faz

| Fase | VUs | Volume | Conta nos resultados? |
|---|---|---|---|
| `aquecimento` | 1 | 40 iterações | **Não** (JIT, pool e caches) |
| `principal` | 1 | 200 iterações = **200 requisições de cada tipo** | Sim |
| `complementar` | 10 | 30 s | Sim (carga concorrente) |

- Cada iteração faz uma consulta RF06, uma RF09 e uma RF15 (3 requisições). Em cada tipo, a **variante** muda em rodízio e o parâmetro avança a cada volta:
  - **RF06 (5 variantes, 40 requisições cada):** município, bairro, material, filtros combinados (nome + município + bairro + material) e paginação em páginas de 5 (páginas 0 a 3 do total de 20; nunca uma página além do fim).
  - **RF09 (10 variantes, 20 cada):** ADMINISTRADOR (situação, ponto, período, página sem filtro) e, no contexto do **MORADOR** e do **OPERADOR**, sem filtro, por situação e por período.
  - **RF15 (2 variantes, 100 cada):** geral e por período.
- **Nenhum id é fixo.** O `setup()` descobre pelos endpoints os pontos (`/pontos-coleta?nome=PERF`), os materiais aceitos por eles, os moradores (`/moradores`) e os operadores (`/operadores`) do dataset `PERF`. Por isso o roteiro vale depois de limpar e recarregar a base, quando os ids mudam.
- Tags estáveis em cada requisição medida: `etapa=medicao`, `consulta=RF06|RF09|RF15`, `variante=...` e `perfil=PUBLICO|ADMINISTRADOR|MORADOR|OPERADOR`. As requisições do `setup()` levam `etapa=preparo` e ficam fora do p95.
- O `setup()` **se recusa a medir** se o banco não tiver exatamente 1.000 solicitações, 20 pontos, 200 moradores e 20 operadores `PERF` (protege contra medir o banco errado).
- Cada requisição verifica o **status 200 e o conteúdo** (lista não vazia quando o filtro garante resultado; o ponto esperado no filtro combinado; 5 itens e total 20 na paginação; o morador sem filtro vê exatamente as suas 5 e o operador as 50 do seu ponto). Uma resposta de erro rápida reprova a medição: o limiar de `checks` exige 100% e o de falhas HTTP exige 0%.
- Limiares: p95 < 2.000 ms no geral (`etapa:medicao`), em cada tipo de consulta e em **cada variante** (o limiar por variante faz o k6 exportar o p95 de cada uma). Se algum falhar, o k6 termina com código 99.
- **Instabilidade da fase `complementar` (Issue #58):** nas medições oficiais anteriores, feitas com o Tomcat no padrão (`max-keep-alive-requests=100`), 12 de 22 execuções reprovaram por `dial: i/o timeout` entre o container do k6 e o host (6 de 14 nas medições oficiais e 6 de 8 na reprodução do diagnóstico), sem erro na aplicação. O diagnóstico mostrou que a aplicação, o Tomcat e o Windows aceitam as conexões normalmente (o mesmo tráfego direto do host passou 8 de 8) e que o travamento ocorre no caminho container → Docker Desktop → host, disparado pelo ritmo de conexões novas que o limite de 100 requisições por conexão provoca. **Contorno:** subir o JAR com `SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS=-1` (passo 2). O **mecanismo interno do Docker Desktop continua não comprovado**. Detalhes, números e limites em [`relatorio-rnf04.md`](relatorio-rnf04.md) e [`evidencias-estabilidade/`](evidencias-estabilidade). Se mesmo assim ocorrer um timeout, **não descarte a tentativa**: registre-a e investigue antes de repetir.

### EXPLAIN

```bash
docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho < docs/desempenho/explain-consultas.sql
```

As instruções do script são as que o Hibernate enviou de fato ao PostgreSQL, inclusive as das variantes de material, filtros combinados, paginação e contextos de morador e operador. Para recapturá-las: `ALTER DATABASE coleta_desempenho SET log_statement='all'`, reinicie o JAR (as conexões do pool só leem a configuração ao abrir), dispare uma requisição de cada variante, leia `docker logs coleta-seletiva-db` e **desfaça** com `ALTER DATABASE coleta_desempenho RESET log_statement`. Isso é feito à parte da medição, porque o log de SQL altera os tempos.

### Limpeza

```bash
docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho -v ON_ERROR_STOP=1 < docs/desempenho/limpar-dataset.sql
# ou, para descartar tudo: docker exec coleta-seletiva-db psql -U coleta -d postgres -c "DROP DATABASE coleta_desempenho"
```

## Volume ampliado (só para avaliar índices)

A medição oficial usa exatamente 1.000 solicitações. Para justificar índices, um **banco descartável separado** recebe o mesmo dataset e depois o volume ampliado (100.000 solicitações), e os planos são comparados sem e com os índices candidatos. **Os resultados deste volume nunca se misturam com os da medição oficial.**

```bash
docker exec coleta-seletiva-db psql -U coleta -d postgres -c "CREATE DATABASE coleta_desempenho_xl"
# esquema: V1 e V2 (o Flyway cria o esquema 'coleta' quando a aplicação sobe; aqui, à mão)
(echo "CREATE SCHEMA coleta; SET search_path TO coleta;"; cat src/main/resources/db/migration/V1__*.sql src/main/resources/db/migration/V2__*.sql)   | docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho_xl
docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho_xl -v ON_ERROR_STOP=1 < docs/desempenho/carga-dataset.sql
docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho_xl -v ON_ERROR_STOP=1 < docs/desempenho/carga-volume-ampliado.sql
docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho_xl -v ON_ERROR_STOP=1 < docs/desempenho/explain-indices-v3.sql
docker exec coleta-seletiva-db psql -U coleta -d postgres -c "DROP DATABASE coleta_desempenho_xl"
```

## Comparação antes/depois da V3 (feita)

A medição depois da V3 repetiu **exatamente** estes passos (mesmo banco e dataset de 1.000 solicitações, roteiro `k6-consultas.js` sem alterações, mesma tag do k6, fases e máquina; a única diferença é a migração V3). **As duas medições oficiais foram feitas com o Tomcat no padrão, antes do contorno do passo 2**; elas permanecem como foram registradas, com todas as tentativas. A comparação e todas as tentativas estão em [`relatorio-rnf04.md`](relatorio-rnf04.md). Para repetir a medição no futuro, siga os passos acima e compare com o relatório.
