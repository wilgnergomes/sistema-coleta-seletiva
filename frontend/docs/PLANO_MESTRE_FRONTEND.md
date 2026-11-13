# Plano mestre do frontend

**Sistema de Coleta Seletiva · Programação Web I · Instituto Federal de Sergipe**  
Revisão final para aprovação · 13 de novembro de 2026  
`main` remota auditada no commit `dfa80517905a834cd1e7c3ca1be73af839f18e4c`.

## Entrega da segunda unidade

O frontend será um único projeto Flutter e Dart em `frontend/`, com Web responsiva como entrega acadêmica. A consulta pública, o cadastro e os fluxos completos de recolhimento terão interface em celular e computador. A operação usa dados fictícios e identificação de demonstração. Android e desktop compartilham o código e entram na matriz de verificação conforme a disponibilidade de dispositivos e toolchains.

| Documento | Decisões que orientam a implementação |
|---|---|
| [Telas e navegação](TELAS_E_NAVEGACAO.md) | Rotas, páginas, campos, ações, estados e aceites por perfil |
| [Design system](DESIGN_SYSTEM.md) | Paleta, tipografia, componentes, acessibilidade e [seis wireframes para aprovação](DESIGN_SYSTEM.md#wireframes-para-aprovação) |
| [Arquitetura e API](ARQUITETURA_E_API.md) | Estrutura Flutter, 33 operações HTTP, contratos, permissões e ambientes |
| [Testes e implementação](TESTES_E_IMPLEMENTACAO.md) | Evidências, metas de desempenho, roteiro acadêmico e branches cumulativas |

## Base e precedência das fontes

O PR #68, integrado em `c71ee0e`, moveu o projeto Spring para `backend/` e registrou a decisão no ADR 0027. A raiz passou a ter um README geral. O README técnico, Maven, Compose, `.env.example`, código, testes e documentação técnica estão em `backend/`. Quem já usa o projeto deve mover o `.env` local para esse diretório e executar a API a partir dele. O nome do projeto Compose continua `coleta-seletiva`, com o mesmo volume. [PR #68](https://github.com/wilgnergomes/sistema-coleta-seletiva/pull/68) · [Árvore auditada](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c) · [ADR 0027](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/adr/0027-backend-em-pasta-propria-no-repositorio.md)

A comparação de conteúdo com a revisão anterior (`162d778`) confirmou que os 106 arquivos Java de produção, os 15 recursos do Spring e scripts SQL, os 50 arquivos de testes e o OpenAPI são idênticos nos novos caminhos. A mudança foi de organização. Continuam válidos os 33 métodos HTTP distribuídos em 21 caminhos, assim como os DTOs, perfis e regras de transição. O PR #68 registrou **575 testes, zero falhas e zero erros**, além de `health` em `UP` e Swagger acessível. Esses resultados são evidências do PR.

O PR #69 atualizou `CLAUDE.md` e `CONTRIBUTING.md`. Ele formaliza branches cumulativas preservadas, revisão entre etapas e a modalidade de implementação integral pela IA quando escolhida expressamente. A `main` auditada ainda tem apenas o backend. Na abertura de `frontend/01-estrutura`, confirme o commit mais recente e validado da `main` remota. [PR #69](https://github.com/wilgnergomes/sistema-coleta-seletiva/pull/69) · [CLAUDE.md](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/CLAUDE.md) · [Guia de contribuição](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/CONTRIBUTING.md)

O enunciado do professor está versionado como Markdown, com as oito páginas identificadas. Foi essa a versão consultada. A árvore e o histórico disponíveis não contêm um PDF do enunciado. As seções 5 a 9 definem o escopo obrigatório, a seção 11 lista as interfaces mínimas e a seção 13 define os cenários de demonstração. [Enunciado versionado](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/referencias/sistema_de_coleta_seletiva.md)

| Fonte | Aplicação no frontend |
|---|---|
| Enunciado e matriz | O enunciado fixa o aceite acadêmico. A matriz associa RFs, decisões, endpoints, fixtures e evidências. [Matriz de rastreabilidade](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/requirements/matriz-rastreabilidade.md) |
| ADRs aceitos e OpenAPI | O ADR 0017 estabelece o YAML como contrato. O springdoc e os testes permitem conferir a implementação. [ADRs](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/adr) · [OpenAPI](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/api/openapi.yaml) |
| Código e testes | Controllers, forms, DTOs, services e consultas mostram o comportamento da versão que o navegador vai encontrar. Trate divergências como itens explícitos de verificação de contrato. [Código](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva) · [Testes](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/test/java/br/edu/ifs/coletaseletiva) |
| Operação, auditorias e desempenho | O README técnico descreve a execução em `backend/` e o `demo`. As auditorias até RF09 são históricas. Confira cada achado no código atual antes de tratá-lo como pendência. [README geral](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/README.md) · [README técnico](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/README.md) · [Auditorias](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/auditorias) · [Melhorias pendentes](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/melhorias-pendentes.md) · [RNF04](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/desempenho/relatorio-rnf04.md) |

### Requisitos funcionais

A base contém operações implementadas para **RF01 a RF15**. Os testes abaixo foram inspecionados e a execução histórica consta da matriz. A aceitação do frontend exige exercitar os mesmos comportamentos pela interface, além dos testes de integração HTTP.

| RF | Obrigação do professor | Implementação encontrada e evidência de referência | Superfície prevista |
|---|---|---|---|
| RF01 | Cadastrar, consultar e atualizar morador, com e-mail válido, normalizado e único | Cadastro público, consulta e edição do próprio morador ativo, administração dos cadastros e situação. `MoradorFluxoIntegracaoTest` | Cadastro público, Minha conta e Moradores |
| RF02 | Gerenciar endereços com dono único e número ou `S/N` | Criar, listar e atualizar por morador. `EnderecoIntegracaoTest` | Minha conta e cadastro administrativo do morador |
| RF03 | Gerenciar materiais, unidade, orientações e inativação | Criação, leitura, atualização, inativação e bloqueio de troca de unidade com dependências. `MaterialIntegracaoTest`, `SolicitacaoIntegracaoTest` | Catálogo e Materiais administrativos |
| RF04 | Gerenciar pontos, endereço, contato, horários, situação e recolhimento domiciliar | Criação, detalhe, lista, atualização e inativação sujeita à RN05. `PontoColetaIntegracaoTest`, `Rn05IntegracaoTest` | Pontos de coleta e gestão de pontos |
| RF05 | Vincular materiais ao ponto com observação e limite positivo opcional | `PUT` de inclusão ou edição e `DELETE` do vínculo, sujeito à RN05. `MaterialAceitoIntegracaoTest`, `Rn05ConcorrenciaIntegracaoTest` | Materiais aceitos no detalhe administrativo do ponto |
| RF06 | Pesquisar pontos ativos por nome, município, bairro e material, com paginação | Filtros combináveis, detalhe com horários e materiais, catálogo público ativo. `PontoColetaPesquisaIntegracaoTest` | Catálogo público e descoberta para o morador |
| RF07 | Cadastrar operadores, vincular a ponto e isolar a atuação | Gestão exclusiva do administrador, vínculo a um ponto por operador e escopo consultado no banco. `OperadorIntegracaoTest`, `SolicitacaoConsultaIntegracaoTest`, `AgendamentoIntegracaoTest` | Operadores e fila do próprio ponto |
| RF08 | Criar solicitação com pelo menos um material compatível, endereço, protocolo e data | Criação transacional e correção com versão antes do agendamento. `SolicitacaoIntegracaoTest`, `SolicitacaoRollbackIntegracaoTest` | Nova solicitação e correção do morador |
| RF09 | Acompanhar solicitações, com filtros, paginação, itens e histórico | Lista resumida por ator, detalhe completo e período por criação. `SolicitacaoConsultaIntegracaoTest` | Minhas solicitações, Fila do ponto e consulta administrativa |
| RF10 | Agendar para data e hora futuras e registrar o histórico | Operador agenda `SOLICITADA`. `AgendamentoIntegracaoTest` | Agendar no detalhe operacional |
| RF11 | Recusar solicitação pendente com justificativa | Operador recusa `SOLICITADA`. `RecusaIntegracaoTest` | Recusar no detalhe operacional |
| RF12 | Cancelar `SOLICITADA` ou `AGENDADA` com motivo | Morador dono ou operador do ponto cancela. `CancelamentoIntegracaoTest` | Cancelar no detalhe do morador ou operador |
| RF13 | Iniciar e concluir, registrando as quantidades efetivamente recolhidas | Operador inicia `AGENDADA` e conclui `EM_RECOLHIMENTO`. Responsável e data vêm do servidor. `ExecucaoIntegracaoTest` | Iniciar e concluir no detalhe operacional |
| RF14 | Reagendar e registrar tentativa sem sucesso com justificativa e nova data | Dois comandos distintos com histórico e versão. `ReagendamentoIntegracaoTest` | Reagendar ou registrar falha no detalhe operacional |
| RF15 | Exibir solicitações por situação e quantidades recolhidas por material no período | Resumo exclusivo do administrador. Contagem por criação e quantidade efetiva de concluídas por data de conclusão e unidade copiada. `ResumoGerencialIntegracaoTest` | Resumo gerencial |

### Regras de negócio

| Regra | Consequência para a interface |
|---|---|
| RN01 · Compatibilidade | Mostrar recolhimento domiciliar, oferecer itens vinculados e ativos e orientar sobre o limite. Cada material entra uma vez. A API decide a disponibilidade no momento da gravação. |
| RN02 · Quantidades | Identificar a unidade em toda linha de item. Estimada positiva, efetiva não negativa e peça inteira. Na conclusão, pelo menos uma efetiva positiva e uma quantidade explícita para cada item. |
| RN03 · Histórico e integridade | Inativar cadastros aplicáveis. Exibir o histórico entregue pela API, com situação anterior e nova, data, responsável e motivo quando houver. |
| RN04 · Dados preservados | Usar `enderecoCopia` e a unidade do item no acompanhamento. Morador dono corrige endereço, itens e observações enquanto `SOLICITADA`. O ponto permanece o mesmo. A correção altera a versão, enquanto o histórico registra mudanças de situação. |
| RN05 · Alterações no ponto | A API bloqueia a inativação de ponto com solicitação aberta e a remoção de vínculo que afete uma aberta. Exibir a mensagem de `409` e oferecer consulta das solicitações do ponto. |
| RN06 · Atomicidade e concorrência | Usar `versao` lida na API e atualizar a interface após confirmação. Recarregar diante de conflito, preservar campos editados para revisão e exigir nova ação explícita. As transições seguem a tabela de ações da integração. |

### Requisitos não funcionais

| RNF | Entrega ou evidência esperada |
|---|---|
| RNF01 · Tecnologias e arquitetura | O backend existente usa Java 21, Spring Boot, PostgreSQL, JPA e Bean Validation. O Flutter consome a API REST. A validação definitiva permanece nos serviços do backend. |
| RNF02 · Usabilidade e responsividade | Testar as telas principais a 360 e 1366 px sem rolagem horizontal da página. Identificar obrigatórios, mostrar erros junto aos campos e confirmar cancelamentos. |
| RNF03 · Contrato e erros | Exercitar JSON, `201`, `400`, `403`, `404`, `409` e respostas inesperadas. Exibir mensagem e erros de campo sem detalhes internos. |
| RNF04 · Desempenho verificável | Respeitar a paginação de até 100 registros nos endpoints paginados. Reproduzir, quando necessário, o ensaio acadêmico HTTP com 1.000 solicitações e p95 até 2 s. Medir o frontend separadamente. A API entrega a lista de endereços inteira. |
| RNF05 · Persistência e consistência | Verificar os registros após reiniciar a API. Usar data de evento com offset e datas civis para filtros. |
| RNF06 · Segurança básica e demonstração | Dados fictícios, configuração sem credenciais no frontend e consultas públicas sem dados pessoais de moradores. Mostrar a simulação de identidade nas áreas restritas da demonstração. |
| RNF07 · Reprodução | Vincular a documentação do frontend ao OpenAPI. Na implementação, registrar configuração do ambiente, comandos, base de dados demo e roteiro. |
| RNF08 · Testes essenciais | Testar compatibilidade, limites, quantidades, transições, concorrência e rollback. Usar os testes do backend como evidência de integridade e os do frontend para interação e integração. |

### Decisões existentes com efeito nas telas

| ADR | Efeito |
|---|---|
| 0002, 0005–0008 | Contratos em português. Endereço do ponto embutido, horários em texto, itens e histórico vinculados à solicitação e cópias de endereço e unidade. |
| 0009, 0010, 0011, 0012 | Versão otimista na solicitação, operador com um ponto, histórico com morador ou operador e enums estáveis em maiúsculas sem acento na API. |
| 0013, 0014 | Alteração de unidade condicionada às dependências. Filtro RF09 e contagem RF15 por criação, quantidade RF15 por conclusão, com dias no fuso de negócio. |
| 0015, 0016, 0020, 0025 | Configuração e persistência são do backend. Leia a organização de pacotes e use o DER físico único do ADR 0025, que substituiu o 0004. |
| 0017, 0018, 0019 | OpenAPI design-first, paginação própria, ações HTTP explícitas, versão no corpo e identificação temporária por cabeçalhos. |
| 0021, 0026 | `MoradorForm` único. Todo morador nasce ativo. Só o administrador pode mudar a situação. O ADR 0026 substitui as decisões 1, 2 e 4 do 0021 e atualiza o entendimento do 0018 e do 0020 nesse ponto. |
| 0022, 0023, 0024 | Seed exclusivo do perfil `demo`, erros em pt-BR e inglês e bloqueio no backend para a RN05. |
| 0027 | Consolida `backend/` como diretório de execução e documentação técnica. Atualiza a organização física do ADR 0015. O futuro aplicativo terá raiz de trabalho em `frontend/`. |

## Decisões propostas para o frontend

| Área | Decisão |
|---|---|
| Identidade visual | Material 3 com tema claro, verde `#176B4D`, fundo `#F7F9F7`, texto `#24352D` e destaque `#E9AD55` com texto escuro. Situações usam rótulo e ícone. Ilustração de intensidade 1/5, restrita a estados em que facilite a compreensão. |
| Experiências | Área pública com catálogo e cadastro. Morador com início por tarefa, pedidos e conta. Operador com fila por situação e detalhe com ações. Administrador com resumo, auditoria de solicitações e cadastros. |
| Layout | Barra inferior para o morador, menu administrativo no compacto e navegação lateral conforme o espaço. Tabelas viram listas rotuladas. A fila do operador combina lista e detalhe no amplo, com ações e histórico no mesmo contexto. Os três fluxos têm wireframes para 360 e 1366 px. |
| Arquitetura | Organização por funcionalidade, com View e ViewModel, repositórios por recurso e cliente HTTP comum. `go_router` trata rotas web, `ChangeNotifier` trata estado observável e `package:http` atende Web e plataformas nativas. |
| Ambiente | Spring e banco de demonstração em `backend/`, Flutter e Caddy em `frontend/` na implementação. A URL relativa `/api` usa a mesma origem no PC e no celular na rede local. Identidade explícita em memória e testes em banco descartável. |
| Entrega | Branches cumulativas segundo o PR #69, com etapa verificável e referência de comparação. A publicação de cada branch e a integração final à `main` dependem das respectivas autorizações. |

São **propostas de experiência** os atalhos do início do morador, a fila com seleção rápida de situação, o detalhe lado a lado, a cópia de protocolo e o gráfico acessível de contagens. Todos usam dados já disponíveis. O [mapa de telas](TELAS_E_NAVEGACAO.md) fixa os comportamentos escolhidos e o [design system](DESIGN_SYSTEM.md) fixa os tokens iniciais.

## Limites da API e alternativas

| Constatação | Solução para a primeira entrega | Possível evolução da API |
|---|---|---|
| Solicitação devolve IDs de ponto, morador e responsável. O operador recebe `403` no cadastro do morador. | Exibir protocolo, endereço copiado, tipo e ID do responsável e ponto identificado pelo catálogo quando disponível. O administrador pode abrir o cadastro autorizado por demanda. | Projeções mínimas de nomes e dados operacionais, com revisão do acesso por ator. |
| O detalhe contém `enderecoCopia`, mas a correção exige `enderecoId`. | Exibir a cópia e pedir seleção explícita de um endereço atual do morador. | Referência de origem do endereço, preservando a cópia histórica e a autorização. |
| `materiaisAceitos` descreve vínculos e omite a situação do material. | Confirmar a disponibilidade pelo catálogo público ou detalhe do material selecionado. Usar o `409` da solicitação para mudanças concorrentes. | Incluir situação efetiva no vínculo ou oferecer consulta de elegibilidade no ponto. |
| Filtros são específicos e a ordenação é fixa. | Aplicar filtros suportados, exibir a ordenação efetiva e usar navegação entre páginas. | Busca global por protocolo ou nome onde faltar, filtros por agendamento ou material no RF09 e ordenação parametrizada. |
| Operador não tem endpoint de perfil próprio. Resumo é global e administrativo. | Fila do ator como página inicial. Exibir o total retornado para a consulta atual e os dois blocos reais do RF15 no admin. | Perfil do ator, resumo por ponto e séries temporais com contrato próprio. |
| Horários são descritivos e endereço é textual. | Apresentar informações cadastradas e disponibilidade de recolhimento domiciliar. | Coordenadas para mapa e horários estruturados para cálculo de disponibilidade. |
| Backend usa identificação simulada e o código auditado não configura CORS. | Demonstração em ambiente controlado, proxy de mesma origem na Web e fronteira de sessão substituível. | Autenticação real e, se necessário, política CORS por ambiente aprovada no backend. |
| `405`/`415`, `403` de cabeçalhos públicos inválidos e alguns detalhes de validação extrapolam o YAML. | Usar o contrato como referência, tratar respostas observadas defensivamente e registrar divergências nos testes. | Alinhar contrato, implementação e ADRs em revisão própria. |

Mapas, geolocalização, notificações externas, fotos, roteirização, pagamentos, exportação de relatórios e login real são extensões fora da entrega básica. A execução nativa é uma extensão de plataforma do mesmo código. Os testes de apresentação usam a Web responsiva.

## Aprovações antes da implementação

| Decisão | Proposta para revisão |
|---|---|
| Composição visual | Aprovar os seis wireframes, em especial filtros secundários no catálogo compacto, lista e detalhe na fila ampla e dois blocos de métricas com datas de referência próprias no resumo. Os aceites de leitura e navegação estão no design system. |
| Ambiente de apresentação | Confirmar se a demonstração será local no PC ou também no celular por rede controlada. A segunda opção exige acesso ao endereço de rede do proxy. |
| Início da implementação | Autorizar separadamente a abertura de `frontend/01-estrutura` e confirmar a modalidade de implementação integral pela IA, caso desejada. Publicação de branches e eventual integração final seguem o fluxo autorizado. |

Os cinco documentos estão prontos para incorporação futura em `frontend/docs/`. Nesta revisão, a evidência de código e contrato é estática. Os resultados de interface, compatibilidade e desempenho dependem dos testes descritos para a implementação.
