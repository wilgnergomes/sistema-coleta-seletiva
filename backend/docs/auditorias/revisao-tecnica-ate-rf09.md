# Auditoria técnica geral: RF01 a RF09

**Data:** 2026-11-08 · **Base:** `main` limpa, commit `eab0a46` · **Autor:** agente `revisor-tecnico` (modelo Opus, esforço alto), somente leitura · **Registro:** texto do agente, gravado pelo Claude principal. Não foram rodados testes nem compilação pelo agente (o último `clean verify` teve 285 testes, 0 falhas). As verificações independentes do Claude principal, que confirmaram ou rebaixaram alguns achados, estão em [diagnostico-consolidado-ate-rf09.md](diagnostico-consolidado-ate-rf09.md).

Fontes cruzadas: enunciado (`docs/referencias/sistema_de_coleta_seletiva.md`), matriz, `openapi.yaml`, ADRs 0001 a 0023 (todos com status Aceita), V1, seeds 010 a 070, código, testes e Issues #1 a #9, #29, #30, #31 e #33.

---

## 1. Veredito geral

**Pode continuar para o RF10. Não há achado Bloqueante.** Os aceites do enunciado para RF01 a RF09 estão implementados, e quase todos têm teste que os prova, inclusive o rollback de integração exigido pelo RNF08 e a concorrência da correção. Autorização e isolamento entre atores estão corretos em todos os endpoints. Nenhuma consulta pública devolve dado pessoal.

Há oito achados **Importantes**. Os mais relevantes:
- A RN05 ainda não existe no código, embora o contrato já prometa o 409. A regra pertence a RF04 e RF05, e a equipe a adiou para o ciclo 6.
- `itens: [null]` derruba a criação com 500 em vez de 400.
- O seed de demonstração não reproduz o cenário "Material incompatível" da seção 13.
- O seed recria a cada início dados que o usuário apagou ou alterou.
- Faltam índices para os filtros do RF09 (RNF04), e a Issue #33 não os prevê.
- Materiais inativos aparecem como "aceitos" na consulta pública de pontos.

Os demais são dívida de documentação e de teste.

---

## 2. Cobertura RF01 a RF09 (aceite do enunciado → implementado → teste que prova)

