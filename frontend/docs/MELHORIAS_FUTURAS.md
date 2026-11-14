# Melhorias futuras identificadas no frontend

Limitações encontradas durante a implementação que dependem da API ou de decisões ainda
não tomadas. Nenhuma foi implementada no backend. Na integração final à `main`, convém
registrá-las também em `backend/docs/melhorias-pendentes.md`, conforme o `CLAUDE.md`.

| Item | Tipo | Situação atual no frontend | Possível evolução |
|---|---|---|---|
| Pesquisa de moradores por nome | Melhoria (API) | `GET /moradores` aceita só `page` e `size`; a administração mostra o total e abre um cadastro pelo número (`GET /moradores/{id}`), sem filtrar só a página carregada. | Filtro `nome` (e, se desejado, `email`) em `GET /moradores`. |
| Pesquisa de operadores por nome | Melhoria (API) | `GET /operadores` filtra só por `pontoId`; a pesquisa pelo nome acontece no seletor de pontos, não de operadores. A tela abre um operador pelo número. | Filtro `nome` em `GET /operadores`. |
| Pesquisa por vários materiais | Melhoria (API) | `GET /pontos-coleta` aceita um único `materialId`; a seleção de material é única. | Aceitar vários `materialId` com semântica E: o ponto precisa aceitar todos os selecionados, podendo aceitar outros. |
| Pontos usados recentemente | Limitação documentada | Derivados da primeira página de 20 solicitações e de no máximo 4 leituras de ponto; pontos usados antes disso não aparecem. | Endpoint de pontos recentes do morador ou filtro por ponto distinto. |
| Nomes de pontos nas solicitações | Limitação (API) | Rótulos vêm de uma leitura de até 100 pontos ativos; inativos aparecem como `Ponto #id` na lista. | Projeção com nome do ponto no resumo da solicitação. |
| Endereço na correção | Limitação (API) | O detalhe traz só a cópia do endereço; a correção exige escolha explícita do endereço atual. | Referência ao endereço de origem, preservando a cópia. |
| Disponibilidade do material no vínculo | Limitação (API) | Confirmada pelo catálogo público de ativos (uma página de até 100). | Situação efetiva do material em `materiaisAceitos`. |
| Perfil do operador | Limitação (API) | Sem endpoint próprio; a fila mostra `Operador #id`. | `GET` do próprio operador. |
| Ocultar a barra inferior ao rolar | Opcional não implementado | Barra Material fixa, mais estável e acessível. | Avaliar com testes de usabilidade. |
| Medições ausentes | Pendência de evidência | Sem fluidez de rolagem, memória, Firefox, Safari, celular físico e leitores de tela. | Executar com os aparelhos e navegadores disponíveis. |
| Alvos nativos (etapa 12) | Etapa opcional não iniciada | Android SDK e workload C++ do Visual Studio ausentes. | Instalar as ferramentas e executar os smokes nativos. |
