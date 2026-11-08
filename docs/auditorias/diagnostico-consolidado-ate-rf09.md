# Diagnóstico consolidado: auditoria geral até o RF09

**Data:** 2026-11-08 · **Base:** `main` limpa, commit `eab0a46` (285 testes, 0 falhas no último `clean verify`) · **Autor:** Claude principal, a partir dos relatórios [revisao-tecnica-ate-rf09.md](revisao-tecnica-ate-rf09.md) (agente `revisor-tecnico`) e [revisao-arquitetural-ate-rf09.md](revisao-arquitetural-ate-rf09.md) (agente `arquiteto`).

Esta é uma auditoria, não uma implementação. Nenhum código, teste, ADR ou documento existente foi alterado, e nenhuma issue foi criada. Os três relatórios desta pasta são o único acréscimo.

## Como a auditoria foi feita

- **Fonte de verdade:** o documento do professor (`docs/referencias/sistema_de_coleta_seletiva.md`), lido integralmente antes de delegar, cruzado com matriz, issues, ADRs, OpenAPI, DER/V1, código, testes e README.
- **Agentes:** foram conferidas as definições em `.claude/agents`. `revisor-tecnico` e `arquiteto` são somente leitura (sem `Write`/`Edit`), por isso **não gravam arquivos**: devolveram os relatórios ao Claude principal, que os gravou. Também não rodam testes (o `Bash` é só de leitura). O `investigador` não foi usado, porque nenhuma consulta pontual justificou (as verificações pontuais foram feitas pelo Claude principal). Nenhum agente novo foi criado.
- **Modelo:** foi tentado o Fable 5.1, o de maior capacidade, mas ele exige créditos de uso e as duas execuções falharam (HTTP 429) sem produzir nada. Os agentes foram relançados com **Opus, esforço alto**. O Claude principal permaneceu no Sonnet.
- **Verificação independente do Claude principal:** os achados mais impactantes foram conferidos no código e nos documentos, e alguns foram rebaixados ou refinados (seção "Divergências entre os agentes").

---

## 1. Avaliação geral da aderência ao documento do professor

**Aderência alta no escopo entregue (RF01 a RF09), sem nenhum achado Bloqueante.**

- Todos os critérios de aceite do enunciado para RF01 a RF09 estão implementados, e quase todos têm teste que os prova (os dois agentes chegaram à mesma conclusão). Isso inclui os exigidos pelo RNF08: compatibilidade, limites, quantidades, rollback na criação e concorrência da correção.
- Camadas do RNF01, contrato e erros do RNF03, `timestamptz` e restrições do RNF05, e ausência de dados pessoais em consultas públicas (RNF06) estão atendidos.
- Não há falha material de autorização ou de isolamento entre atores nos 16 grupos de endpoints analisados (matriz endpoints × atores no relatório técnico, seção 4).
- **Parciais por planejamento (previstos para ciclos futuros):** RN05 (bloqueios retroativos), RN03/RN06 nas transições, RF07 (aceite do operador de outro ponto), RNF04 (índices e medição), RNF08 nas transições, seção 11 (interfaces) e RNF02 (2ª unidade).
- **Entregáveis da seção 14 ainda inexistentes, o que é esperado nesta fase:** frontend, relatório final (modelo de dados, decisões e evidências dos testes), relatório do RNF04 (máquina, ferramenta e resultados) e evidências consolidadas dos testes.

## 2. Quantidade de achados por prioridade (consolidados, sem duplicações)

| Prioridade | Quantidade | Observação |
|---|---|---|
| Bloqueante | **0** | Nenhum defeito comprovado impede o RF10 ou compromete integridade ou segurança lógica |
| Importante | **11** | 3 exigem decisão sua, 1 é correção de código pequena, o restante é documental, de teste ou de planejamento |
| Opcional | **15** | Qualidade, organização e documentação |
| Hipóteses (não confirmadas) | **11** | Dependem de verificação ou de confirmação do professor |

Os 52 itens originais (arquiteto: 23 achados e 9 hipóteses; revisor: 15 achados e 5 hipóteses) foram reduzidos por fusão de duplicatas. A tabela de correspondência está na seção 9.

