# Desempenho (RNF04) — dataset e método de medição

O RNF04 pede que a pesquisa de pontos (RF06), a listagem de solicitações (RF09) e o resumo gerencial (RF15) respondam em até **2 segundos (p95)** com **1.000 solicitações** cadastradas. Esta pasta reúne tudo o que é preciso para repetir a medição, antes e depois dos índices da V3 (Issue #33), sempre com o **mesmo dataset, os mesmos parâmetros e o mesmo método**.

| Arquivo | Para quê |
|---|---|
| [`carga-dataset.sql`](carga-dataset.sql) | Carrega o dataset `PERF` (determinístico, em uma transação) |
| [`limpar-dataset.sql`](limpar-dataset.sql) | Remove somente os registros `PERF` |
| [`k6-consultas.js`](k6-consultas.js) | Script do k6: fases `aquecimento`, `principal` e `complementar` |
| [`explain-consultas.sql`](explain-consultas.sql) | `EXPLAIN (ANALYZE, BUFFERS)` das consultas reais |
| [`resultados-iniciais.md`](resultados-iniciais.md) | Resultados da medição inicial (sem os índices da V3) |
| [`evidencias/`](evidencias) | Saídas brutas do roteiro definitivo: planos, resumos do k6 (inclusive todas as tentativas da fase complementar) e SQL capturado. `evidencias/historico-roteiro-v1/` guarda a primeira versão do roteiro, só como histórico |

Nada disso roda sozinho: o dataset **não** faz parte do seed `demo` e a aplicação nunca o carrega. Os testes de integração (`DatasetDesempenhoIntegracaoTest`) o carregam em um PostgreSQL descartável do Testcontainers.

## O dataset

- **1.000 solicitações** criadas ao longo de 12 meses (nov/2025 a out/2026, fuso America/Sao_Paulo), com 2 a 4 itens cada (3.000 itens) e 2.850 linhas de histórico.
- **200 moradores** (um endereço cada), **20 pontos** ativos em 8 municípios e 5 bairros, **8 materiais** (KG, L e UN), **101 vínculos** (4 a 6 materiais por ponto) e **20 operadores** (um por ponto).
- Situações: CONCLUIDA 450, SOLICITADA 150, AGENDADA 150, RECUSADA 100, CANCELADA 100, EM_RECOLHIMENTO 50.
- **Determinístico**: não usa `random()`. Cada carga gera exatamente os mesmos dados, o que torna os números comparáveis entre medições.
- Respeita as regras do sistema (RN01, RN03, ADRs 0008, 0010 e 0011): ponto ativo e domiciliar, materiais aceitos pelo ponto, limites do vínculo, unidade e endereço copiados, histórico coerente com a situação atual e quantidade efetiva só nas concluídas.
- Marcador **PERF**: e-mails `perf.morador.NNN@exemplo.com` e `perf.operador.NN@exemplo.com`, e nomes `PERF ...`. A limpeza e a guarda contra carga repetida usam esse marcador.

A carga recusa uma segunda execução (`O dataset de desempenho já foi carregado...`). Para repetir, rode antes a limpeza. A limpeza apaga na ordem inversa das chaves estrangeiras e preserva qualquer outro dado.

## Como medir (passo a passo)

Pré-requisitos: container `coleta-seletiva-db` em execução, Docker, JDK 21. **Use um banco dedicado**, sem tocar no de desenvolvimento.

```bash
# 1. Banco dedicado, no container que já existe (não recria o container)
docker exec coleta-seletiva-db psql -U coleta -d postgres -c "CREATE DATABASE coleta_desempenho"

# 2. Empacota e sobe o JAR apontando para o banco dedicado (sem DevTools, sem log SQL, sem perfil demo).
#    DB_NAME no ambiente tem precedência sobre o .env, que continua intacto.
./mvnw -DskipTests package
DB_NAME=coleta_desempenho java -jar target/coleta-seletiva-0.0.1-SNAPSHOT.jar

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

No Git Bash do Windows, prefixe o `docker run` com `MSYS_NO_PATHCONV=1` e use `$(pwd -W)` no volume. O k6 acessa a aplicação em `http://host.docker.internal:8080` (altere com `-e BASE_URL=...`).

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
- **Instabilidade observada na fase `complementar`:** em 3 de 6 execuções houve `dial: i/o timeout` entre o container do k6 e o host, sem erro registrado na aplicação. A causa é hipótese não comprovada (veja `resultados-iniciais.md`). Se ocorrer, repita a fase e **registre todas as tentativas**.

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

## Comparação futura (V3)

Depois da migração V3, repita **exatamente** os passos acima com o roteiro definitivo (`k6-consultas.js` sem alterações, mesmo dataset, tag do k6, fases, parâmetros e máquina, documentando qualquer diferença) e compare com [`resultados-iniciais.md`](resultados-iniciais.md). O relatório final do RNF04 fecha a Issue #58.