| RF | Critério do enunciado | Implementado? | Teste que prova (classe#método) | Observações |
|---|---|---|---|---|
| RF01 | Cadastro válido é persistido | Sim | `MoradorFluxoIntegracaoTest#rf01_postDepoisGetDevolveOMoradorGravadoComEmailNormalizado`; `MoradorBancoIntegracaoTest#rf01_salvarEBuscarPreservaOsCamposESituacaoPadraoEhAtivo` | Nome obrigatório e e-mail válido em `MoradorControllerTest` (400) |
| RF01 | E-mail repetido rejeitado após normalização | Sim (service, `uq_mor_email` e `ck_mor_email_normalizado`) | `MoradorFluxoIntegracaoTest#rf01_postComEmailRepetidoEmOutraCaixaRetorna409EMantemUmaLinha`, `#rf01_putComEmailDeOutroMoradorRetorna409`, `#rf01_cadastrosSimultaneosComOMesmoEmailCriamApenasUm`; `MoradorBancoIntegracaoTest#rf01_bancoRejeitaDoisMoradoresComOMesmoEmail` | Na corrida, o 409 sai com mensagem genérica de integridade (já em `melhorias-pendentes.md`) |
| RF01 | Consultar e atualizar | Sim | `MoradorFluxoIntegracaoTest#rf01_moradorAtualizaOProprioCadastro`, `#rf01_buscarIdInexistenteRetorna404`, `#rf01_administradorInativaMoradorQueEntaoPerdeOAcesso` | RN03 com solicitação sem prova (RT-08) |
| RF02 | Endereço incompleto rejeitado | Sim | `EnderecoIntegracaoTest#rf02_enderecoIncompletoOuComFormatoErradoRetorna400SemGravar`, `#rf02_bancoRejeitaCepUfENumeroInvalidos` | `S/N` explícito provado em `#rf02_postDepoisGetLista...` |
| RF02 | Cada endereço tem um único proprietário | Sim (FK `NOT NULL`, mapper nunca troca o dono, busca por `(id, moradorId)`) | `EnderecoIntegracaoTest#rf02_atualizarEnderecoPelaUrlDeOutroMoradorRetorna404EnaoAltera`, `#rf02_atualizarEnderecoDoProprioMoradorMantemODono`, `#rf02_bancoNaoAceitaEnderecoDeMoradorInexistente` | Endereço válido e longo impede o RF08 (RT-07) |
| RF03 | Nome duplicado rejeitado | Sim | `MaterialIntegracaoTest#rf03_nomeRepetidoEmOutraCaixaEComEspacosRetorna409`, `#rf03_bancoRejeitaNomeDuplicadoSemDiferenciarCaixaEUnidadeInvalida` | — |
| RF03 | Material inativo não integra nova solicitação | Sim | `SolicitacaoIntegracaoTest#rf08_pontoInativoPontoSemRecolhimentoMaterialInativoERepetidoSaoRejeitados` (Isopor) | A matriz ainda lista como pendente (RT-13) |
| RF03 | Inativar e ADR 0013 (unidade) | Sim | `MaterialIntegracaoTest#rf03_administradorAtualizaDadosEInativaOMaterial`, `#rf03_trocarAUnidadeComVinculoComPontoRetorna409ENadaMuda`, `#rf03_trocarAUnidadeComItemDeSolicitacaoRetorna409EmInglesENadaMuda`; `MaterialAceitoIntegracaoTest#rf05_umVinculoPassaABloquearATrocaDeUnidadeDoMaterial` | Item vem de fixture SQL, o que o ADR 0013 permite |
| RF04 | Ponto sem nome ou endereço rejeitado | Sim | `PontoColetaIntegracaoTest#rf04_pontoSemNomeOuSemEnderecoOuComCepInvalidoRetorna400SemGravar` | — |
| RF04 | Ponto inativo não aparece como disponível para atendimento | Sim (consulta pública e RN01) | `PontoColetaIntegracaoTest#rf04_listagemPublicaSoAtivosAdministradorFiltraPorSituacao`, `#rf04_pontoInativoEhNaoEncontradoParaOPublicoEVisivelParaOAdministrador`; `SolicitacaoIntegracaoTest#rf08_pontoInativo...` | O bloqueio da RN05 na inativação **não existe** (RT-01) |
| RF05 | Não repetir o mesmo vínculo | Sim (PK composta e upsert) | `MaterialAceitoIntegracaoTest#rf05_vincularDuasVezesNaoRepeteOVinculoEAtualizaOsDados`, `#rf05_bancoRejeitaLimiteZeroEVinculoRepetidoEMantemAIntegridade` | — |
| RF05 | Rejeitar limite ≤ 0 | Sim (`@DecimalMin` exclusivo e `ck_mac_limite`) | `MaterialAceitoIntegracaoTest#rf05_limiteInvalidoRetorna400SemGravar`; `MaterialAceitoControllerTest` | Limite fracionário em `UN` é aceito (RT-09). RN05 no `DELETE` não existe (RT-01) |
| RF06 | Filtrar por vidro retorna só pontos ativos que aceitam vidro | Sim | `PontoColetaPesquisaIntegracaoTest#rf06_filtroPorMaterialVidroRetornaSomentePontosAtivosQueAceitamVidro` (com fixture de ponto inativo que aceita vidro) | Material inativo aparece como aceito (RT-06) |
| RF06 | Nome, município, bairro, material e paginação; exibir endereço, horários, materiais e orientações | Sim | `#rf06_nomeEPorTrechoSemDiferenciarMaiusculas`, `#rf06_municipioEBairroSaoIgualdadeSemDiferenciarMaiusculas`, `#rf06_filtrosSeCombinamComE`, `#rf06_paginacaoFuncionaComFiltro`; `MaterialAceitoIntegracaoTest#rf05_osMateriaisAceitosAparecemNaConsultaDoPonto...` | Busca não ignora acentos (registrado) |
| RF07 | Cadastrar operadores e vinculá-los a pontos | Sim | `OperadorIntegracaoTest#rf07_postDepoisGetDevolveOperadorComEmailNormalizadoVinculadoAoPonto`, `#rf07_atualizarMudaOPontoEInativaOOperador`, `#rf07_soAdministradorGerenciaOperadores` | — |
| RF07 | Operador não atualiza solicitações de outro ponto | Ainda não testável: não existe endpoint de atualização pelo operador (RF10 a RF14) | Leitura isolada provada em `SolicitacaoIntegracaoTest#rf08_consultaPorIdRespeitaOContextoDoAtor` e `SolicitacaoConsultaIntegracaoTest#rf09_operadorVeSomenteAsDoSeuPonto` | Pendência legítima, já registrada na matriz |
| RF08 | Material não aceito rejeitado sem gravação parcial | Sim | `SolicitacaoIntegracaoTest#rf08_materialNaoAceitoPeloPontoEhRejeitadoSemGravacaoParcial` (papel aceito mais metal não aceito; conta as 3 tabelas) | Bom teste |
| RF08 | Criação com itens e histórico, com rollback (RNF08) | Sim | `SolicitacaoRollbackIntegracaoTest#rf08_falhaNaGravacaoDoHistoricoDesfazSolicitacaoEItens` (falha induzida depois do `saveAndFlush`) | Prova real de rollback |
| RF08 | Protocolo único e data de criação | Sim | `SolicitacaoIntegracaoTest#rf08_solicitacaoValidaGeraProtocoloItensEHistoricoInicial`, `#rf08_doisPedidosGeramProtocolosDiferentes` | `uq_sol_protocolo` sem teste de banco. Offset de `criadaEm` não conferido (RT-14) |
| RF08 | Ao menos um item com quantidade estimada positiva | Sim | `SolicitacaoIntegracaoTest#rf08_dadosInvalidosRetornam400`; `SolicitacaoControllerTest#rf08_semItensComQuantidadeInvalidaOuSemCamposObrigatoriosRetorna400` | `itens: [null]` gera 500 (RT-02) |
| RF08 (RN01/RN02/RN04/RN06) | Ponto ativo com recolhimento, material ativo, sem repetição, limite, inteiro em UN, cópias, correção com versão | Sim | `#rf08_pontoInativoPontoSemRecolhimento...`, `#rf08_limiteExcedidoEQuantidadeDecimalEmPecaSaoRejeitados`, `#rf08_limiteIgualAoCadastradoEhAceitoEPilhasInteirasTambem`, `#rf08_mudarOEnderecoDepoisNaoAlteraACopiaDaSolicitacao`, `#rf08_correcaoRevalidaARn01ENaoGravaQuandoFalha`, `#rf08_versaoDesatualizadaRetorna409ComCodigoDeVersaoENadaMuda`, `#rf08_duasCorrecoesSimultaneasNaMesmaVersaoSomenteUmaVence` | H-01 e H-02 sobre a versão |
| RF09 | Retorna só o que corresponde aos filtros e ao contexto do ator | Sim | `SolicitacaoConsultaIntegracaoTest#rf09_moradorVeSomenteAsSuas`, `#rf09_operadorVeSomenteAsDoSeuPonto`, `#rf09_filtroNuncaAmpliaOContextoDoAtor`, `#rf09_filtraPorSituacaoPontoECombinaOsFiltros`, `#rf09_filtraPorPeriodoComOsDoisDiasInclusive`, `#rf09_bordasDoPeriodoUsamOFusoDeNegocioEIntervaloSemiaberto`, `#rf09_paginaOResultadoEInformaOsTotais`, `#rf09_semAtorOperadorInexistenteOuInativoEMoradorInativoRecebem403` | Testes fortes, inclusive as bordas do ADR 0014 |
| RF09 | Exibir detalhes, itens, protocolo, situação, agendamento e histórico | Sim | `#rf09_listaTrazResumoSemItensNemHistoricoEODetalheContinuaCompleto`; `SolicitacaoIntegracaoTest#rf08_solicitacaoValida...` | `agendadaPara`, histórico com responsável OPERADOR e `motivo` nunca são conferidos pela API (RT-14) |

---

## 3. Regras de negócio e requisitos não funcionais

| Item | Situação até o RF09 | O que é de RF futuro |
|---|---|---|
| **RN01** | Atendida e testada: ponto ativo, recolhimento domiciliar, material ativo e aceito, limite, sem repetição (`SolicitacaoService.java:227-281`) | — |
| **RN02** | Estimada positiva (form e `ck_iso_quantidade_estimada`); inteiro em UN na solicitação; unidade exibida no item (cópia). Limite fracionário em UN é aceito (RT-09) | Efetiva ≥ 0 e inteira em UN: RF13 (o banco já tem `ck_iso_quantidade_efetiva`) |
| **RN03** | Não há `DELETE` para morador, material ou ponto (405 provado só para morador). FKs `ON DELETE RESTRICT`. Inativação pelo `PUT`. Histórico inicial com responsável morador. **Falta a prova com solicitação** (RT-08). A correção não grava histórico, por decisão da Issue #8 (H-04) | Histórico das transições: RF10 a RF14 |
| **RN04** | Atendida: cópias de endereço e unidade; correção só em SOLICITADA, com revalidação. Testada | — |
| **RN05** | **Não implementada**: `PontoColetaService.java:79`, `MaterialAceitoService.java:50-57`. Pertence a RF04 e RF05, adiada por planejamento (matriz:29-30, 55 e 101; Issue #4). O contrato já descreve o 409 (`openapi.yaml:428-430` e `:484`) | A matriz põe no ciclo 6. Depende do RF12 só para o operador resolver as abertas (RT-01) |
| **RN06** | Criação atômica com rollback provado. Versão otimista na correção, com teste concorrente | Transições: RF10 a RF14 |
| **RNF01** | Java 21, Spring Boot 4.1.1, REST, PostgreSQL, Spring Data JPA; camadas conforme o ADR 0020; Bean Validation nos forms; regras no service | — |
| **RNF02** | — | Unidade 2 (front) |
| **RNF03** | JSON; 201 com `Location`; 400, 403, 404 e 409; tratador global `{codigo, mensagem, erros}` sem stack trace; 500 genérico. Exceções: `itens: [null]` gera 500 (RT-02); 405 e 415 tratados fora do catálogo do ADR 0018 (RT-13, Issue #31) | — |
| **RNF04** | Paginação com máximo de 100 nas listagens. `GET .../enderecos` não é paginado (RT-15). **Índices dos filtros do RF09 ausentes** (RT-05) | Medição com 1.000 solicitações (ciclo 6) |
| **RNF05** | PK, FK, UNIQUE e CHECK; `timestamptz` e `OffsetDateTime`; transações. O seed recria dados apagados no reinício (RT-04) | — |
| **RNF06** | Dados fictícios; credenciais por variáveis de ambiente (`application.properties` usa `${DB_*}`; só `.env.example` versionado); consultas públicas sem dado pessoal (ponto mostra contato institucional, material só catálogo) | Hash de senha só se houver login |
| **RNF07** | Contrato OpenAPI completo; springdoc; README; migração e seeds. Swagger mostra só os sucessos (registrado); README desatualizado (RT-13) | — |
| **RNF08** | Compatibilidade, quantidades e limites: `[INT]` (não há `SolicitacaoServiceTest`, ao contrário do que a matriz planeja, RT-13). Concorrência: correção e e-mail. Rollback: sim | Transições e concorrência das ações: RF10 a RF14 |

---

## 4. Endpoints × atores

Legenda: "403" significa `ACESSO_NEGADO`. "Público" significa sem cabeçalhos. Em todos os perfis não administradores, o service confere no banco que o usuário existe e está ATIVO.

| Endpoint | Público | MORADOR | OPERADOR | ADMINISTRADOR |
|---|---|---|---|---|
| `POST /moradores` | Cria (cabeçalhos ignorados, nasce ATIVO) | idem | idem | idem |
| `GET /moradores` | 403 | 403 | 403 | Lista paginada |
| `GET /moradores/{id}` | 403 | Só o próprio (outro id: 403) | 403 | Qualquer |
| `PUT /moradores/{id}` | 403 | Só o próprio; mudar a situação dá 403 | 403 | Qualquer, inclusive a situação |
| `POST`/`GET /moradores/{mid}/enderecos` | 403 | Só o próprio | 403 | Qualquer morador existente (inexistente: 404) |
| `PUT /moradores/{mid}/enderecos/{id}` | 403 | Só o próprio; par id e morador divergente: 404 | 403 | Qualquer; par divergente: 404 |
| `POST`/`PUT /materiais` | 403 | 403 | 403 | Sim |
| `GET /materiais`, `/{id}` | Só ATIVOS (inativo: 404) | idem | idem | Todos, com filtro `situacao` |
| `POST`/`PUT /pontos-coleta` | 403 | 403 | 403 | Sim (sem RN05) |
| `GET /pontos-coleta`, `/{id}` | Só ATIVOS (inativo: 404) | idem | idem | Todos, com filtro `situacao` |
| `PUT`/`DELETE /pontos-coleta/{p}/materiais/{m}` | 403 | 403 | 403 | Sim (sem RN05) |
| `/operadores` (todos) | 403 | 403 | 403 | Sim |
| `POST /solicitacoes` | 403 | Só para si, endereço próprio | 403 | 403 |
| `GET /solicitacoes` | 403 | Só as suas | Só as do ponto lido do banco | Todas |
| `GET /solicitacoes/{id}` | 403 | Dono (fora do contexto: 404) | Ponto do operador (fora: 404) | Todas |
| `PUT /solicitacoes/{id}` | 403 | Dono, só em SOLICITADA, com versão | 403 | 403 |

**Achados de autorização:** nenhuma falha material. Morador e ponto nunca vêm do corpo (ADR 0019 e 0010). O filtro `pontoId` não amplia o contexto, e há teste disso. Há uma divergência menor de contrato: em endpoint público, `X-Perfil` inválido ou MORADOR sem `X-Usuario-Id` dão 403 (`AtorArgumentResolver.java:34-39`), e o contrato desses endpoints não lista 403 (RT-12). O OPERADOR não consegue consultar o próprio cadastro (`/operadores` é só do administrador): não é defeito, é risco para o front (seção 8).

---

## 5. Achados confirmados

Nenhum foi classificado como Bloqueante.

### RT-01: RN05 não implementada, e o contrato já promete o 409
- **Prioridade:** Importante.
- **Descrição:** inativar um ponto e remover um vínculo de material funcionam mesmo com solicitações abertas (SOLICITADA, AGENDADA ou EM_RECOLHIMENTO) afetadas.
- **Evidência:** `PontoColetaService.java:79` e `MaterialAceitoService.java:50-57` só têm comentários: "entra no ciclo das solicitações". Contrato: `openapi.yaml:428-430` e `:484`, que descrevem o 409. A matriz põe a RN05 no ciclo 6 (matriz:29-30, 55 e 101), e a Issue #4 a remete aos ciclos posteriores. O comentário do código fala no "ciclo das solicitações", que é o ciclo 3, já encerrado. O planejamento está incoerente.
- **Regra:** RN05 do professor (obrigatória, seção 5), pertencente a RF04 e RF05, que já foram entregues. É adiamento decidido pela equipe, não omissão silenciosa.
- **Impacto real:** desde o RF08, o administrador pode inativar a "Cooperativa Verde Vida" com SEEDA2 (SOLICITADA) e SEEDB3 (AGENDADA) abertas, ou remover o vínculo de Vidro com SEEDB3 aberta. Essas solicitações ficam órfãs, e RF10 a RF14 vão ter de decidir o que fazer com elas. O contrato descreve um comportamento que não existe, embora a matriz marque os endpoints como "✔ executado".
- **Recomendação mínima:** consultar as solicitações abertas do ponto (e, para o vínculo, as abertas do ponto com item daquele material) e lançar `ConflitoRegraException` (409). Incluir testes `[INT]` e as chaves pt e en.
- **Momento:** junto do RF12 (ciclo 4), que é quando o operador passa a poder cancelar as abertas. Não deixar para o ciclo 6. Até lá, corrigir o comentário do código ou a matriz para que digam o mesmo.

### RT-02: elemento nulo em `itens` gera 500 em vez de 400
- **Prioridade:** Importante.
- **Descrição:** `List<@Valid ItemForm> itens` não tem `@NotNull` no elemento. `@NotEmpty` aceita `[null]`, e `@Valid` ignora o nulo. O service então faz `item.materialId()` sobre `null`.
- **Evidência:** `SolicitacaoForm.java:22`, `SolicitacaoCorrecaoForm.java:20`, `SolicitacaoService.java:242-243`. Nenhum teste usa `[null]` (busca em `src/test` sem resultados).
- **Regra:** RNF03 (400 para entrada inválida) e ADR 0018.
- **Impacto:** `POST /solicitacoes` com `"itens":[null]` devolve 500 `ERRO_INTERNO`, com stack trace no log. O mesmo vale para o `PUT`.
- **Recomendação:** `List<@NotNull(message="{...}") @Valid ItemForm>`, chave nova em pt e en, e um teste `[API]`.
- **Momento:** antes do RF10. O RF13 vai repetir o padrão em `ConclusaoEntrada`.

### RT-03: o seed não reproduz o cenário "Material incompatível" da seção 13
- **Prioridade (do agente):** Importante. **Reavaliada pelo Claude principal: Opcional** (ver diagnóstico consolidado).
- **Descrição:** o cenário pede "vidro no ponto anterior", isto é, um ponto que aceita papel e plástico. No seed: o único ponto ativo com recolhimento domiciliar que aceita papel e plástico é a Cooperativa Verde Vida, e ela aceita vidro; os dois pontos sem vidro ("Ecoponto Centro" e "Ponto Comunitário Sem Contato") têm `poc_bl_rec_domiciliar = FALSE`; o service verifica o ponto antes dos itens, então a resposta é "ponto sem recolhimento domiciliar", não "material não aceito".
- **Evidência:** `afterMigrate__040_pontos_coleta.sql:15` e `:19` (FALSE); comentário em `afterMigrate__050_materiais_aceitos.sql:5-6`, que afirma servir ao cenário; `SolicitacaoService.java:102-103` (`exigirPontoDisponivel` antes de `validarEMontarItens`); `SeedDemoIntegracaoTest.java:133-138`, que confere "ativo e sem vidro" e não confere o recolhimento domiciliar.
- **Regra:** seção 13 do enunciado; RNF07 (dados de demonstração); matriz:30.
- **Impacto:** na arguição, a demonstração com o seed mostra a causa errada ou nem mostra o erro.
- **Recomendação:** dar recolhimento domiciliar a um dos pontos sem vidro (por exemplo, o Ecoponto Centro). Depois, revisar os testes do seed que contam pontos com e sem recolhimento domiciliar e ajustar o teste do seed para exigir `poc_bl_rec_domiciliar = TRUE`.
- **Momento:** antes do RF10 (é barato) ou, no máximo, antes da demonstração.

### RT-04: o seed reaplicado a cada início recria dados apagados ou alterados
- **Prioridade (do agente):** Importante. **Reavaliada pelo Claude principal: Opcional.**
- **Descrição:** os scripts são idempotentes por chave natural (e-mail, nome, par ponto e material, logradouro com número e CEP). O que o usuário removeu ou renomeou durante a demonstração volta no próximo início do perfil `demo`.
- **Evidência:** `afterMigrate__050_materiais_aceitos.sql:26` (vínculo removido pelo `DELETE` é reinserido); `afterMigrate__010_moradores.sql:18` (morador com e-mail alterado ganha um "gêmeo" com o e-mail antigo, e o 020 cria endereços para ele); `afterMigrate__030_materiais.sql:17` (material renomeado reaparece). O ADR 0022:34 trata o "recompõe a cada início" como positivo.
- **Regra:** RNF05 ("após reiniciar, os dados confirmados permanecem"); seção 13, cenário "Persistência e indicadores"; ADR 0022.
- **Impacto:** o administrador remove o vínculo Verde Vida e Vidro, reinicia para provar a persistência, e o vínculo volta. O mesmo acontecerá com a demonstração da RN05.
- **Recomendação mínima:** decidir com o estudante, porque altera a consequência de um ADR aceito. Opções: o seed só insere quando a tabela está vazia (ou há uma marca de "seed aplicado"), ou o README orienta subir com `demo` uma única vez e reiniciar sem o perfil.
- **Momento:** antes da demonstração final. Documentar já, no README, é barato.

### RT-05: índices dos filtros do RF09 ausentes e não planejados
- **Prioridade:** Importante.
- **Descrição:** a V1 só tem PK, UNIQUE, `uq_mat_nome` e `idx_ope_poc`. Os filtros e o contexto do RF09 (`poc_nr_id`, `mor_nr_id`, `sol_tx_situacao`, `sol_dt_criacao`) e o detalhe (`his_historico.sol_nr_id`) não têm índice. O PostgreSQL não indexa FKs automaticamente.
- **Evidência:** `V1__criar_esquema_incial.sql:215-264` e `:307-364`. A Issue #33 e a matriz:148 listam só `iso(mat)`, `his(sol, data)` e `mac(mat)`, nada em `sol_solicitacao`. O ADR 0014 (Cuidados) menciona `sol_dt_criacao` só como candidato.
- **Regra:** RNF04 ("índices coerentes com os filtros").
- **Impacto:** não quebra a funcionalidade, mas a evidência do RNF04 fica incompleta e a meta com 1.000 solicitações deixa de ser defensável.
- **Recomendação:** incluir na V2 (#33) os candidatos `sol_solicitacao (poc_nr_id, sol_dt_criacao)`, `(mor_nr_id, sol_dt_criacao)` e `his_historico (sol_nr_id)`, confirmados com `EXPLAIN`.
- **Momento:** junto da Issue #33, antes da medição do ciclo 6.

### RT-06: materiais inativos aparecem como "aceitos" na consulta pública de pontos
- **Prioridade:** Importante (exige decisão da equipe).
- **Descrição:** `materiaisAceitos` e o filtro `materialId` não consideram a situação do material. Um material inativo vinculado aparece nos pontos públicos, e `?materialId=<inativo>` devolve pontos. Ao mesmo tempo, `GET /materiais/{id}` desse material dá 404 ao público, e a RN01 recusa o pedido.
- **Evidência:** `MaterialAceitoMapper.java:10-19`; `PontoColetaService.java:91-105`; `PontoColetaSpecifications.java:64-73`; compare com `MaterialService.java:60`. A decisão do RF05 (matriz:75) permite o vínculo com material inativo, mas não trata da exibição.
- **Regra:** RF06 ("exibir materiais aceitos") combinado com RF03 e RN01; coerência da consulta pública (ADR 0019, item 6).
- **Impacto:** o morador vê "Isopor" como aceito, escolhe o ponto e recebe 409. É inconsistência de catálogo.
- **Recomendação:** para quem não é administrador, filtrar `material.situacao = ATIVO` em `materiaisAceitos` e em `aceitaMaterial`, ou registrar a decisão contrária na matriz. Pedir aprovação.
- **Momento:** antes da Unidade 2 (front). Pode ser feito antes do RF10.

### RT-07: endereço válido do RF02 pode impedir o RF08 (cópia limitada a 400 caracteres)
- **Prioridade:** Importante (já aprovado e rastreado).
- **Evidência:** `SolicitacaoService.java:307-316`; `V1...sql:220`; `melhorias-pendentes.md:20`; Issue #33.
- **Impacto:** um morador com endereço cadastrado válido e longo (até cerca de 490 caracteres) não consegue solicitar, e recebe 409. Não há perda de dados, mas é uma restrição funcional ao RF08.
- **Recomendação:** a V2 já aprovada (500 caracteres).
- **Momento:** Issue #33, de preferência antes do fim do ciclo 4.

### RT-08: RN03 sem prova com solicitação, e constraints do fluxo sem teste de banco
- **Prioridade:** Importante (baixo custo; é critério da Issue #1 e a matriz:102 admite "falta a prova com solicitação").
- **Evidência:** a busca por `fk_sol|fk_iso|fk_his|ck_his_responsavel|uq_sol|ck_iso_` em `src/test` não encontra nada. Só existe `rf01_bancoImpedeExcluirMoradorQueTemEndereco` e o `DELETE /moradores` com 405.
- **Impacto:** a RN03 está atendida pelo desenho (sem `DELETE`, FKs `RESTRICT`), mas sem evidência para morador, ponto e material com solicitação, nem para `ck_his_responsavel` (ADR 0011) e `uq_sol_protocolo`.
- **Recomendação:** um teste `[INT]` que cria uma solicitação pela API e tenta `DELETE` por SQL em `mor_morador`, `poc_ponto_coleta` e `mat_material` (esperando violação de FK), mais inserções inválidas em `his_historico` (zero ou dois responsáveis) e protocolo repetido.
- **Momento:** antes do RF10 ou junto dele.

### RT-09: limite fracionário aceito para material em unidade de peça
- **Prioridade:** Opcional.
- **Evidência:** `MaterialAceitoService.java:44-45` e `VinculoForm.java:15-17` não olham a unidade do material.
- **Regra:** RN02 ("inteiro para unidade de peça") e RF05 ("limite na unidade do material").
- **Impacto:** `limite: 2.5` em "Pilhas" (UN) é gravado, e a RN01 compara inteiros com 2.5.
- **Recomendação:** 409 quando `material.unidade == UN` e `limite` tem casas decimais.
- **Momento:** junto do RT-01 (mesmo service).

### RT-10: `PUT /pontos-coleta` sem `recolhimentoDomiciliar` zera o recolhimento em silêncio
- **Prioridade:** Opcional.
- **Evidência:** `PontoColetaMapper.java:30-33`. A situação omitida é mantida, mas o recolhimento omitido vira `false`. O contrato tem `default: false` (`openapi.yaml:1167`).
- **Impacto:** um administrador que edita só o nome tira o recolhimento domiciliar do ponto, e os pedidos novos passam a dar 409.
- **Recomendação:** tratar `null` como "manter", como já ocorre com `situacao`, e ajustar o contrato. Ou deixar documentado que o `PUT` exige o campo.
- **Momento:** ao final, ou antes do front.

### RT-11: data do protocolo no fuso da JVM, não no fuso de negócio
- **Prioridade:** Opcional.
- **Evidência:** `SolicitacaoService.java:320` (`OffsetDateTime.now().format(...)`), enquanto o filtro usa `FusoNegocio` (ADR 0014).
- **Impacto:** com o servidor em UTC, um pedido feito às 22h em São Paulo, em 08/11, recebe `SOL-20261109-...`, mas o filtro do RF09 o põe em 08/11.
- **Recomendação:** formatar com `fusoNegocio`. Isso ajuda também o RT-14 e o Clock da seção 8.
- **Momento:** antes do RF10.

### RT-12: 403 em endpoints públicos não documentado no contrato
- **Prioridade:** Opcional.
- **Evidência:** `AtorArgumentResolver.java:34-39` e `:44-70`. Os `GET /materiais` e `GET /pontos-coleta` do contrato (`openapi.yaml:276-283` e `:395-402`) listam só 200, 400 e 500.
- **Impacto:** `GET /pontos-coleta` com `X-Perfil: VISITANTE` devolve 403, e o contrato não descreve isso (ADR 0017).
- **Recomendação:** acrescentar `'403'` nesses endpoints ou tratar perfil inválido como público. Contrato e matriz no mesmo PR.
- **Momento:** com a Issue #31.

### RT-13: documentação e comentários desatualizados
- **Prioridade:** Opcional (o Claude principal a reclassificou como Importante no consolidado, por afetar a arguição).
- **Evidências:**
  - `README.md:170`: a lista do seed omite as 8 solicitações do `070`.
  - `README.md:154`: diz que os testes precisam do banco e do `.env`, mas eles usam Testcontainers (já em melhorias).
  - `0018-convencoes-da-api.md:45`: "o ADR 0014 continua Proposta" (está Aceita). Além disso, 405 e 415 já são tratados (`TratadorGlobalExceptions.java:171-181`) fora do catálogo, contra a própria regra do ADR (Issue #31).
  - Matriz:28: "material inativo fora de nova solicitação (RF08)" ainda consta como pendente.
  - Matriz:29: "ponto inativo fora do atendimento (RF08)" ainda consta como não implementado.
  - Matriz:69-70: correção e 403 ainda "A confirmar", embora o ADR 0018 esteja aceito e a Issue #8 tenha aprovado.
  - Matriz:108-115: a matriz planeja `[UNIT]` para compatibilidade, quantidades e limites, mas não existe `SolicitacaoServiceTest`; a prova real é `[INT]`.
  - Comentários obsoletos: `MaterialRepository.java:21-24` ("ainda não têm entidade"), `MaterialAceitoDTO.java:7` ("será preenchido a partir do RF05"), e `PontoColetaService.java:79` e `MaterialAceitoService.java:50` (ciclo diferente da matriz).
  - Issues #29 e #30 seguem abertas com o trabalho visivelmente feito.
- **Impacto:** ruído na arguição ("qual parte atende a qual requisito") e risco de contradição entre ADR aceito e código.
- **Recomendação:** um PR de documentação. ADR aceito só se altera com aprovação.
- **Momento:** antes do RF10 (matriz e README) e na Issue #31 (ADR 0018 e contrato).

### RT-14: lacunas de teste no detalhe do RF09 e nas datas
- **Prioridade:** Opcional.
- **Evidência:** nenhum teste de API confere `agendadaPara`, histórico com responsável `OPERADOR` (o ramo de `SolicitacaoMapper.java:69-71`), `motivo`, nem o formato com offset de `criadaEm`. O teste só confere que o campo existe: `SolicitacaoIntegracaoTest.java:101`. O s4 AGENDADA é montado por SQL, sem agendamento.
- **Regra:** RF09 ("exibir agendamento e histórico") e RNF05 (offset).
- **Recomendação:** consultar pela API uma solicitação do seed (SEEDD5, CONCLUIDA, com 4 linhas de histórico) e conferir `agendadaPara`, `concluidaEm`, `responsavel.tipo = OPERADOR`, `motivo` (SEEDE6) e o offset.
- **Momento:** junto do RF10, que vai precisar disso de qualquer forma.

### RT-15: listagem de endereços sem paginação
- **Prioridade:** Opcional.
- **Evidência:** `EnderecoController.java:44-47` devolve `List`; o contrato define um array (`openapi.yaml:186-200`).
- **Regra:** RNF04 ("paginar listagens"), na leitura literal.
- **Impacto:** baixo, porque cada morador tem poucos endereços. Na arguição pode ser apontado como listagem sem paginação.
- **Recomendação:** justificar na matriz (lista limitada por morador) ou paginar.
- **Momento:** ao final.

---

## 6. Hipóteses a verificar

### H-01: a versão pode subir 2 numa correção que só acrescenta ou retira itens
- **Prioridade:** Opcional.
- **Descrição:** quando endereço e observações não mudam, o código força o incremento (`OPTIMISTIC_FORCE_INCREMENT`, `SolicitacaoService.java:212-218`). Se o Hibernate também incrementar a versão do dono ao mudar a coleção inversa `itens` (comportamento padrão para coleções sujeitas ao bloqueio otimista), a versão sobe 2.
- **Impacto:** contraria a decisão da Issue #8 ("soma 1"). Pelo cálculo, a resposta continua igual ao banco, então o cliente não é prejudicado.
- **Lacuna de teste:** `#rf08_corrigirSoAQuantidadeDeUmItemTambemSomaExatamenteUmaVersao` não lê `sol_nr_versao` no banco, e o caso "retirar o vidro mantendo endereço e observações" não é testado.
- **Como verificar:** teste `[INT]` que cria com papel e vidro, faz `PUT` com o mesmo endereço, as mesmas observações ("Caixas na calçada") e só papel, com versão 0, e depois confere `SELECT sol_nr_versao` = 1 e `$.versao` = 1.

### H-02: concorrência no caminho do incremento forçado
- **Prioridade:** Opcional.
- **Descrição:** nesse caminho o conflito só aparece no commit (`RollbackException`). A tradução esperada para `ObjectOptimisticLockingFailureException`, que vira 409, não é exercitada. O teste concorrente existente muda as observações e usa o outro caminho.
- **Como verificar:** repetir `#rf08_duasCorrecoesSimultaneasNaMesmaVersaoSomenteUmaVence` com o mesmo endereço e as mesmas observações, mudando só as quantidades, e conferir 1 sucesso e o restante 409, nunca 500.

### H-03: representação de `criadaEm` diferente entre `POST` e `GET`
- **Prioridade:** Opcional.
- **Descrição:** o `POST` devolve o `OffsetDateTime.now()` da memória (offset da JVM, nanossegundos). O `GET` devolve o valor lido do `timestamptz` (microssegundos, offset normalizado pelo driver ou pelo Hibernate).
- **Impacto:** mesmo instante com texto diferente, o que confunde o front e testes que comparam strings.
- **Como verificar:** criar e consultar em seguida, e comparar as strings de `criadaEm`.

### H-04: interpretação da RN03 ("cada alteração") sobre a correção sem histórico
- **Prioridade:** Opcional (risco de arguição).
- **Descrição:** a equipe decidiu que a correção não grava histórico (matriz:72). O texto do professor ("situação anterior, nova situação... de cada alteração") sugere mudança de situação, mas pode ser lido de forma ampla.
- **Como verificar:** confirmar com o professor ou registrar a justificativa explicitamente na matriz e no relatório.

### H-05: meta do RNF04 sem os índices do RT-05
- **Como verificar:** carregar o dataset de 1.000 solicitações e rodar `EXPLAIN ANALYZE` em `GET /solicitacoes?pontoId=&de=&ate=` e no detalhe (`his_historico WHERE sol_nr_id`).

---

## 7. Itens verificados sem problemas

- Tratador global: formato `{codigo, mensagem, erros}`, 400 (validação, JSON, tipo, enum, parâmetros), 403, 404 (inclusive rota inexistente), 409 de regra, versão e integridade, 500 genérico.
- i18n (ADR 0023): todas as chaves usadas no código existem nos dois arquivos (105 e 105), e não há texto ao cliente fixo no código.
- Normalização de e-mail no form (trim) e no service (minúsculas), coerente com o CHECK do banco, para morador e operador.
- Mapeamento JPA × V1: tabelas, colunas, tamanhos, `@Version` em `sol_nr_versao`, chaves compostas com `@MapsId`, enums como texto (ADR 0012), `ddl-auto=validate`.
- `@Transactional` em todas as escritas; criação atômica com `saveAndFlush` antes do histórico; rollback provado.
- Contexto do ator no RF09 como condição fixa do WHERE; ponto do operador lido do banco; operador e morador inativos recebem 403.
- Período do RF09 no fuso de negócio configurável, em intervalo semiaberto (ADR 0014), com testes de borda.
- Cópias de endereço e unidade (ADR 0008); bloqueio da unidade (ADR 0013); recusa sem truncar o endereço.
- Paginação com no máximo 100 e DTO `Pagina` próprio (ADR 0018); ordenação estável.
- Seeds coerentes com as constraints (exatamente um responsável no histórico, operador do ponto, unidade copiada, `ck_sol_conclusao`) e idempotentes.
- Credenciais fora do repositório; `.claude/settings.local.json` não versionado; árvore limpa.
- Pacotes conforme o ADR 0020.

---

## 8. Riscos para RF10 a RF15 vindos do código atual

1. **RN05 e solicitações órfãs (RT-01):** RF10, RF13 e RF14 precisam decidir se uma solicitação aberta de ponto inativo, ou com material cujo vínculo foi removido, pode ser agendada, iniciada ou concluída. Hoje esse estado é possível. Fechar a RN05 junto do RF12 evita regras de exceção em cada transição.
2. **Relógio sem abstração:** `OffsetDateTime.now()` direto no service (`SolicitacaoService.java:111` e `:320`). RF10 e RF14 exigem "data e hora futuras" e o cenário "Agendamento inválido". Injetar um `java.time.Clock` (com `FusoNegocio`) antes do RF10 torna as bordas testáveis sem dados frágeis.
3. **Mecanismo único de transição e histórico:** o enum `SituacaoSolicitacao` não tem as transições ("entram nos RF10 a RF14"). O ADR 0011 pede "um único método de registro de histórico", e hoje `historicoInicial` é ad hoc. Convém criar no RF10 a tabela de transições, a checagem de versão (reaproveitando a de `corrigir`) e um único gravador de histórico com o responsável vindo do ator. Nas transições a situação muda, então o UPDATE já incrementa a versão, e o truque do `OPTIMISTIC_FORCE_INCREMENT` não deve ser replicado.
4. **Unidade na conclusão (RF13):** validar o inteiro em UN pela `unidadeCopia` do item, e não por `material.getUnidade()` como no RF08 (`SolicitacaoService.java:266`). Hoje dá no mesmo pelo ADR 0013, mas a cópia é a fonte histórica (ADR 0008).
5. **`itens: [null]` (RT-02):** o mesmo padrão em `ConclusaoEntrada` (RF13) repetirá o 500.
6. **RF15:** sem índices em `sol_dt_conclusao`, `sol_tx_situacao` e `iso(mat)` (RT-05). Agrupar por `iso_tx_copia_unidade`. Nada no banco impede CONCLUIDA com `sol_dt_conclusao` nulo, então o RF13 tem de preencher o campo.
7. **Operador e ponto inativo:** o ADR 0019 e o teste `#rf09_operadorAtivoContinuaConsultandoMesmoComOPontoInativo` permitem o operador ativo de ponto inativo. Decidir explicitamente se ele também pode **agir** (agendar, concluir) e registrar a decisão.
8. **Front (Unidade 2):** o operador não tem como descobrir o próprio ponto pela API (`/operadores` é só do administrador), e a pesquisa pública não filtra por `recolhimentoDomiciliar` (passo 2 do fluxo principal). Não é exigido pelo RF06, mas vai pesar no painel do operador e no formulário do morador.
9. **Seed e demonstração (RT-03 e RT-04):** os cenários "Material incompatível" e "Persistência" estão comprometidos com o seed atual. A SEEDB3 (AGENDADA para 2026-11-20) passará a ter agendamento no passado. Não quebra regra, mas convém lembrar disso ao demonstrar o reagendamento.
10. **i18n:** `MensagensParidadeTest` confere só a paridade entre os arquivos, não se as chaves usadas no código existem. Uma chave esquecida nas ações novas sairia como o próprio texto da chave. Um teste que resolva as chaves usadas evitaria isso.
11. **`spring.jpa.open-in-view`** está no padrão (true). Com mais relações LAZY nas transições, um acesso tardio pode passar despercebido no controller. Desligar antes do RF10 faz esses erros aparecerem cedo.

**ADRs impactados:** 0018 (Cuidados desatualizado; 405 e 415), 0021 (Cuidados desatualizado, já registrado) e 0022 (RT-04, se a estratégia do seed mudar). Todos exigem aprovação do estudante antes de qualquer alteração.

**Observação de processo do agente:** para cruzar as chaves de mensagem, o agente criou por engano um arquivo temporário (`/tmp/usadas.txt`) fora do projeto e do scratchpad. Não pertence ao repositório e não afeta o Git.
