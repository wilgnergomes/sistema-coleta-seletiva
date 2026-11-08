# Relatório final do RNF04: desempenho das consultas

**Data:** 2026-11-08 · **Issues:** #58 e #33 · **Meta:** 95% das consultas em até 2.000 ms (p95), em teste local com 1.000 solicitações, registrando máquina, ferramenta e resultados.

**Conclusão: a meta é atendida com ampla folga antes e depois da V3** (pior p95 geral: 10,44 ms). A V3 criou sete índices que o PostgreSQL já passou a usar com 1.000 solicitações e que, no volume ampliado de 100.000, reduziram as consultas de milissegundos a décimos de milissegundo. **Ponta a ponta (HTTP, k6), a diferença antes/depois não é distinguível da variação entre execuções.** A instabilidade da fase complementar foi diagnosticada e contornada na Issue #58 (seção 3.1); os resultados das seções 2 a 5 foram mantidos como registrados.

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

### Instabilidade: fatos observados nas medições oficiais

- **Fatos:** nas fases complementares de todas as medições feitas com o roteiro definitivo (6 + 4 + 4 = 14 tentativas), 6 reprovaram por `dial: i/o timeout` (de 8 a 10 mensagens de timeout e de 16 a 20 verificações reprovadas por tentativa), sem nenhum erro registrado pela aplicação; as respostas recebidas foram rápidas. A fase principal (1 VU) **nunca** falhou. A tentativa 3 depois da V3 teve vazão de 428 req/s sem falhas, sem causa identificada.
- **Causa:** na época destas medições era uma hipótese não comprovada (rede do Docker Desktop, configuração do servidor ou roteiro), e nenhuma causa era atribuída. Ela foi investigada na Issue #58 e o resultado está na subseção 3.1; **os números e as tentativas desta seção permanecem como foram registrados**. A instabilidade ocorreu antes e depois da V3 e não tem relação com ela.

### 3.1 Diagnóstico e contorno da instabilidade (Issue #58)

**Data:** 2026-11-08 · **Banco:** `coleta_desempenho` recriado do zero pelo Flyway (V1, V2 e V3) e recarregado com o dataset corrigido (totais e 7 índices conferidos) · **Evidências, scripts e logs:** [`evidencias-estabilidade/`](evidencias-estabilidade).

#### O problema observado

Com o procedimento oficial (aquecimento, principal e 8 rodadas da complementar) e o Tomcat no padrão, **6 de 8 rodadas reprovaram**, e só por `dial: i/o timeout` (52 mensagens; nenhum outro tipo de erro, nenhum erro na aplicação). Nas rodadas reprovadas os 10 VUs ficaram presos ao tentar abrir uma conexão nova por cerca de 30 s: a fase durou de 36 a 61 s (o normal é ~31 s) e a vazão caiu para 118 a 974 req/s (nas aprovadas, 1.502 a 1.667 req/s). Contando as medições oficiais (6 de 14) e este diagnóstico (6 de 8 e, no Teste B, 1 de 5), 13 de 27 rodadas reprovaram. A fase principal (1 VU) nunca falhou.

#### O que foi testado, isoladamente

| Teste | O que isola | Resultado |
|---|---|---|
| **A.** Mesmo tráfego (10 threads, 30 s, as 3 consultas) feito **direto do host, sem Docker**, 8 rodadas | A aplicação, o Tomcat e o Windows | 8 de 8 sem erro; 2.752 a 2.822 req/s; 830 a 850 conexões abertas por rodada (cerca de 1 a cada 100 requisições, o limite do Tomcat); pior `connect()` de 29 ms |
| **B.** k6 via Docker, em paralelo com uma sonda no host que abre uma conexão **nova** a cada 250 ms, 5 rodadas | O host continua aceitando conexões enquanto o k6 trava? | k6: 4 aprovadas e 1 reprovada (3 timeouts). Sonda: 300 conexões por rodada, **0 erros**, pior `connect()` de 26,1 ms, **inclusive na rodada reprovada** |
| **B, log do Docker.** `gvisor/forwarder` do Docker Desktop (o componente que leva a conexão do container ao host) | Onde o pedido de conexão se perde | veja a próxima subseção |
| **C.** Aplicação com o limite de keep-alive do Tomcat removido (variável de ambiente), 10 rodadas | O ritmo de conexões novas é o gatilho? | **10 de 10 aprovadas**; 1.801 a 1.975 req/s; só 10 conexões abertas; 0 timeouts |

