# Melhorias pendentes

Melhorias **não críticas**, registradas para depois. Defeitos que impedem o cumprimento de um requisito não ficam aqui: são corrigidos na própria Issue. A matriz de rastreabilidade trata dos requisitos e de suas evidências, e por isso não recebe estes itens.

Itens que já têm Issue própria no GitHub (por exemplo, #30 e #31) não são repetidos aqui.

Legenda: `[ ]` pendente · `[x]` resolvida (o item é mantido para preservar o histórico, com a data e a referência da resolução).

## Documentação da API (Swagger)

- [ ] **Swagger gerado mostra só a resposta de sucesso.** A interface gerada pelo springdoc lista apenas `200` (inclusive no `POST /moradores`, que na prática devolve `201`), sem os erros `400`, `403`, `404`, `409` e `500` do contrato e com o tipo de conteúdo `*/*`. O contrato `docs/api/openapi.yaml` está completo e é a fonte da verdade (ADR 0017). Avaliar anotações `@Operation`/`@ApiResponse` nos controllers ou publicar o próprio `openapi.yaml` na interface. Origem: RF01, conferência de 2026-11-07.
- [ ] **`POST /moradores` aparece com autenticação na interface gerada.** O contrato o define como público (`security: []`); a interface gerada o mostra com os cabeçalhos `X-Perfil` e `X-Usuario-Id`. Resolver junto com o item anterior.

## Documentação de decisões

- [ ] **ADR 0021, trecho "Cuidados" desatualizado.** O texto diz que a identificação do ator (ADR 0019) está "ainda por implementar" e que o `PUT` aceita `situacao` sem checar o perfil. Isso já foi implementado e testado no RF01. Atualizar o trecho, com aprovação, por ser um ADR aceito.

## Funcionalidades

- [ ] **Cópia do endereço limitada a 400 caracteres (decisão aprovada, implementação pendente na Issue #33).** `sol_tx_endereco_copia` guarda até 400 caracteres, mas o endereço cadastrado pode somar 470 só de dados (150 + 10 + 100 + 100 + 2 + 8 + 100) e cerca de 490 com o formato do texto. Para não truncar o histórico (ADR 0008), o RF08 **recusa** o pedido com 409 quando o texto passa de 400, e continua assim até a V2. **Aprovado em 2026-11-08:** ampliar a coluna para 500 caracteres em uma migração V2, sem editar a V1, junto da Issue #33 (que registra a compatibilidade com os limites do endereço, o DER e a documentação). Origem: RF08, 2026-11-07.

- [ ] **Pesquisa de pontos sem distinção de acentos.** Hoje "joao" não encontra "João" e "lagarto" encontra "Lagarto", mas "araçá" não encontra "Araca". Ignorar acentos exigiria a extensão `unaccent` do PostgreSQL e uma migração (junto da V2 da Issue #33). O contrato fala apenas de maiúsculas e minúsculas. Origem: RF06, 2026-11-07.

## Testes e ambiente

- [ ] **Dois contêineres no `./mvnw test`.** `ColetaSeletivaApplicationTests` e os testes de integração têm configurações diferentes e sobem contêineres PostgreSQL separados, o que acrescenta cerca de 10 s. Unificar a configuração quando houver mais classes de integração.
- [ ] **Corrida de e-mail pelo `UNIQUE` do banco.** O teste de 8 cadastros simultâneos passa, mas o 409 vem da checagem do service; o caminho em que o banco barra o duplicado e o tratador devolve a mensagem genérica de integridade só é coberto no controller, com o service simulado (relacionado à Issue #30).
- [ ] **README sobre os testes.** O README diz que os testes com contexto precisam do banco de desenvolvimento e do `.env`, mas esses testes usam Testcontainers. Conferir e ajustar o texto.
- [ ] **Consulta nativa do ADR 0013.** Com a entidade `MaterialAceito` (RF05), `MaterialRepository.contarVinculosComPontos` pode deixar de usar SQL nativo e passar a usar uma consulta derivada ou JPQL. Falta a entidade do item de solicitação (RF08) para fazer o mesmo com `contarItensDeSolicitacao`.
