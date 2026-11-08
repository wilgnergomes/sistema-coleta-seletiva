# Auditoria arquitetural e documental: Sistema de Coleta Seletiva (até o RF09)

**Data:** 2026-11-08 · **Base:** `main` limpa, commit `eab0a46` · **Autor:** agente `arquiteto` (modelo Opus, esforço alto), somente leitura · **Registro:** texto do agente, gravado pelo Claude principal. As verificações independentes do Claude principal estão em [diagnostico-consolidado-ate-rf09.md](diagnostico-consolidado-ate-rf09.md).

Nada aqui é decisão aprovada. Os itens que pedem decisão ficam com o estudante. Nenhum ADR foi criado ou alterado.

Legenda de prioridade: **Bloqueante** (compromete requisito obrigatório, integridade ou a continuidade), **Importante** (inconsistência real, não impede continuar), **Opcional** (melhoria de organização ou documentação).

---

## 1. Veredito geral

O projeto segue bem o documento do professor no escopo entregue (RF01 a RF09). As camadas pedidas no RNF01 estão no código. As regras ficam no service e o banco tem restrições coerentes. O contrato OpenAPI cobre os 15 RFs. Há teste de rollback e de concorrência da correção, seed de demonstração e DER em duas visões.

**Não foi encontrado nada bloqueante para iniciar o RF10.** As inconsistências reais são documentais:
- ADR 0018 desatualizado e contradito pelo tratamento de 405/415;
- matriz com itens "a confirmar" que já foram decididos;
- README desatualizado;
- issues de housekeeping já cumpridas, mas ainda abertas.

Há duas lacunas de requisito obrigatório ainda sem plano concreto: a RN05 não tem Issue, e o RNF04 exige "índices coerentes com os filtros", que a V1 não tem e a Issue #33 não cobre por inteiro.

Dois pontos de método precisam de decisão antes do RF10:
- a política 403 × 404 para o operador de outro ponto;
- se os dados de exibição (nome do ponto, do responsável e do morador) entram no contrato da solicitação.

Os entregáveis finais da seção 14 ainda não existem, o que é esperado nesta fase: relatório, evidência do RNF04, frontend e roteiro dos cenários da seção 13.

---

## 2. Aderência por seção do documento mestre

| Item | Situação | Observação |
|---|---|---|
| RF01 | Atendido | E-mail normalizado e único (V1:26-30, serviço, testes INT e CONC) |
| RF02 | Atendido | Endereço com dono único (`fk_end_mor`), endereço de outro morador → 404 |
| RF03 | Atendido | Nome único (`uq_mat_nome`, V1:96). O material inativo já é rejeitado no RF08 (SolicitacaoIntegracaoTest:131) |
| RF04 | Parcial (resto é futuro) | Cadastro e inativação ok. Falta o bloqueio da RN05, previsto para o ciclo 6 |
| RF05 | Parcial (resto é futuro) | Vínculo único (PK composta) e limite > 0 ok. Falta o bloqueio da remoção (RN05) |
| RF06 | Atendido | O filtro de vidro devolve só pontos ativos (PontoColetaPesquisaIntegracaoTest) |
| RF07 | Parcial (resto é futuro) | O cadastro está pronto. O aceite (operador não atua em outro ponto) depende dos RF10 a RF14 |
| RF08 | Atendido | RN01/RN02 sem gravação parcial, protocolo, histórico inicial, rollback |
| RF09 | Atendido | Filtros, contexto do ator, fuso de negócio (ADR 0014), paginação |
| RF10 a RF15 | Futuro | Endpoints previstos no contrato (openapi.yaml:691-897) |
| RN01 | Atendido | SolicitacaoService:227-281 |
| RN02 | Parcial | Estimada positiva e inteira para UN ok. Quantidade efetiva só no RF13 |
| RN03 | Parcial | Não há DELETE de cadastros (405) e as FKs são RESTRICT. Falta o histórico das transições. Também falta testar a FK RESTRICT com uma solicitação existente (matriz:102) |
| RN04 | Atendido | Cópias de endereço e unidade, correção só em SOLICITADA |
| RN05 | Futuro, sem Issue | O contrato já promete 409 (openapi:429-430, 484) |
| RN06 | Parcial | Criação atômica com teste de rollback, conflito de versão na correção. Faltam as transições |
| RNF01 | Atendido | Java 21, Spring Boot, JPA, camadas (ADR 0020), Bean Validation |
| RNF02 | Futuro | 2ª unidade |
| RNF03 | Atendido, com ressalva | O 403 é acréscimo (ADR 0018). 405 e 415 são tratados fora do catálogo (RA-01) |
| RNF04 | Parcial | Limite de 100 por página ok. Faltam índices para os filtros (RA-05). Medição no ciclo 6 |
| RNF05 | Atendido | PK, FK, UNIQUE, `timestamptz`, volume Docker. A demonstração após reinício fica para o cenário 8 |
| RNF06 | Atendido | `.env` fora do Git, `.env.example` fictício, consultas públicas sem dados pessoais |
| RNF07 | Atendido, com ressalvas | openapi.yaml, README, migrações e seed existem. O README está desatualizado (RA-07) e o Swagger gerado está incompleto |
| RNF08 | Parcial | Compatibilidade, limites, concorrência da correção e rollback ok. Transições no RF10 a RF14 |
| Modelo de dados (seção 10) | Atendido | Todas as entidades e relações. DER com 2 visões. Uma nota pendente (RA-10) |
| Seção 11 (interfaces mínimas) | Futuro | 2ª unidade |
| Seção 13 (cenários de aceite) | Parcial | 3 de 8 já viáveis: cadastro e pesquisa, solicitação válida, material incompatível. Não há roteiro de demonstração |
| Seção 14 (entregas) | Parcial | Repositório, backend, migrações, dados fictícios, README, doc da API e ER existem. Ainda não existem: frontend, relatório, evidências consolidadas dos testes e relatório do RNF04. Participação: ver H-01 |