#### O que o log do Docker Desktop mostra

Em todas as 7 rodadas reprovadas com registro, o `gvisor/forwarder` ficou de **14,2 a 29,2 s sem registrar nenhum evento**; nas 6 rodadas aprovadas com conexões novas, o maior silêncio foi de 0,5 a 0,9 s ([`analise-forwarder-por-rodada.txt`](evidencias-estabilidade/diagnostico/analise-forwarder-por-rodada.txt)). Na rodada 4 do Teste B (trecho cru em [`docker-desktop-monitor-teste-b-rodada-4.log`](evidencias-estabilidade/diagnostico/docker-desktop-monitor-teste-b-rodada-4.log)):

- até 21:29:35,659 UTC o forwarder atendia cerca de 20 conexões novas por segundo, todas com `dial succeeded` em cerca de 1 ms;
- depois, **29,2 s sem nenhum evento**; as conexões pendentes só aparecem às 21:30:04,8, no mesmo segundo dos 3 timeouts do k6;
- **nunca** houve um `dial` falho ou lento ao host, e o `apiproxy` (API do Docker) seguiu ativo no intervalo (90 eventos), portanto a VM não congelou.

O silêncio é um **marcador** da falha, não uma causa independente: com os VUs presos não há mais tráfego novo. O que ele mostra, junto do Teste B, é que o pedido de conexão **não chegou a ser discado ao host**, enquanto o host aceitava conexões normalmente.

#### O contorno aplicado

- **Gatilho.** O Tomcat fecha cada conexão depois de 100 requisições (`max-keep-alive-requests=100`, padrão desta versão do Spring Boot/Tomcat). A cerca de 1.900 req/s são cerca de 20 conexões novas por segundo atravessando o Docker Desktop. Sem o limite, a fase abre só 10 conexões.
- **Aplicado:** subir o JAR da medição com `SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS=-1`, documentado no passo 2 do [README](README.md). Isso **altera a configuração de keep-alive do processo da aplicação durante a validação** (e durante qualquer medição feita por esse procedimento), por variável de ambiente. **Não modifica** o roteiro `k6-consultas.js`, o dataset, os endpoints nem os resultados históricos das medições oficiais. Também não foram alterados `application.properties`, o código Java e as migrations; fora do procedimento de medição, a aplicação mantém o padrão do Tomcat.
- O contorno **evita o gatilho; não corrige o Docker Desktop**.

#### Validação do contorno: 15 rodadas pelo procedimento do README

Aquecimento, principal e 15 rodadas da fase complementar (10 VUs, 30 s), todas registradas; o executor pararia na primeira reprovada, e isso não ocorreu. A aplicação não registrou nenhum `ERROR` (só 3 avisos de configuração na inicialização) e o Flyway validou as 4 entradas do histórico, o que confirma que a divergência de checksum da V3 foi resolvida.

| Rodada | Resultado | Requisições (req/s) | Falhas HTTP | Verificações falhas | p95 RF06 (ms) | p95 RF09 (ms) | p95 RF15 (ms) | p95 geral (ms) | Pior variante (ms) |
|---|---|---|---|---|---|---|---|---|---|
| 1 | aprovada | 49.202 (1.638) | 0 | 0 | 9,17 | 11,23 | 9,10 | **9,89** | 15,50 |
| 2 | aprovada | 60.467 (2.013) | 0 | 0 | 5,92 | 7,95 | 6,09 | **6,85** | 10,25 |
| 3 | aprovada | 59.042 (1.966) | 0 | 0 | 6,17 | 8,17 | 6,34 | **7,13** | 10,56 |
| 4 | aprovada | 60.659 (2.020) | 0 | 0 | 5,89 | 7,89 | 5,99 | **6,81** | 10,32 |
| 5 | aprovada | 59.393 (1.978) | 0 | 0 | 6,08 | 8,01 | 6,30 | **7,00** | 10,25 |
| 6 | aprovada | 57.194 (1.904) | 0 | 0 | 6,35 | 8,35 | 6,60 | **7,26** | 10,69 |
| 7 | aprovada | 56.621 (1.885) | 0 | 0 | 6,45 | 8,38 | 6,72 | **7,35** | 10,68 |
| 8 | aprovada | 55.952 (1.863) | 0 | 0 | 6,56 | 8,59 | 6,90 | **7,55** | 11,02 |
| 9 | aprovada | 54.437 (1.812) | 0 | 0 | 6,80 | 8,87 | 7,18 | **7,79** | 11,51 |
| 10 | aprovada | 55.925 (1.862) | 0 | 0 | 6,52 | 8,57 | 6,85 | **7,49** | 10,92 |
| 11 | aprovada | 58.322 (1.942) | 0 | 0 | 6,26 | 8,20 | 6,53 | **7,23** | 10,72 |
| 12 | aprovada | 59.159 (1.970) | 0 | 0 | 6,05 | 8,08 | 6,24 | **7,02** | 10,29 |
| 13 | aprovada | 59.387 (1.977) | 0 | 0 | 6,00 | 8,01 | 6,17 | **6,92** | 10,06 |
| 14 | aprovada | 58.781 (1.957) | 0 | 0 | 6,15 | 8,07 | 6,30 | **7,03** | 10,49 |
| 15 | aprovada | 57.938 (1.929) | 0 | 0 | 6,27 | 8,25 | 6,56 | **7,22** | 10,68 |