## 3. Problemas que exigem ação ANTES do RF10

Nenhum bloqueia tecnicamente. Mas **três decisões e uma correção pequena** são muito mais baratas agora do que depois, porque as sete ações dos RF10 a RF14 herdam o comportamento:

| ID | Tipo | O que precisa acontecer | Por que antes do RF10 |
|---|---|---|---|
| **C-02** | Decisão sua | Política para solicitação fora do contexto do ator nas ações: 404 (como o `GET /solicitacoes/{id}`) ou 403 para operador de outro ponto | Convenção transversal que as 7 ações vão repetir. O ADR 0019 pede o teste do operador de outro ponto (RF07) |
| **C-03** | Decisão sua | Se a resposta da solicitação passa a trazer dados de exibição: `nomePonto`, nome do responsável no histórico e, para operador e administrador, nome e telefone do morador | As ações dos RF10 a RF14 devolvem `SolicitacaoDetalhe`. Mudar depois altera 7 respostas. Pelo ADR 0017, contrato e matriz mudam juntos. Também é pré-requisito do front |
| **C-01** | Decisão sua | Quando fazer a RN05 e o que são solicitações "abertas" | Há solicitações órfãs possíveis desde o RF08. Os dois agentes divergem sobre o momento (seção 8) |
| **C-04** | Correção de código | `itens: [null]` gera 500; o ajuste é `@NotNull` no elemento da lista, mais chave pt/en e teste | Defeito real de RNF03. O RF13 repetirá o padrão |

Também é barato e útil fazer agora, mas é opcional para destravar: C-07/C-08 (PR de documentação e organização das issues), para a matriz e o README não induzirem ao erro durante o RF10.

**Resposta curta à pergunta final:** podemos continuar, mas recomendo uma etapa curta de ajustes antes do RF10 (seção 6), não por haver defeito grave, e sim porque três decisões afetam todas as ações do RF10 em diante.

## 4. Achados Importantes (consolidados)

### C-01. RN05 obrigatória, não implementada e sem issue dedicada
- **Evidência:** `PontoColetaService.java:79` e `MaterialAceitoService.java:50-57` só têm comentários ("entra no ciclo das solicitações"); `openapi.yaml:428-430` e `:484` já prometem o 409; a matriz o põe no ciclo 6 (matriz:55 e :101) e a Issue #4 remete aos "ciclos posteriores". Verificado pelo Claude principal: **não é esquecimento**, está planejada e a matriz do RF05 (:75) já diz "o 409 ainda não existe"; falta issue própria e definição de "aberta".
- **Origem:** professor (RN05, obrigatória, seção 5) + planejamento da equipe (adiamento).
- **Impacto:** o administrador pode inativar um ponto, ou remover um vínculo, com solicitações em SOLICITADA, AGENDADA ou EM_RECOLHIMENTO afetadas. O comentário do código ("ciclo das solicitações") não bate com o ciclo 6 da matriz.
- **Recomendação mínima:** criar a issue; definir "aberta" (sugestão dos agentes: SOLICITADA, AGENDADA e EM_RECOLHIMENTO; no vínculo, só as com item daquele material); alinhar comentário e matriz.
- **Quando:** criar a issue e decidir antes do RF10; implementar após o RF12 (revisor) ou no ciclo 6 (matriz atual). Decisão sua.

### C-02. Política 403 × 404 fora do contexto do ator sem registro em ADR
- **Evidência:** ADR 0019:19 diz "perfil sem permissão → 403"; matriz:72 e :78 e o código (SolicitacaoService:144-146, :186-187, :297-300) usam 404 "fora do contexto"; a matriz não define o operador de outro ponto nas ações (:35-39).
- **Impacto:** risco de ações inconsistentes. **Recomendação:** decidir e registrar (ADR complementar aos 0018/0019). O arquiteto sugere 404 por coerência com o GET.
- **Quando:** no planejamento do RF10.