---

## 3. Consistência entre artefatos

| Assunto | Divergência | Evidência |
|---|---|---|
| Status do ADR 0014 | ADR 0018 diz que o 0014 "continua Proposta", mas ele está Aceita | 0018:45 × 0014:5,13 |
| 405/415 | O código trata (código `VALIDACAO`). Não estão no catálogo do ADR 0018 nem no contrato, e o próprio ADR exige atualizar os dois no mesmo PR | TratadorGlobalExceptions:171-181. ADR 0018:22-30 e :45. openapi:1038. MoradorControllerTest:167,390 |
| Decisões na matriz | `PUT /solicitacoes/{id}` e 403 aparecem como "A confirmar", mas o ADR 0018 está Aceita. No RF15 aparece "(a confirmar)", mas a própria matriz já marca como Decidida (ADR 0014) | matriz:69-70, :85, :88 × :63-64 |
| Pendências "ainda dependem" | RF03 (material inativo) e RF04 (ponto inativo fora do atendimento) já estão cobertos pelo RF08 | matriz:28-29, :97-99 × SolicitacaoIntegracaoTest:131 |
| Tipos dos testes do RNF08 | A matriz prevê `[UNIT]` para compatibilidade, quantidades e limites. Não existe `SolicitacaoServiceTest`: a cobertura real é `[INT]` | matriz:110-112 × lista de testes |
| RF15 | A matriz manda "agrupar por período". O contrato só tem filtro `de`/`ate`, sem agrupar por período | matriz:85 × openapi:880-884, 1375-1385 |
| Porta do banco | ADR 0015 e README sugerem 5433. O `.env.example` tem 5432 | ADR 0015:18, README:209 × .env.example:4 (já citado na #33) |
| Seed no README | O README lista os seeds até os operadores e omite as 8 solicitações do seed 070 | README:170 × afterMigrate__070_solicitacoes.sql |
| Testes no README | Diz que os testes precisam do banco de desenvolvimento e do `.env`, mas eles usam Testcontainers | README:154 × TestcontainersConfiguration:13-16. Ver H-04 |
| Nomes do DER | ADR 0003 fala em `er.dbml`/`er.png`. O real é `der-estrutural.dbml`/`.svg`. A visão transacional cita um `er-cadastros.dbml` que não existe | ADR 0003:17,25. ADR 0004:20. der-transacional.dbml:5 |
| Nota do DER | `mat_tx_unidade` sem a restrição do ADR 0013 | der-estrutural.dbml:47. ADR 0013:32. matriz:147 |
| Cópia do endereço | V1, entidade, service, contrato e DER têm 400 de forma coerente. A ampliação para 500 (aprovada) precisa mudar todos juntos | V1:220, Solicitacao.java:37, SolicitacaoService:71, openapi:1290, der-transacional.dbml:44 |
| Índices | ADR 0007 recomenda `iso(mat_nr_id)` e `his(sol_nr_id, his_dt_data)`. Não estão na V1 | ADR 0007:24 × V1 (só `idx_ope_poc`, V1:208) |
| ADR 0021 | O trecho "Cuidados" diz que o ator ainda não foi implementado | ADR 0021:26 (já em melhorias-pendentes:16) |
| ADR 0023 | Fala em "quatro exceções". Hoje existem também `ParametroInvalidoException` e a base `NegocioException` | ADR 0023:32 |
| Contrato × código (RF01 a RF09) | Campos e tamanhos de forms e DTOs batem com os schemas. Nenhuma divergência encontrada | forms/*, dto/* × openapi:1099-1308 |
| Pacotes (ADR 0020) | A estrutura bate. Pequenos desvios de nome: `service/NormalizacaoEndereco`, `form/FiltroPontoColeta`, `form/FiltroSolicitacao` | ADR 0020:62-72 |

---

## 4. Estado dos ADRs e das issues de housekeeping

### ADRs (todos com status Aceita)

| ADR | Estado | Nota |
|---|---|---|
| 0001 Maven | Vigente | |
| 0002 Idioma | Vigente | |
| 0003 Ferramenta do ER | Vigente, texto desatualizado | Nomes de arquivo. Superado na prática pelo 0004 nesse ponto (RA-09) |
| 0004 Visões do DER | Vigente | Nomes citados como exemplo não batem |
| 0005 Endereço do ponto | Vigente | |
| 0006 Horários | Vigente | |
| 0007 Itens e histórico | Vigente | Índices recomendados ainda não criados (#33) |
| 0008 Cópias | Vigente | O tamanho 500 não muda a substância (#33) |
| 0009 Concorrência | Vigente | Aplicado na correção, falta nas transições |
| 0010 Operador 1:N | Vigente | |
| 0011 Responsável do histórico | Vigente | Não admite administrador como responsável (H-08) |
| 0012 Enums | Vigente | Item 3 (transições no enum) pendente para o RF10. Teste de cada valor no banco ainda não existe (RA-19) |
| 0013 Unidade do material | Vigente | Nota do DER pendente |
| 0014 Datas e fuso | Vigente (aceito em 2026-11-08) | |
| 0015 Docker/.env | Vigente, com divergência | Porta. Item 7 pede outro ADR para o banco de testes, que não existe (RA-17) |
| 0016 Schema `coleta` | Vigente | |
| 0017 Design-first | Vigente | Swagger gerado incompleto (melhorias:11-12) |
| 0018 Convenções | Vigente, desatualizado e contradito | RA-01 |
| 0019 Ator | Vigente | Falta registrar a política 404 para recurso fora do contexto (RA-12) |
| 0020 Pacotes | Vigente | Desvios menores de nome (RA-20) |
| 0021 Morador | Vigente, trecho desatualizado | RA-08 |
| 0022 Seed | Vigente | |
| 0023 i18n | Vigente | Texto pequeno desatualizado |

Nenhum ADR foi substituído formalmente. Nenhum é contradito por outro em substância. A única contradição real é entre o ADR 0018 e o código (405/415).

### Issues de housekeeping

| Issue | Escopo cumprido de fato? | O que ainda falta |
|---|---|---|
| #17 ADRs | É contínua por definição (fecha no último ciclo) | O checklist vai só até o ADR 0012. Faltam 0013 a 0023 |
| #18 Matriz | Contínua. RF01 a RF09 preenchidos | Corrigir os itens "a confirmar" e os "ainda dependem" (RA-02, RA-03) |
| #29 Fundação da API | **Sim.** Tratador global, 404, 500, paginação (`Pagina`), ator (`AtorArgumentResolver`), testes | Nada de código. Só os checkboxes 4 a 10 desmarcados. Pode ser fechada depois da conferência do estudante |
| #30 Exceções do banco e de concorrência | **Sim.** Bloqueio otimista → 409 (TratadorGlobalExceptions:153). Integridade → 409 (:160). Mensagens por chave (ADR 0023 substitui a ideia do `ValidationMessages`). Teste CONC real (SolicitacaoIntegracaoTest:359-391) | Resíduo opcional: corrida no UNIQUE de e-mail coberta só com o service simulado (melhorias:27). Pode ser fechada |
| #31 Contrato e docs | Parcial. `Erro500` existe (openapi:1008). Não há mais "proposta" no contrato. A seção do README está concluída | 405/415 no contrato e no ADR 0018 (RA-01). Coerência do springdoc (melhorias:11-12) |
| #32 Status ADRs/matriz | Parcial. ADR 0020 e ADR 0014 já Aceitos. `CodigoErro` e `RespostaErro` coerentes com o ADR 0020 | Matriz:69-70, :85, :88. Pode absorver a correção do ADR 0018:45 |
| #33 V2, índices, DER | Não iniciada | Escopo incompleto (RA-05, RA-06) |

Higiene: os RF10 a RF15 (#10 a #15) estão sem milestone e não existem milestones Ciclo 4 a 6. As issues #29 a #32 continuam no milestone "Ciclo 1".

---

## 5. Achados CONFIRMADOS

**RA-01: Importante. ADR 0018 desatualizado e contradito pelo código**
- **Evidência:** ADR 0018:45 ("ADR 0014 continua Proposta"). TratadorGlobalExceptions:171-181 trata 405 e 415 com `codigo=VALIDACAO`. Catálogo em ADR 0018:22-30 e enum em openapi:1038 sem 405/415. O próprio ADR 0018:45 exige atualizar catálogo, contrato e ADR no mesmo PR. Issue #31.
- **Relacionado:** RNF03, ADR 0017/0018.
- **Impacto:** o contrato, que é a fonte da verdade, não descreve respostas que a API devolve. `VALIDACAO` para 405/415 também não tem o mesmo sentido do 400.
- **Recomendação mínima:** o estudante decide entre (a) documentar 405/415 reaproveitando `VALIDACAO`; (b) criar códigos próprios. Como o ADR 0018 é aceito, a mudança deve vir por ADR complementar ou revisão aprovada, com contrato e matriz no mesmo PR. Corrigir junto a menção ao ADR 0014.
- **Momento:** antes do RF10 (as ações novas vão herdar o catálogo).

**RA-02: Importante. Matriz com decisões já tomadas marcadas como "a confirmar"**
- **Evidência:** matriz:69-70 (ADR 0018 Aceita). matriz:85 e :88 contradizem :63-64 da própria matriz. Issue #32.
- **Impacto:** passa ao leitor, inclusive ao professor, a impressão de decisão em aberto.
- **Recomendação:** atualizar o status e o registro. Renomear a seção (ver seção 8).
- **Momento:** antes do RF10.

**RA-03: Importante. Matriz com pendências que já foram cumpridas**
- **Evidência:** matriz:28 e :29 ("ainda dependem… RF08") × SolicitacaoIntegracaoTest:131. Tabela da matriz:97-99 sem marcar o que foi cumprido. Matriz:102: a prova da RN03 com solicitação continua faltando, e não há teste de `fk_sol_*`/`fk_iso_*`.
- **Relacionado:** RF03, RF04, RN03.
- **Impacto:** a matriz serve à arguição e hoje está incorreta.
- **Recomendação:** atualizar. Para a RN03, um teste INT barato que tente apagar um morador, material ou ponto com solicitação (espera violação da FK).
- **Momento:** junto do próximo PR de documentação ou do RF10.

**RA-04: Importante (documental). Tipos de teste do RNF08 divergentes**
- **Evidência:** matriz:110-112 prevê `[UNIT]`. Não existe `src/test/.../service/SolicitacaoServiceTest.java`.
- **Impacto:** nenhum sobre o requisito, porque o RNF08 só pede testes automatizados. O problema é a matriz afirmar algo que não existe.
- **Recomendação:** ajustar a matriz para `[INT]`, ou criar testes unitários quando as transições entrarem no enum (RF10).
- **Momento:** junto do RF10.

**RA-05: Importante. RNF04 pede "índices coerentes com os filtros" e eles não existem**
- **Evidência:** a V1 só tem PKs, UNIQUEs e `idx_ope_poc` (V1:208). Filtros do RF09: `poc`, `mor`, `situacao`, `sol_dt_criacao` (SolicitacaoSpecifications:45-63). Filtros do RF06: `lower(municipio)` e `lower(bairro)` (PontoColetaSpecifications:49-53) e uma subconsulta por `mac.mat_nr_id` (:68-70). A #33 e a matriz:148 listam só `iso(mat)`, `his(sol, dt)` e `mac(mat)`.
- **Relacionado:** RNF04, ADR 0007, ADR 0014:27.
- **Impacto:** o texto do RNF04 exige os índices em si, não só o tempo de resposta. Com 1.000 solicitações a meta de 2 s é atingível sem índices, mas a exigência explícita fica descumprida.
- **Recomendação:** ampliar a #33 com candidatos para `sol (poc_nr_id, sol_dt_criacao)`, `sol (mor_nr_id, sol_dt_criacao)`, `sol (sol_dt_criacao)`/`situacao`, índices de expressão em `poc` e `mac(mat_nr_id)`. Cada um com motivação e `EXPLAIN`, como a #33 já prevê.
- **Momento:** antes da medição do RNF04 (ciclo 6). Pode ir na mesma V2 da cópia do endereço.

**RA-06: Importante. Checklist da #33 incompleto para ampliar a cópia do endereço**
- **Evidência:** o limite de 400 está em openapi:1290, Solicitacao.java:37, SolicitacaoService:71 e der-transacional.dbml:44. A #33 fala só em "documentação".
- **Relacionado:** ADR 0008, ADR 0017 (contrato e matriz no mesmo PR).
- **Impacto:** risco de mudar a V2 e deixar o contrato ou a entidade em 400. Com `ddl-auto=validate`, a diferença de tamanho não quebra, mas fica incoerente.
- **Recomendação:** listar os cinco pontos na Issue.
- **Momento:** ao executar a #33.

**RA-07: Importante. README desatualizado**
- **Evidência:** README:170 omite o seed de solicitações; :98 diz "futuramente, banco de desenvolvimento"; :154 descreve a dependência dos testes de forma errada; :285 diz "a pasta docs será criada"; :123 tem o placeholder `<usuario>`; :209 diverge do .env.example:4; :319-320 tem a equipe como placeholder.
- **Relacionado:** RNF07, seção 14.
- **Impacto:** a reprodução e a avaliação do README.
- **Recomendação:** revisão curta.
- **Momento:** antes do RF10 (barato) e de novo antes da entrega.

**RA-08: Opcional. "Cuidados" do ADR 0021 desatualizado**
- **Evidência:** ADR 0021:26 e melhorias:16.
- **Recomendação:** corrigir com aprovação, por ser ADR aceito.
- **Momento:** junto da #32.

**RA-09: Opcional. Nomes dos arquivos do DER nos ADRs 0003 e 0004 e no dbml**
- **Evidência:** ADR 0003:17,25. ADR 0004:20. der-transacional.dbml:5.
- **Recomendação:** nota de errata aprovada (ou substituição do 0003 nesse ponto) e corrigir o comentário do dbml.
- **Momento:** junto da #33.

**RA-10: Importante. Nota do DER sem o ADR 0013**
- **Evidência:** der-estrutural.dbml:47, matriz:147, #33.
- **Relacionado:** seção 10 (diagrama ER obrigatório).
- **Recomendação:** atualizar o dbml e o svg.
- **Momento:** com a #33, antes da entrega.

**RA-11: Importante (organização). Issues com escopo cumprido ainda abertas**
- **Evidência:** quadro da seção 4 (#29, #30, #31, #32, #17).
- **Impacto:** o backlog não mostra o trabalho real e a #17 não lista 11 ADRs.
- **Recomendação:** o estudante confere e fecha a #29 e a #30. Reduz a #31 e a #32 ao que resta. Atualiza o checklist da #17.
- **Momento:** antes do RF10.

**RA-12: Importante. Política 403 × 404 fora do contexto do ator sem registro em ADR**
- **Evidência:** ADR 0019:19 diz "perfil sem permissão → 403". A matriz:72 e :78 diz "fora do contexto → 404", implementado em SolicitacaoService:144-146, :186-187 e :297-300. A matriz não define o caso do "operador de outro ponto" nos RF10 a RF14 (matriz:35-39). O ADR 0019 pede esse teste.
- **Relacionado:** RF07 (aceite), RNF03, ADR 0018/0019.
- **Impacto:** as 7 ações dos RF10 a RF14 podem ser implementadas de forma inconsistente. É uma convenção transversal da API.
- **Recomendação:** antes do RF10, decidir e registrar, por ADR complementar aos 0018/0019. Opções: (a) 404 para solicitação fora do contexto, coerente com o GET e mais simples; (b) 403 para operador de outro ponto. Sugestão do agente: (a), por coerência com o `GET /solicitacoes/{id}`.
- **Momento:** no planejamento do RF10.

**RA-13: Importante. RN05 obrigatória e sem Issue**
- **Evidência:** a seção 5 do enunciado torna todas as RN obrigatórias. RN05 só aparece na matriz:55 e :101. O contrato já promete 409 (openapi:429-430, 484). Nenhuma Issue a cobre (lista #1 a #35).
- **Impacto:** risco de esquecimento. Falta também definir o que é "solicitação aberta afetada".
- **Recomendação:** criar a Issue; definir "abertas" (sugestão: SOLICITADA, AGENDADA e EM_RECOLHIMENTO e, no vínculo, só as que têm item daquele material); avaliar trazer a RN05 para o fim do ciclo 4, já que ela só depende do RF08 e do RF12 (matriz:101).
- **Momento:** criar a Issue antes do RF10 e implementar logo depois do RF12.

**RA-14: Opcional. Milestones**
- **Evidência:** #10 a #15 sem milestone. Ciclos 4 a 6 não existem. #29 a #32 no milestone "Ciclo 1".
- **Recomendação:** criar os milestones Ciclo 4 a 6 e mover as issues.
- **Momento:** antes do RF10.

**RA-15: Importante. RF15: "período" como agrupamento ou como filtro**
- **Evidência:** matriz:85 × openapi:880-884 e :1375-1385.
- **Relacionado:** RF15, ADR 0014.
- **Recomendação:** decidir no planejamento do RF15. O contrato e o ADR 0014 sugerem filtro `de`/`ate`. Alinhar a matriz ao que for decidido.
- **Momento:** junto do RF15.

**RA-16: Opcional. Regra do melhorias-pendentes violada pelo próprio arquivo**
- **Evidência:** melhorias:5 diz que itens com Issue não se repetem, mas :20 repete a #33. A matriz:147-148 também duplica a #33.
- **Recomendação:** deixar só a referência à Issue.

**RA-17: Opcional. Banco de testes (Testcontainers) sem ADR**
- **Evidência:** ADR 0015:23 e :29 pedem outro ADR. pom.xml:101-113. TestcontainersConfiguration:13-16.
- **Recomendação:** um ADR curto (com aprovação) ou o registro no relatório. A decisão já tomada só precisa ser registrada.
- **Momento:** antes da entrega.

**RA-18: Opcional. Data do protocolo no fuso da JVM**
- **Evidência:** SolicitacaoService:320 usa `OffsetDateTime.now()` sem `FusoNegocio`, em desacordo com o espírito do ADR 0014:19-21.
- **Impacto:** cosmético. Em servidor UTC, um pedido às 22h de São Paulo recebe a data do dia seguinte no protocolo.
- **Recomendação:** usar o `FusoNegocio`. Para o RF10 ("data futura"), considerar um `Clock` injetável para testes determinísticos.
- **Momento:** junto do RF10.

**RA-19: Opcional. Falta o teste recomendado no ADR 0012**
- **Evidência:** ADR 0012:27 recomenda gravar cada valor de enum no banco. Não há teste de `ck_sol_situacao`/`ck_iso_unidade`.
- **Recomendação:** os RF10 a RF14 passarão por todas as situações, então basta conferir ao fechar o ciclo 5.

**RA-20: Opcional. Desvios menores de nome em relação ao ADR 0020 e ao texto do ADR 0023**
- **Evidência:** `service/NormalizacaoEndereco`, `form/Filtro*`. ADR 0023:32.
- **Recomendação:** registrar a convenção de filtros e helpers no próximo ajuste aprovado do ADR 0020. Não renomear só por isso.

**RA-21: Importante (antes da 2ª unidade, idealmente antes do RF10). A solicitação não traz dados de exibição**
- **Evidência:** os schemas `Solicitacao` e `Historico` só têm ids (openapi:1261-1296; SolicitacaoResumoDTO:8-19; ResponsavelDTO:5); o ponto inativo devolve 404 para quem não é administrador (matriz:76), então o morador não consegue o nome do ponto de pedidos antigos; `GET /operadores` é só do administrador (matriz:73), então ninguém além dele obtém o nome do responsável no histórico; o operador não lê dados do morador (MoradorService:104), então não obtém nome nem telefone para o recolhimento domiciliar.
- **Relacionado:** seção 11 (lista e detalhe com histórico, painel do operador), RF09 ("exibir detalhes"), RNF06 (não se aplica, porque não são consultas públicas).
- **Impacto:** o front da 2ª unidade não conseguirá montar telas obrigatórias sem mudar o contrato.
- **Recomendação:** decisão do estudante. Proposta: acrescentar `nomePonto`, o nome em `responsavel` e, só para o operador do ponto e o administrador, `nomeMorador`/`foneMorador` no detalhe. Pelo ADR 0017, contrato e matriz mudam no mesmo PR.
- **Momento:** como as ações dos RF10 a RF14 devolvem `SolicitacaoDetalhe`, é mais barato decidir antes do RF10.

**RA-22: Opcional. Template de PR incompleto**
- **Evidência:** .github/pull_request_template.md, item "Documentação atualizada (ER, README, ADR, matriz)". Omite `openapi.yaml` (ADR 0017:18) e `messages*.properties` (ADR 0023:34).
- **Recomendação:** acrescentar os dois itens.

**RA-23: Opcional (antes da entrega). Swagger gerado incompleto**
- **Evidência:** melhorias:11-12. OpenApiConfig só define a segurança.
- **Impacto:** na demonstração, o Swagger UI mostra só as respostas 200. O RNF07 já é atendido pelo `openapi.yaml`.
- **Recomendação:** servir o próprio `openapi.yaml` no Swagger UI ou anotar os controllers. A primeira opção muda o papel do springdoc definido no ADR 0017:17 e por isso pede aprovação.

---

## 6. HIPÓTESES a verificar

| ID | Prior. | Hipótese | Evidência parcial | Como verificar | Momento |
|---|---|---|---|---|---|
| H-01 | Importante | "Histórico de participação" (seção 14): os 126 commits são de um único autor, com duas identidades | `git shortlog -sne`. README:319-320 com placeholders | Confirmar a composição da equipe. Se houver outros integrantes, eles precisam de commits e PRs próprios. Opcional: `.mailmap` para unificar "Wilgner" e "Wilgner S. Gomes" | Já. A participação se constrói ao longo do tempo |
| H-02 | Importante | Os "protótipos" da Etapa 1 (seção 14) não estão no repositório | Nenhum arquivo em docs/ | Perguntar ao professor se já foram entregues ou se a cobrança vem junto da Unidade 2 | Antes da 2ª unidade |
| H-03 | Importante | A correção (`PUT /solicitacoes/{id}`) não grava histórico (matriz:72). A RN03 diz "cada alteração" e o RF14 "cada tentativa e alteração". O professor pode ler de forma ampla | A decisão está só na matriz e na Issue #8, sem ADR | Confirmar a leitura com o professor. Se mudar, o histórico teria SOLICITADA→SOLICITADA com motivo. A estrutura atual já permite | Antes do RF10 |
| H-04 | Opcional | README:154: os testes talvez rodem sem `.env` (Testcontainers com `@ServiceConnection`) | TestcontainersConfiguration:13-16. A URL usa placeholders (application.properties:8) | Rodar `mvnw clean verify` em clone limpo, sem `.env` e sem `docker compose up` | Antes da entrega |
| H-05 | Opcional | `GET /moradores/{id}/enderecos` não é paginado (openapi:186-200). O RNF04 diz "paginar listagens" | Devolve um array | Decidir: justificar no relatório (poucos por morador) ou paginar | Ao final dos RFs |
| H-06 | Opcional | RF15: "apenas CONCLUÍDOS" vale para as quantidades. O contrato conta todas as situações no bloco 1 (openapi:881) | Interpretação razoável, mas não registrada | Confirmar no planejamento do RF15 | Junto do RF15 |
| H-07 | Opcional | O seed AGENDADA tem data fixa (2026-11-20, seed 070:87). Depois disso, a demonstração mostra um agendamento vencido | afterMigrate__070:31-32 | Comparar com a data da apresentação. O seed só insere (ADR 0022), então ajustes exigem um banco novo | Antes da entrega |
| H-08 | Opcional | O ADR 0011 e o `ck_his_responsavel` (V1:337-338) não permitem o administrador como responsável. Hoje é coerente (RN05: "o operador resolve") | Responsavel enum MORADOR/OPERADOR (openapi:1265) | Confirmar no RF12 e na RN05 que o administrador nunca muda situação | Planejamento do RF12 |
| H-09 | Opcional | Corrida entre inativar um ponto (RN05) e criar uma solicitação concorrente nele. `poc` não tem versão. É o mesmo tipo de risco aceito no ADR 0013:30 | | Decidir na RN05: aceitar o risco, documentado, ou travar a linha do ponto | Junto da RN05 |

---

## 7. Documentos adicionais

| Artefato | Utilidade concreta | Obrigatório? | Quando produzir |
|---|---|---|---|
| Relatório (modelo de dados, decisões, evidências dos testes) | Avaliado em "Requisitos e modelagem" (15%) e "Testes" (5%). Pode montar-se a partir do DER, dos ADRs e da matriz | **Sim.** Seção 14: "relatório com modelo de dados, decisões e evidências dos testes" | Ao final dos RFs. Um esqueleto com links pode existir antes |
| Relatório de desempenho do RNF04 | Prova da meta de 95% em até 2 s | **Sim.** RNF04: "registrando máquina, ferramenta e resultados" | Ciclo 6, depois do RF15. O script do dataset pode vir antes. A escolha da ferramenta é registrada no próprio relatório |
| Diagrama ER | Já existe | **Sim.** Seção 10: "deverá ser apresentado em um diagrama entidade-relacionamento" | Atualizar com a V2 (#33) |
| Roteiro dos 8 cenários de aceite (passos e requisições `.http`/curl, ou testes `[INT]` nomeados por cenário) | Demonstração (10%) e prova de que cada cenário funciona | A execução é obrigatória (seção 13). O formato do roteiro, não | Incremental. Completo antes da entrega |
| Protótipos de tela | Guiam as interfaces mínimas (seção 11) e o RNF02 | Citado na Etapa 1 (seção 14: "requisitos, protótipos e modelo de dados"). Ver H-02 | Antes da 2ª unidade |
| Matriz de permissões ator × operação | Hoje as permissões estão espalhadas por 8 linhas da matriz (:72-78) e pelos ADRs 0019/0021. Ajuda os RF10 a RF14, o front e a arguição | Não | Antes do RF10. É uma tabela curta e pode ficar dentro da própria matriz |
| Diagrama de estados da solicitação | Só visual. O enunciado já traz a tabela de transições (seção 8), repetida no README:75-82 | Não | Opcional, no relatório (Mermaid). Não criar documento próprio |
| Casos de uso e fluxos principal/alternativos | O enunciado já tem atores (seção 3), fluxo principal (seção 4), aceites por RF e cenários (seção 13). Os fluxos alternativos são os erros do contrato | Não | Não recomendado produzir |
| Diagramas de sequência | Pouca utilidade. Poderia ajudar a explicar a transação e o rollback do RF08 na arguição | Não | No máximo um, no relatório, se o tempo sobrar |
| Diagrama de arquitetura (front ↔ API ↔ BD) | Útil quando o front existir | Não | No relatório, depois da 2ª unidade |

---

## 8. Redundâncias documentais e melhorias de orientação

**Redundâncias com risco de divergir (algumas já divergiram):**
1. **Pendências do banco e do DER** aparecem em quatro lugares: matriz:147-148, melhorias:20 e :22, Issue #33 e ADR 0013:32. Sugestão: a Issue fica como fonte única e os demais só citam "#33".
2. **Estado de implementação dos endpoints** aparece na matriz, no README "Roadmap", no openapi:27 ("hoje, RF01 a RF09") e nas issues. Sugestão: a matriz como fonte única; o README só aponta para ela.
3. **Quantidade de testes por classe na matriz** (por exemplo "(28: …)") muda a cada PR e envelhece rápido. Sugestão: listar as classes e o que cobrem, sem números. Manter só o total da execução final do ciclo.
4. **Lista de seeds** aparece no README:170, na coluna de seed da matriz e nos próprios SQL. O README já divergiu. Sugestão: o README cita a pasta e a matriz.
5. **A seção "Decisões a confirmar" da matriz** hoje funciona como registro de decisões de negócio aprovadas em Issues, misturado a itens apoiados por ADR. Sugestão: renomear para "Decisões de interpretação e regras aprovadas"; manter "A confirmar" só para o que está de fato aberto; levar a ADR as decisões transversais (RA-12; e, se confirmado, H-03).
6. **O fluxo Git** está no CLAUDE.md, no CONTRIBUTING.md, na skill `desenvolvimento-issue` e na memória do agente. É aceitável, porque cada um tem um público, mas o CONTRIBUTING não menciona a regra do ADR 0017 (contrato e matriz no mesmo PR) nem a do ADR 0023 (chaves nos dois idiomas).

**Melhorias de orientação, sem criar documentos novos:**
- No template de PR, acrescentar "openapi.yaml e matriz (ADR 0017)" e "chaves em messages*.properties (ADR 0023)" (RA-22).
- No CONTRIBUTING, um parágrafo "Documentos que andam juntos" ligando endpoint → openapi + matriz, migração → DER, ADR aceito → não editar sem aprovação.
- No CLAUDE.md, uma linha dizendo que decisões transversais de API (códigos HTTP, políticas de acesso) vão para ADR, e não só para a matriz.

---

## 9. Riscos arquiteturais e dependências (RF10 a RF15 e 2ª unidade)

1. **Mecanismo de transição (RF10, base dos RF11 a RF14).** Os ADRs 0009, 0011 e 0012 já definem: transições no enum (0012:20), um único método de histórico (0011:17), versão no corpo (0018:19). Risco: cada ação implementar sua própria checagem de versão, transição e histórico. Recomendação: um único método transacional de transição (validar ator e contexto → versão → transição no enum → aplicar → histórico) reutilizado pelas 7 ações. O padrão de versão da correção (SolicitacaoService:192-218) funciona, mas o `OPTIMISTIC_FORCE_INCREMENT` com a versão calculada à mão é frágil. Nas transições a situação sempre muda e o `@Version` incrementa sozinho, sem esse truque.
2. **Concorrência.** Coberta pelo `@Version` e pelo handler (TratadorGlobalExceptions:153). O RNF08 pede o teste `[CONC]` nas transições (matriz:114). O teste da correção (SolicitacaoIntegracaoTest:359-391) serve de modelo.
3. **Tempo e "data futura" (RF10 e RF14).** O código usa `OffsetDateTime.now()` direto. Para testes determinísticos e bordas, um `Clock` injetável ajuda (RA-18).
4. **Política de acesso das ações** (RA-12) e **dados de exibição** (RA-21) devem ser decididos antes do RF10, porque afetam as 7 respostas.
5. **RN05** (RA-13, H-09). Depende do RF12. O contrato já promete 409. Falta definir "abertas". Enquanto não estiver implementada, o administrador consegue inativar um ponto com solicitações abertas, e as ações dos RF10 a RF14 sobre esses pedidos precisam de comportamento definido (sugestão: permitir que o operador termine ou cancele, que é justamente o que a RN05 pressupõe).
6. **RNF04.** Faltam os índices (RA-05), o dataset de 1.000 solicitações isolado do seed (matriz:126-136) e a escolha da ferramenta de medição. Fica para o ciclo 6, mas a V2 pode sair antes.
7. **Seed (ADR 0022).** O seed só insere: mudar linhas existentes (por exemplo, quantidades efetivas para o RF15) não atualiza bancos já populados. O RF15 pede concluídas em KG e UN e em períodos diferentes (matriz:40), e hoje só existe 1 concluída. Novos dados devem usar protocolos novos. Documentar no README que é preciso `docker compose down -v` para recompor.
8. **i18n (ADR 0023).** Risco baixo. O `MensagensParidadeTest` protege. As ações novas precisam incluir o parâmetro `AcceptLanguage` no contrato (openapi:691 em diante ainda não tem, como previsto).
9. **Identificação por cabeçalho (ADR 0019) e a 2ª unidade.** A evolução para login está prevista (0019:21) e o resolver isola a leitura do ator (AtorArgumentResolver). Para o front faltam decisões **ainda não registradas** que pedem ADR antes da 2ª unidade: tecnologia e forma de hospedagem do front (SPA separada ou servida pelo Spring); **CORS**, que hoje não está configurado em WebConfig e precisaria liberar `X-Perfil`, `X-Usuario-Id`, `Accept-Language` e expor `Location`; como a UI faz a "seleção de usuário" (seção 3).
10. **RNF02 "erros próximos aos campos".** O 400 traz `erros[].campo`. Os 409 da RN01/RN02 trazem `erros: []` (TratadorGlobalExceptions:142-145), então o front não sabe a qual item associar o erro. Avaliar, com decisão do estudante, preencher `campo` (por exemplo `itens[1].materialId`) nos conflitos por item. Mudaria o uso do formato do ADR 0018, não a estrutura.
11. **Versionamento do contrato.** `info.version: v1`, sem versão na URL. As mudanças previstas (500 caracteres, campos de exibição) são aditivas. Risco baixo. Basta registrar as mudanças no PR, sem estratégia nova.
12. **Migração V1.** O nome tem um erro de digitação (`V1__criar_esquema_incial.sql`). **Não renomear**: o Flyway valida a descrição de migrações já aplicadas.

---

## Decisões pendentes que dependem do estudante

- RA-01: catálogo do 405/415;
- RA-12: política 403 × 404;
- RA-13: o que são "abertas" na RN05 e em que ciclo implementá-la;
- RA-15: "período" no RF15;
- RA-21: dados de exibição no contrato;
- H-03: histórico da correção;
- ADR do front e do CORS (antes da 2ª unidade).

Nenhum ADR foi criado ou alterado.
