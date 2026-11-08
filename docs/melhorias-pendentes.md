# Melhorias pendentes

Melhorias **não críticas**, registradas para depois. Defeitos que impedem o cumprimento de um requisito não ficam aqui: são corrigidos na própria Issue. A matriz de rastreabilidade trata dos requisitos e de suas evidências, e por isso não recebe estes itens.

Itens que já têm Issue própria no GitHub (por exemplo, #30 e #31) não são repetidos aqui.

Legenda: `[ ]` pendente · `[x]` resolvida (o item é mantido para preservar o histórico, com a data e a referência da resolução).

Também entram aqui os defeitos confirmados, as melhorias opcionais e as hipóteses que decidimos não corrigir de imediato e que não impedem um requisito. Cada entrada diz o **tipo** (defeito confirmado, melhoria opcional ou hipótese), a descrição, o impacto, a localização no código, a prioridade sugerida e o momento recomendado. Nada daqui é corrigido automaticamente: a correção depende de aprovação (regra permanente do `CLAUDE.md`).

## Defeitos confirmados

- [ ] **Id do material formatado com separador de milhar em mensagem de erro.** Tipo: defeito confirmado (reproduzido por teste no RF13). O `MessageFormat` formata argumentos numéricos conforme o idioma, e um `Integer` vira "1.000" em português ou "1,000" em inglês. Em `SolicitacaoService.validarEMontarItens` a mensagem `erro.solicitacao.material-repetido` recebe `item.materialId()` como `Integer`. Impacto: baixo e cosmético; para ids a partir de 1000 o 409 de material repetido mostra o id com ponto ("O material 1.000 aparece mais de uma vez na solicitação."); nenhuma regra nem dado é afetado. Localização: `SolicitacaoService.java:244` (no RF13 o caso equivalente, `conclusao-item-desconhecido` em `TransicaoSolicitacaoService`, já passa o id como texto). Os argumentos de quantidade, como o limite em `erro.solicitacao.limite-excedido`, são formatados pelo idioma e isso é adequado a quantidades. Prioridade sugerida: baixa. Momento recomendado: junto de qualquer próxima alteração em `SolicitacaoService` ou na revisão final antes da entrega; a correção é de uma linha (passar `String.valueOf(item.materialId())` ou citar o nome do material, como no RF13), com um teste usando um id a partir de 1000. Origem: RF13, 2026-11-08.

## Documentação da API (Swagger)

- [ ] **Swagger gerado mostra só a resposta de sucesso.** A interface gerada pelo springdoc lista apenas `200` (inclusive no `POST /moradores`, que na prática devolve `201`), sem os erros `400`, `403`, `404`, `409` e `500` do contrato e com o tipo de conteúdo `*/*`. O contrato `docs/api/openapi.yaml` está completo e é a fonte da verdade (ADR 0017). Avaliar anotações `@Operation`/`@ApiResponse` nos controllers ou publicar o próprio `openapi.yaml` na interface. Origem: RF01, conferência de 2026-11-07.
- [ ] **`POST /moradores` aparece com autenticação na interface gerada.** O contrato o define como público (`security: []`); a interface gerada o mostra com os cabeçalhos `X-Perfil` e `X-Usuario-Id`. Resolver junto com o item anterior.

## Documentação de decisões

- [ ] **ADR 0021, trecho "Cuidados" desatualizado.** O texto diz que a identificação do ator (ADR 0019) está "ainda por implementar" e que o `PUT` aceita `situacao` sem checar o perfil. Isso já foi implementado e testado no RF01. Atualizar o trecho, com aprovação, por ser um ADR aceito.

## Funcionalidades

- [x] **Cópia do endereço limitada a 400 caracteres.** Resolvida em 2026-11-08 pela migração `V2__ampliar_copia_do_endereco.sql` (coluna de 500 caracteres; o máximo calculado é 489), Issue #33, PR 1. A verificação que recusava o pedido com 409, sua constante e as mensagens deixaram de existir. Origem: RF08, 2026-11-07.

- [ ] **Pesquisa de pontos sem distinção de acentos.** Hoje "joao" não encontra "João" e "lagarto" encontra "Lagarto", mas "araçá" não encontra "Araca". Ignorar acentos exigiria a extensão `unaccent` do PostgreSQL e uma migração (junto da V2 da Issue #33). O contrato fala apenas de maiúsculas e minúsculas. Origem: RF06, 2026-11-07.

## Funcionalidades (adiadas por decisão)

- [ ] **Resumo gerencial por ponto para o operador.** Tipo: melhoria opcional. Hoje só o administrador consulta `GET /resumo-gerencial` (decisão do RF15, enunciado seção 3: o administrador consulta os indicadores); o operador recebe 403 e não há filtro por ponto. Impacto: baixo; o painel do operador (seção 11) pode querer ver os totais do próprio ponto. Localização: `ResumoGerencialService.resumir` (acesso) e `ResumoGerencialRepository` (filtros). Prioridade sugerida: baixa. Momento recomendado: só se a interface da segunda unidade pedir; exigiria um filtro por ponto lido do banco (ADR 0019), contrato e testes de isolamento. Origem: RF15, 2026-11-08.

## Concorrência

- [ ] **Tempo máximo de espera pelo bloqueio da linha do ponto.** Tipo: melhoria opcional (nenhum defeito). A RN05 coordena a criação e a correção de solicitações, a inativação do ponto e a remoção de vínculo pelo bloqueio pessimista da linha do ponto (ADR 0024), sem tempo máximo: uma operação espera enquanto a dona do bloqueio estiver ativa. Impacto: baixo, porque as transações são curtas; só aparece se uma transação ficar presa. Localização: `PontoColetaRepository.findComBloqueioById` (um `@QueryHints` com `jakarta.persistence.lock.timeout`) e o `TratadorGlobalExceptions` (mapear `PessimisticLockingFailureException` para 409 "tente novamente"). Prioridade sugerida: baixa. Momento recomendado: só se a medição do RNF04 ou a demonstração mostrarem espera longa; exigiria teste do tempo esgotado e chaves pt/en. Origem: RN05, 2026-11-08.

## Organização do código

- [ ] **`TransicaoSolicitacaoService` concentra todas as ações de situação.** Tipo: melhoria opcional (nenhum defeito). Com o RF14 o serviço passou a ter sete ações públicas (agendar, recusar, cancelar, iniciar, concluir, reagendar e registrar falha) em cerca de 370 linhas, tamanho próximo ao do `SolicitacaoService`. Impacto: baixo; a leitura ainda é simples porque as ações compartilham o mesmo apoio (contexto, versão, transição e histórico). Localização: `TransicaoSolicitacaoService.java`. A regra dos itens da conclusão (`registrarQuantidadesEfetivas`) é o trecho mais independente e o candidato natural a extração. Prioridade sugerida: baixa. Momento recomendado: só se o RF15 ou a RN05 acrescentarem lógica ao serviço, ou na revisão final antes da entrega; extrair apenas com aprovação, sem mudar o comportamento. Origem: RF14, 2026-11-08.

## Testes e ambiente

- [ ] **Dois contêineres no `./mvnw test`.** `ColetaSeletivaApplicationTests` e os testes de integração têm configurações diferentes e sobem contêineres PostgreSQL separados, o que acrescenta cerca de 10 s. Unificar a configuração quando houver mais classes de integração.
- [ ] **Corrida de e-mail pelo `UNIQUE` do banco.** O teste de 8 cadastros simultâneos passa, mas o 409 vem da checagem do service; o caminho em que o banco barra o duplicado e o tratador devolve a mensagem genérica de integridade só é coberto no controller, com o service simulado (relacionado à Issue #30).
- [ ] **README sobre os testes.** O README diz que os testes com contexto precisam do banco de desenvolvimento e do `.env`, mas esses testes usam Testcontainers. Conferir e ajustar o texto.
- [ ] **Consulta nativa do ADR 0013.** Com a entidade `MaterialAceito` (RF05), `MaterialRepository.contarVinculosComPontos` pode deixar de usar SQL nativo e passar a usar uma consulta derivada ou JPQL. Falta a entidade do item de solicitação (RF08) para fazer o mesmo com `contarItensDeSolicitacao`.

## Desempenho (RNF04)

- [x] **Dataset de desempenho com ponto, morador e situação correlacionados.** Resolvida em 2026-11-08 (Issue #33): distribuição corrigida e medição inicial repetida. Diagnóstico em `docs/desempenho/historico-dataset-original/resultados-dataset-original.md`; resultado vigente em `docs/desempenho/resultados-iniciais.md`.
- [ ] **Fase complementar do k6 instável (timeouts de conexão).** Tipo: hipótese (causa não comprovada). Fatos, as seis tentativas e as suspeitas em `docs/desempenho/resultados-iniciais.md` (seção "Instabilidade do ambiente"). Verificar durante a medição final da Issue #58, que acompanha a investigação.