### C-03. A solicitação não traz dados de exibição
- **Evidência:** schemas e DTOs só têm ids (openapi:1261-1296; `SolicitacaoResumoDTO`; `ResponsavelDTO`). Ponto inativo é 404 para não administradores; `GET /operadores` é só do administrador; o operador não lê dados do morador (`MoradorService.java:104`, verificado).
- **Origem:** seção 11 do professor (lista e detalhe com histórico, painel do operador), para a 2ª unidade.
- **Impacto:** o front não monta telas obrigatórias sem mudar o contrato.
- **Recomendação:** decisão sua. Os dois agentes e o Claude principal concordam que é mais barato antes do RF10.

### C-04. `itens: [null]` gera 500 em vez de 400
- **Evidência:** `SolicitacaoForm.java:21-22` (`List<@Valid ItemForm>` sem `@NotNull` no elemento) e `SolicitacaoService.validarEMontarItens` chamando `item.materialId()`. **Verificado pelo Claude principal por leitura do código** (a execução do cenário não foi feita). Nenhum teste usa `[null]`.
- **Origem:** RNF03. **Recomendação:** `List<@NotNull(message="{…}") @Valid ItemForm>`, chave pt/en e teste `[API]`; vale também para o form de correção.
- **Quando:** antes do RF10.

### C-05. RNF04 pede "índices coerentes com os filtros", e a V1 não os tem; a Issue #33 está incompleta
- **Evidência:** V1 só tem PKs, UNIQUEs e `idx_ope_poc` (V1:208). Filtros do RF09 em `poc_nr_id`, `mor_nr_id`, `sol_tx_situacao`, `sol_dt_criacao`; do RF06 em `lower(municipio)`, `lower(bairro)` e `mac.mat_nr_id`. A #33 só lista `iso(mat)`, `his(sol, data)` e `mac(mat)`. A ampliação do endereço para 500 toca cinco pontos (V1:220, `Solicitacao.java:37`, `SolicitacaoService:71`, openapi:1290, `der-transacional.dbml:44`) que a #33 não enumera. A nota do DER sobre o ADR 0013 também está pendente (der-estrutural.dbml:47).
- **Impacto:** exigência explícita do professor sem evidência; risco de a V2 deixar contrato e entidade em 400.
- **Recomendação:** ampliar o escopo da #33 (candidatos `sol(poc, criação)`, `sol(mor, criação)`, `his(sol)`, com motivação e `EXPLAIN`). Já está aprovada a ampliação para 500.
- **Quando:** junto da #33, antes da medição do ciclo 6. A V2 pode sair antes.

### C-06. ADR 0018 desatualizado e contradito pelo código (405/415)
- **Evidência:** ADR 0018:45 diz "o ADR 0014 continua Proposta" (**verificado**: está Aceita). O tratador (`TratadorGlobalExceptions:171-181`) trata 405/415 com `VALIDACAO`, fora do catálogo do ADR e do contrato, contra a regra do próprio ADR (catálogo, contrato e matriz no mesmo PR). Issue #31.
- **Recomendação:** decisão sua entre documentar 405/415 reaproveitando `VALIDACAO` ou criar códigos próprios. Como o ADR é aceito, a mudança vem por ADR complementar ou revisão aprovada.
- **Quando:** antes do RF10, ou junto da #31.

### C-07. Matriz, README e comentários desatualizados
- **Evidência (verificada em parte):** matriz:28-29 e :97-99 ainda listam pendências já cumpridas pelo RF08; matriz:69-70 mantém "A confirmar" para decisões do ADR 0018 Aceita; matriz:85 e :88 contradizem :63-64; matriz:108-115 prevê `[UNIT]` mas só há `[INT]`; README:170 omite o seed 070, README:154 descreve errado a dependência dos testes, README:98, :123, :285 e :319-320 com placeholders; comentários obsoletos em `MaterialRepository`, `MaterialAceitoDTO`, `PontoColetaService:79`, `MaterialAceitoService:50`.
- **Impacto:** a matriz serve à arguição e hoje afirma coisas incorretas.
- **Recomendação:** um PR de documentação curto. Mudanças em ADR aceito exigem sua aprovação.
- **Quando:** antes do RF10 (barato) e de novo antes da entrega.