Total: 862.479 requisições, 0 falhas HTTP, 0 verificações reprovadas, 0 timeouts. p95 geral de 6,81 a 9,89 ms (a rodada 1, a primeira fase concorrente da sequência, foi a mais lenta, com menos requisições) e vazão de 1.638 a 2.020 req/s. Somadas ao Teste C, são **25 rodadas seguidas sem nenhuma falha**, contra 13 reprovações em 27 rodadas sem o contorno (um cálculo ilustrativo, que supõe rodadas independentes, dá menos de 1 chance em 10 milhões de isso ocorrer por acaso com a taxa observada).

#### O que as evidências mostram e o que não está comprovado

- **O que as evidências mostram** (indícios fortes, não uma prova definitiva): (1) com o mesmo tráfego, a aplicação, o Tomcat, o banco e o Windows funcionaram sem falhas no Teste A, e a sonda do Teste B abriu conexões normalmente no host durante a rodada em que o k6 travou; isso torna essas causas improváveis neste ambiente, mas **não as exclui em definitivo**; (2) o roteiro k6, inalterado, passou em 25 de 25 rodadas quando só o ritmo de conexões novas mudou, o que indica que ele não é o fator determinante, também sem excluí-lo em definitivo; (3) no log do Docker Desktop, o pedido de conexão não aparece sendo discado ao host durante o travamento. Em conjunto, as evidências **localizam a falha no caminho de rede do Docker Desktop** (container → Docker Desktop → host), **sem comprovar o mecanismo interno** desse componente; (4) o gatilho que as evidências apontam é o ritmo de conexões novas causado pelo limite de keep-alive do Tomcat: removê-lo eliminou as falhas em 25 rodadas.
- **NÃO comprovado:** o **mecanismo interno do Docker Desktop** (o forwarder do Docker Desktop é código fechado; o projeto aberto gvisor-tap-vsock, cujo forwarder tem o mesmo nome, usa, segundo seu código público, até 128 conexões em voo e 30 s de limite no `dial` ao host, o que não explica o travamento sozinho; a relação entre os dois não foi verificada). Também não se sabe por que o travamento começa em instantes diferentes da fase, nem se uma falha rara ainda poderia ocorrer sem as conexões novas (foram 25 rodadas, cerca de 12 min de carga), nem se o comportamento vale para outras versões do Docker Desktop (testado: 29.8.1, backend WSL2, kernel 6.18.54.1-microsoft-standard-WSL2).

#### Efeito sobre as medições oficiais

**Nenhuma medição oficial foi refeita, substituída ou recalculada.** Elas foram feitas com o Tomcat no padrão, antes do contorno, e continuam valendo com todas as tentativas, inclusive as reprovadas. O contorno não muda o que é medido: o p95 geral desta validação (6,81 a 9,89 ms) está na faixa das tentativas anteriores (5,33 a 10,44 ms). A conclusão do RNF04 não depende dele.

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
- A fase complementar de 10 VUs só ficou estável com o contorno da seção 3.1 (keep-alive do Tomcat sem limite, no procedimento de medição). O mecanismo interno do Docker Desktop segue **não comprovado**.