### C-08. Issues de housekeeping cumpridas ainda abertas; milestones
- **Evidência:** #29 e #30 têm o trabalho feito; #31 e #32 parciais; #17 lista ADRs só até o 0012; #10 a #15 sem milestone e sem Ciclo 4 a 6; #29 a #32 no Ciclo 1.
- **Recomendação:** você confere e fecha a #29 e #30, reduz o escopo da #31 e #32 e atualiza a #17. Criar os milestones. Não criei nada.
- **Quando:** antes do RF10.

### C-09. Materiais inativos aparecem como "aceitos" na consulta pública de pontos
- **Evidência (verificada):** `vinculoMapper::paraDTO` e `aceitaMaterial` não consideram a situação do material. `GET /materiais/{id}` desse material dá 404 ao público e a RN01 recusa o pedido.
- **Impacto:** o morador vê "Isopor" como aceito, escolhe o ponto e recebe 409.
- **Recomendação:** decisão sua. Para não administradores, filtrar material ATIVO nas duas pontas, ou registrar a decisão contrária na matriz.
- **Quando:** antes da 2ª unidade; pode ser antes do RF10.

### C-10. RN03 sem prova com solicitação e constraints do fluxo sem teste de banco
- **Evidência:** nenhum teste de `fk_sol_*`, `fk_iso_*`, `fk_his_*`, `ck_his_responsavel`, `uq_sol_protocolo`; matriz:102 admite a lacuna.
- **Recomendação:** um teste `[INT]` barato: criar solicitação pela API, tentar apagar morador, ponto e material por SQL (espera violação de FK), e inserções inválidas em `his_historico`.
- **Quando:** antes do RF10 ou junto dele.

### C-11. RF15: "período" como agrupamento ou filtro (futuro)
- **Evidência:** matriz:85 diz "agrupar por período"; o contrato só tem `de`/`ate` (openapi:880-884, 1375-1385). Relacionada à H-H.
- **Recomendação:** decidir no planejamento do RF15. Não é para agora.

## 5. Achados Opcionais (resumo)

| ID | Resumo | Origem |
|---|---|---|
| O-01 | O seed não reproduz o "vidro no ponto anterior" (os pontos sem vidro não têm recolhimento domiciliar). **Rebaixado:** o cenário 1 da seção 13 manda *cadastrar* o ponto na hora (papel e plástico), então a demonstração não depende do seed | RT-03 |
| O-02 | O seed recria dados apagados no reinício (vínculo removido volta). **Rebaixado:** o ADR 0022 trata o "recompõe a cada início" como intencional; documentar no README como demonstrar a persistência | RT-04 |
| O-03 | Data do protocolo usa o fuso da JVM, não o `FusoNegocio`; considerar `Clock` injetável para o RF10/RF14 ("data futura") | RT-11, RA-18 |
| O-04 | Limite fracionário aceito para material em UN (RN02) | RT-09 |
| O-05 | `PUT /pontos-coleta` sem `recolhimentoDomiciliar` zera o recolhimento (**verificado** em `PontoColetaMapper:30`) | RT-10 |
| O-06 | 403 em endpoints públicos (perfil inválido) não está no contrato | RT-12 |
| O-07 | Testes do detalhe do RF09 não conferem `agendadaPara`, responsável OPERADOR, `motivo` e offset | RT-14 |
| O-08 | `GET /moradores/{id}/enderecos` sem paginação (RNF04 literal) | RT-15, H-05 |
| O-09 | Textos desatualizados em ADR 0021 (trecho "Cuidados"), 0003/0004 (nomes de arquivo do DER) e 0023 (contagem de exceções); desvios de nome no ADR 0020; a edição exige aprovação | RA-08, RA-09, RA-20 |
| O-10 | Banco de testes (Testcontainers) sem ADR, pedido no ADR 0015 | RA-17 |
| O-11 | Teste de cada valor de enum no banco (ADR 0012), a conferir no fim do ciclo 5 | RA-19 |
| O-12 | Template de PR omite `openapi.yaml` (ADR 0017) e `messages*.properties` (ADR 0023) | RA-22 |
| O-13 | Swagger gerado só mostra respostas 200 (já em melhorias-pendentes) | RA-23 |
| O-14 | Redundâncias documentais (pendências do banco em 4 lugares; contagens de testes na matriz; lista de seeds; "Decisões a confirmar" misturada) e `melhorias-pendentes.md` repete a #33 | RA-16, arquiteto §8 |
| O-15 | Riscos técnicos de menor porte: `open-in-view` ativo, `MensagensParidadeTest` não confere se as chaves usadas existem | Revisor §8 |

## 6. Recomendações práticas, em ordem de prioridade

1. **Decidir C-02, C-03 e C-01** (política 403/404, dados de exibição, momento e definição da RN05). Sem nova implementação; são decisões para o plano do RF10.
2. **Corrigir C-04** (`itens: [null]` → 400), com teste e chaves pt/en, em uma correção pequena.
3. **Um PR curto de documentação** (C-06, C-07, C-08): atualizar matriz e README, corrigir o ADR 0018:45 (com sua aprovação, por ser ADR aceito) e decidir 405/415, e arrumar as issues #29 a #32 e #17. Criar milestones.
4. **Teste de banco da RN03 com solicitação** (C-10), barato, que também prepara o RF10.
5. **Ampliar a #33** (C-05) com os índices e os cinco pontos da cópia de endereço. Pode ser a V2 antes do ciclo 6.
6. **Decidir C-09** (material inativo exibido como aceito).
7. Deixar para depois: O-01 a O-15, na ordem em que cada um for tocado. O-03 (relógio) convém junto do RF10.

Nada acima é obrigatório para continuar tecnicamente. Os itens 1 e 2 são os que realmente economizam retrabalho.

## 7. Melhorias documentais que podem esperar

- Os documentos "extras" do arquiteto, com conclusão do Claude principal:

| Artefato | Utilidade concreta | Obrigatório? | Quando |
|---|---|---|---|
| Relatório (modelo de dados, decisões, evidências dos testes) | Avaliado em "Requisitos e modelagem" (15%) e "Testes" (5%); sai do DER, ADRs e matriz | **Sim**, seção 14 | Depois dos RFs; um esqueleto com links pode existir antes |
| Relatório de desempenho do RNF04 | Prova da meta de 95% em até 2 s | **Sim**, RNF04 ("máquina, ferramenta e resultados") | Ciclo 6, depois do RF15 |
| Diagrama ER | Já existe | **Sim**, seção 10 | Atualizar com a V2 (#33) |
| Roteiro dos 8 cenários de aceite | Demonstração (10%) | A execução é obrigatória (seção 13); o roteiro, não | Incremental, completo antes da entrega |
| Protótipos de tela | Citados na Etapa 1 da seção 14 | Citado no enunciado (H-F) | Antes da 2ª unidade |
| Matriz de permissões ator × operação | Hoje espalhada em 8 linhas da matriz | Não | Pode ir dentro da própria matriz antes do RF10 (já existe uma base no relatório técnico, seção 4) |
| Diagrama de estados da solicitação | Só visual; o enunciado já traz as transições | Não | Opcional, dentro do relatório (Mermaid) |
| Casos de uso e fluxos | O enunciado já tem atores, fluxo principal, aceites e cenários | **Não** | **Não recomendado** |
| Diagramas de sequência | Poderia explicar a transação do RF08 na arguição | Não | No máximo um, no relatório |

- **Conclusão:** não há necessidade de casos de uso. Os documentos exigidos pelo professor que ainda faltam (relatório final, relatório do RNF04, evidências de testes) só podem ser produzidos depois dos RFs.
- Outras melhorias (redundâncias): listadas em O-14.

## 8. Divergências entre os agentes e como foram tratadas

| Assunto | Revisor | Arquiteto | Decisão do Claude principal |
|---|---|---|---|
| Momento da RN05 | Junto do RF12 (ciclo 4), "não deixar para o ciclo 6" | Criar a issue antes do RF10 e implementar logo depois do RF12 | Concordam em antecipar; a matriz diz ciclo 6. **Registrado como decisão sua (C-01)**, não é defeito |
| RN05 "sem Issue" | Reconhece planejamento (matriz, Issue #4) | Diz que não há issue | **Verificado:** está planejada; só falta issue dedicada. Classificado como Importante pelo risco de órfãos, não como esquecimento |
| Seed e cenário "Material incompatível" | Importante | Não cita | **Rebaixado a Opcional (O-01):** o cenário 1 manda cadastrar o ponto na demonstração. O seed continua servindo para demonstrar o resto |
| Seed recria dados apagados | Importante (altera consequência do ADR 0022) | Cita só a data fixa do seed (H-07) | **Opcional (O-02):** o ADR 0022 trata como intencional; só vale documentar |
| `itens: [null]` → 500 | Confirmado | Não cita | **Confirmado por leitura do código** (não executado). Importante pela clareza e custo baixo |
| Material inativo exibido como aceito | Importante | Não cita | **Confirmado no código.** Importante, exige sua decisão |
| Dados de exibição no contrato; política 403/404 | Não cita | Importante | **Aceitos como Importante:** pesam em todas as ações do RF10 em diante. Decisões suas |
| Índices | Importante (RT-05) | Importante (RA-05, RA-06, RA-10) | Fundidos em C-05 |
| Documentação desatualizada | Opcional (RT-13) | Importante (RA-02, RA-03, RA-07) | **Importante (C-07):** afeta a arguição |
| Mecanismo único de transição e `OPTIMISTIC_FORCE_INCREMENT` | Risco para o RF10 | Risco para o RF10 | Coincidem; ver seção 10. É risco de projeto, não defeito atual |

## 9. Correspondência entre IDs

| Consolidado | Revisor técnico | Arquiteto |
|---|---|---|
| C-01 | RT-01 | RA-13, H-09 |
| C-02 | — | RA-12 |
| C-03 | — | RA-21 |
| C-04 | RT-02 | — |
| C-05 | RT-05, RT-07 | RA-05, RA-06, RA-10 |
| C-06 | parte de RT-13 | RA-01 |
| C-07 | RT-13 | RA-02, RA-03, RA-04, RA-07 |
| C-08 | parte de RT-13 | RA-11, RA-14 |
| C-09 | RT-06 | — |
| C-10 | RT-08 | RA-03 (parte) |
| C-11 | — | RA-15 |
| O-01 a O-15 | RT-03, RT-04, RT-09 a RT-12, RT-14, RT-15 | RA-08, RA-09, RA-16 a RA-20, RA-22, RA-23 |

## 10. Riscos para os próximos ciclos

1. **Mecanismo de transição (RF10, base dos RF11 a RF14).** Um único método transacional (validar ator e contexto → versão → transição no enum → aplicar → histórico), reutilizado nas 7 ações. Nas transições a situação sempre muda e o `@Version` incrementa sozinho; o truque `OPTIMISTIC_FORCE_INCREMENT` da correção não deve ser replicado. Os ADRs 0009, 0011 e 0012 já apontam nessa direção.
2. **Tempo ("data futura", RF10/RF14).** O código usa `OffsetDateTime.now()` direto; um `Clock` injetável (com `FusoNegocio`) torna as bordas testáveis (O-03).
3. **RN05 e solicitações órfãs** (C-01): decidir se uma solicitação aberta de ponto inativo ou com vínculo removido pode ser agendada, iniciada ou concluída.
4. **Operador de ponto inativo:** hoje pode consultar (ADR 0019 e teste do RF09); decidir se também pode agir (agendar, concluir).
5. **RF13:** validar inteiro em UN pela `unidadeCopia` do item, não pelo material (fonte histórica, ADR 0008); preencher `sol_dt_conclusao` (o banco não impede CONCLUIDA sem ela); reaplicar o padrão de `@NotNull` nos elementos de lista.
6. **RF15:** mais seeds de concluídas em KG e UN e em períodos diferentes (hoje só existe uma); o seed só insere (ADR 0022), então dados novos usam protocolos novos; índices em `sol_dt_conclusao`, `sol_tx_situacao` e `iso(mat)`.
7. **RNF04:** índices (C-05), dataset de 1.000 solicitações separado do seed, escolha da ferramenta de medição (ciclo 6).
8. **Front (2ª unidade):** ainda **sem decisão registrada** sobre hospedagem do front, CORS (não configurado; precisaria liberar `X-Perfil`, `X-Usuario-Id`, `Accept-Language` e expor `Location`) e a "seleção de usuário" da UI. O operador não descobre o próprio ponto pela API, e os 409 da RN01/RN02 vêm sem `campo` (RNF02 pede erros próximos aos campos).
9. **i18n:** risco baixo; considerar um teste que confira se as chaves usadas existem.
10. **Migração V1:** o nome tem erro de digitação (`criar_esquema_incial`). **Não renomear**: o Flyway valida a descrição de migrações aplicadas.

## 11. Hipóteses a verificar (não confirmadas)

| ID | Hipótese | Como verificar | Quando |
|---|---|---|---|
| H-A | Uma correção que só altera itens (mesmo endereço e observações) pode subir a versão em 2 (revisor H-01). Os testes não leem `sol_nr_versao` no banco | Teste `[INT]` com `PUT` sem mudar endereço e observações; conferir o banco e `$.versao` | Antes ou junto do RF10 |
| H-B | No caminho do incremento forçado, o conflito só aparece no commit; pode virar 500 em vez de 409 (revisor H-02) | Repetir o teste concorrente mudando só quantidades | Idem |
| H-C | `criadaEm` pode ter representação diferente no `POST` e no `GET` (revisor H-03) | Criar e consultar; comparar as strings | Junto do RF10 |
| H-D | O professor pode ler a RN03 ("cada alteração") de forma ampla e esperar histórico também na correção da solicitação, hoje sem histórico (decisão da Issue #8) | Confirmar com o professor ou registrar a justificativa na matriz e no relatório | Antes do RF10 |
| H-E | "Histórico de participação" (seção 14): verificado, os 126 commits são de uma única pessoa em duas identidades de nome (mesmo e-mail); README:319-320 tem a equipe como placeholder | Confirmar a composição da equipe; opcional `.mailmap` | Já |
| H-F | Os "protótipos" da Etapa 1 não estão no repositório | Perguntar ao professor se já foram entregues | Antes da 2ª unidade |
| H-G | O README:154 está errado e os testes rodam só com Docker, sem `.env` | `mvnw clean verify` em clone limpo | Antes da entrega |
| H-H | RF15: "apenas CONCLUÍDOS" vale para quantidades; o contrato conta todas as situações no bloco 1 | Confirmar no planejamento do RF15 | RF15 |
| H-I | O seed AGENDADA (2026-11-20) ficará vencido na data da apresentação | Comparar com a data da demonstração | Antes da entrega |
| H-J | O administrador nunca muda situação (ADR 0011, `ck_his_responsavel`) | Confirmar no RF12 e na RN05 | Planejamento do RF12 |
| H-K | A meta do RNF04 sem os índices | `EXPLAIN ANALYZE` com 1.000 solicitações | Ciclo 6 |

## 12. Resposta final

**Podemos continuar o desenvolvimento normalmente?** Sim, **tecnicamente nada bloqueia o RF10**: 0 achados Bloqueantes, aceites de RF01 a RF09 cumpridos e testados, e sem falha de autorização. **Mas recomendo resolver antes do RF10** (sem pressa e sem refatoração):

1. suas decisões sobre C-02 (403/404), C-03 (dados de exibição) e C-01 (momento e definição da RN05);
2. a correção pequena C-04 (`itens: [null]` → 400);
3. um PR curto de documentação e organização das issues (C-06, C-07, C-08).

O restante pode ser tratado ao longo dos ciclos. Nada foi corrigido nem criado nesta etapa, e a próxima atividade aguarda sua análise.
