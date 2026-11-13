# Arquitetura Flutter e integração com a API

[Plano mestre](PLANO_MESTRE_FRONTEND.md) · [Telas e navegação](TELAS_E_NAVEGACAO.md) · [Testes e etapas](TESTES_E_IMPLEMENTACAO.md)

O contrato em `backend/docs/api/openapi.yaml` usa OpenAPI 3.0.3, com versão informativa `v1` e **33 operações em 21 caminhos**. Na `main` auditada (`dfa8051`), o YAML e os arquivos dos controllers, DTOs, formulários e serviços são idênticos aos da revisão anterior após a mudança de pasta. O Spring atende na raiz de `http://localhost:8080` por padrão. `/api` será o prefixo do proxy no navegador. [OpenAPI versionado](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/api/openapi.yaml)

## Estrutura proposta

| Pasta futura em `frontend/` | Responsabilidade |
|---|---|
| `lib/app/` | Composição de dependências, configuração, rotas, sessão e temas. `lib/main.dart` inicia a aplicação. |
| `lib/core/` | Cliente HTTP, `Pagina<T>`, decodificação comum, falhas e utilitários de data, quantidade e unidades. |
| `lib/ui/compartilhado/` | Navegação adaptativa, campos reutilizáveis, status, feedback, paginação e visualizações de lista/detalhe. |
| `lib/features/{catalogo,moradores,solicitacoes,operadores,resumo_gerencial}/` | Views e ViewModels em `ui/`, acesso a dados e modelos em `data/`. O catálogo concentra pontos, materiais e vínculos. Solicitações concentra lista, detalhe, correção e operação por ator. |
| `lib/l10n/` | Rótulos e mensagens próprias em pt-BR. Usar os recursos de localização do Flutter para componentes e formatação. |
| `test/`, `integration_test/`, `test_driver/` | Testes unitários, de widgets, integração e driver para Web quando necessário. |
| `assets/`, `web/`, `android/`, `windows/`, `linux/`, `macos/` | Fontes locais e arquivos dos alvos ativados na implementação. O primeiro alvo é Web. |
| `Caddyfile` | Serviço local do build Web e encaminhamento de `/api/*` ao backend na demonstração. |
| `docs/`, `README.md`, `pubspec.yaml`, `pubspec.lock`, `analysis_options.yaml` | Planejamento, execução, dependências e análise estática. Ficam dentro de `frontend/`. |

### Escolhas e alternativas

| Tema | Proposta | Justificativa e alternativa considerada |
|---|---|---|
| Organização | Módulos por funcionalidade com duas camadas, interface e dados | View renderiza e envia eventos. ViewModel expõe estado e orquestra tarefas. Repositório concentra recursos, cache e conversão. Cliente HTTP centraliza transporte. Uma camada de domínio separada só se justifica quando surgir lógica compartilhada que exceda essa composição. |
| Estado | `ChangeNotifier`, `ListenableBuilder` e estados imutáveis por tela | SDK suficiente para carregamento, conteúdo, falha e mutações. Estado local para foco e edição, estado de sessão compartilhado. Riverpod ou BLoC podem ser reavaliados se o volume de dependências ou eventos justificar a adoção. |
| Injeção | Construtores, composição em `app/` e interfaces nos repositórios usados por ViewModels | Fakes simples nos testes. `provider` é uma alternativa recomendada pelo Flutter se a composição por construtores ficar extensa. |
| Rotas | `go_router`, com `ShellRoute` por contexto e guard central | URLs, voltar/avançar e links diretos justificam a dependência. `Navigator` do SDK atende fluxos simples, mas a sincronização com URL e shells teria mais código. Use estratégia hash no primeiro host estático. URLs sem hash são evolução com fallback SPA configurado. |
| HTTP | `package:http` com `Client` injetado, reaproveitado e fechável | API JSON pequena e transversal a Web e nativo. O cliente comum cuida de cabeçalhos, prazo, status e decodificação. Dio é alternativa se houver necessidade comprovada de recursos adicionais. |
| JSON | Modelos Dart imutáveis e explícitos. Leitura e escrita separadas quando têm campos distintos. `dart:convert` e testes de contrato | O contrato tem `allOf`, exceções de permissão e comportamentos específicos por ação. Gerar um cliente inteiro do OpenAPI seria uma opção futura após normalizar divergências e medir o ganho. `json_serializable` pode ser avaliado quando o mapeamento repetitivo passar a justificar build adicional. |
| Datas e idioma | `flutter_localizations` do SDK, `intl` para formatação e `timezone` para a zona IANA configurada | Suporta entrada em pt-BR e conversão de instante consistente entre plataformas. `DateTime` local do dispositivo sozinho não preserva a referência da operação quando a máquina está em outro fuso. Usar base IANA empacotada e medir seu custo. |
| Tema e gráficos | `ThemeData`, `ColorScheme`, `ThemeExtension` e widgets do SDK. Barras simples no RF15 com valores textuais | O painel traz seis contagens e uma lista agregada. Uma biblioteca de gráficos adicionaria custo para esse escopo. |

Fixar uma versão estável do Flutter compatível com as dependências na primeira branch, registrar `flutter --version` e versionar o `pubspec.lock` do aplicativo. O plano prioriza as recomendações de Views, ViewModels e repositórios do Flutter e usa composição por construtor pela dimensão do projeto. [Arquitetura Flutter](https://docs.flutter.dev/app-architecture/recommendations) · [Rotas](https://docs.flutter.dev/ui/navigation) · [URL Web](https://docs.flutter.dev/ui/navigation/url-strategies) · [HTTP](https://pub.dev/packages/http) · [Fusos IANA](https://pub.dev/packages/timezone)

A revisão mantém essas escolhas. Introduza `go_router` e `http` na infraestrutura inicial. Use Material 3 e `ChangeNotifier` do SDK. Acrescente formatação e base de fuso quando entrarem datas de negócio, com teste de custo no build. Os dados de calendário civil do RF09 podem ser montados sem carregar a base de fuso até o primeiro uso de um evento com hora. Formulários, serialização, gráfico gerencial e injeção cabem no escopo do SDK e nas dependências já propostas.

### Fluxo de dados e estado

1. A View envia o evento ao ViewModel. O ViewModel captura o ator, a versão e os valores do formulário para aquela operação e usa o repositório correspondente.
2. O repositório usa o cliente HTTP com modo de acesso explícito. O cliente monta URL, aplica cabeçalhos permitidos, lê status e converte JSON ou falha. A View trabalha com modelos tipados.
3. Atualizações de lista e detalhe têm controle independente de solicitação em andamento. Use uma chave por consulta e um contador de geração de sessão e filtros para descartar respostas que ficaram obsoletas. Use cancelamento de leitura quando suportado pelo cliente, mantendo a checagem de geração.
4. Uma mutação confirmada substitui o detalhe pelo DTO retornado. Invalide as consultas que podem ter mudado e releia só as visíveis ou as próximas a abrir. Em lista paginada, releia a página para reconciliar filtros e totais, inclusive quando um registro deixa de pertencer à consulta.
5. Cache é em memória e limitado por tamanho e idade. Catálogos públicos usam chave pública própria. Listas e detalhes restritos usam perfil, ID, sessão, recurso e query. Invalide cadastros relacionados quando houver mutação ou troca de ator. Nunca reexiba conteúdo restrito após perda de acesso.

Carregar uma página de cada vez, evitar refazer o `Future` em `build`, e manter a rolagem e o formulário ao mudar a largura. Enviar cabeçalhos apenas ao endereço configurado. Não seguir `Location` para ler os recursos criados. Use `id` da resposta e a rota conhecida. Em versão demo, persistência de rascunho e identidade é só em memória.

## Identificação de demonstração

| Modo de chamada | Cabeçalhos de identidade | Uso |
|---|---|---|
| Público | Nenhum | `POST /moradores` e leituras públicas de pontos e materiais. Mesmo com ator selecionado, o catálogo público usa esta chamada. |
| Morador | `X-Perfil: MORADOR` e `X-Usuario-Id: <id>` | Dados do próprio morador e ações permitidas. |
| Operador | `X-Perfil: OPERADOR` e `X-Usuario-Id: <id>` | Consulta e ação sobre solicitações do ponto associado no banco. |
| Administrador | `X-Perfil: ADMINISTRADOR` | Gestão, consulta ampla, catálogo administrativo e resumo. O resolver dispensa ID. |

Enviar `Accept: application/json` e `Accept-Language: pt-BR` nas chamadas. Acrescentar `Content-Type: application/json` quando houver corpo JSON. O `AtorArgumentResolver` aceita ausência de perfil nas leituras opcionais e responde `403` por identificação sintaticamente inválida quando o cabeçalho de perfil é enviado. Serviços conferem existência e atividade onde o contexto é obrigatório. A UI pede ID positivo no intervalo de `int32` para morador e operador, valida a seleção com a primeira chamada do perfil e reaproveita a resposta. Para operador, use `GET /solicitacoes?situacao=SOLICITADA&page=0&size=20`, a primeira consulta da fila. A seleção de administrador é explícita e só envia cabeçalho quando escolhida. [Resolver e configuração](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/config)

Definir uma fonte de sessão e uma política de credenciais no cliente HTTP. A sessão expõe ao router e às Views o perfil selecionado, ID quando aplicável e escopo da sessão. O provedor demo cria os cabeçalhos. Uma futura autenticação pode trocar a fonte de sessão e a política HTTP, conservando rotas, repositórios e telas que dependem do contexto do ator. Guard de rota, visibilidade de botões e flags de build servem à experiência. A API atual aceita cabeçalhos declarados pelo cliente, então a demonstração com operações restritas usa ambiente controlado e banco fictício.

Use `APP_MODE=demo` para compilar a entrada de simulação em desenvolvimento ou apresentação. O modo público deixa essas rotas e opções indisponíveis na interface. Isso não torna o backend atual apropriado para dados reais. Após recarregar a página, uma rota restrita pede nova seleção. Roteiro demo resolve IDs fora do cliente pelo admin escolhido. O app não lista moradores e operadores com um cabeçalho administrativo implícito para montar o seletor.

## Inventário de operações

`P` significa público, `M` morador ativo, `O` operador ativo e `A` administrador. `M próprio` inclui apenas seus dados ou solicitações. `O do ponto` depende do vínculo lido no banco. GET de catálogo com `P / A` usa uma rota HTTP com duas visões. Criações `201` devolvem o DTO e o cabeçalho `Location`. A tabela inclui a API funcional completa do YAML. Actuator e Swagger servem à operação e documentação.

| Método e caminho | Acesso | Query ou corpo Java | Sucesso e retorno Java |
|---|---|---|---|
| `POST /moradores` | P | `MoradorForm` | `201` · `MoradorDTO` |
| `GET /moradores` | A | `page`, `size` | `200` · `Pagina<MoradorDTO>` |
| `GET /moradores/{id}` | M próprio / A | — | `200` · `MoradorDTO` |
| `PUT /moradores/{id}` | M próprio / A | `MoradorForm` | `200` · `MoradorDTO` |
| `POST /moradores/{moradorId}/enderecos` | M próprio / A | `EnderecoForm` | `201` · `EnderecoDTO` |
| `GET /moradores/{moradorId}/enderecos` | M próprio / A | — | `200` · `List<EnderecoDTO>` |
| `PUT /moradores/{moradorId}/enderecos/{id}` | M próprio / A | `EnderecoForm` | `200` · `EnderecoDTO` |
| `POST /materiais` | A | `MaterialForm` | `201` · `MaterialDTO` |
| `GET /materiais` | P / A | `situacao` só para A, `page`, `size` | `200` · `Pagina<MaterialDTO>` |
| `GET /materiais/{id}` | P / A | — | `200` · `MaterialDTO` |
| `PUT /materiais/{id}` | A | `MaterialForm` | `200` · `MaterialDTO` |
| `POST /pontos-coleta` | A | `PontoColetaForm` | `201` · `PontoColetaDTO` |
| `GET /pontos-coleta` | P / A | `nome`, `municipio`, `bairro`, `materialId`, `situacao` só para A, `page`, `size` | `200` · `Pagina<PontoColetaDTO>` |
| `GET /pontos-coleta/{id}` | P / A | — | `200` · `PontoColetaDTO` |
| `PUT /pontos-coleta/{id}` | A | `PontoColetaForm` | `200` · `PontoColetaDTO` |
| `PUT /pontos-coleta/{pontoId}/materiais/{materialId}` | A | `VinculoForm` | `200` · `MaterialAceitoDTO` na inclusão ou edição |
| `DELETE /pontos-coleta/{pontoId}/materiais/{materialId}` | A | — | `204` · sem corpo |
| `POST /operadores` | A | `OperadorForm` | `201` · `OperadorDTO` |
| `GET /operadores` | A | `pontoId`, `page`, `size` | `200` · `Pagina<OperadorDTO>` |
| `GET /operadores/{id}` | A | — | `200` · `OperadorDTO` |
| `PUT /operadores/{id}` | A | `OperadorForm` | `200` · `OperadorDTO` |
| `POST /solicitacoes` | M próprio | `SolicitacaoForm` | `201` · `SolicitacaoDetalheDTO` |
| `GET /solicitacoes` | M próprio / O do ponto / A | `situacao`, `pontoId`, `de`, `ate`, `page`, `size` | `200` · `Pagina<SolicitacaoResumoDTO>` |
| `GET /solicitacoes/{id}` | M próprio / O do ponto / A | — | `200` · `SolicitacaoDetalheDTO` |
| `PUT /solicitacoes/{id}` | M próprio | `SolicitacaoCorrecaoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/agendamento` | O do ponto | `AgendamentoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/recusa` | O do ponto | `MotivoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/cancelamento` | M próprio / O do ponto | `MotivoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/inicio` | O do ponto | `VersaoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/conclusao` | O do ponto | `ConclusaoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/reagendamento` | O do ponto | `ReagendamentoForm` | `200` · `SolicitacaoDetalheDTO` |
| `POST /solicitacoes/{id}/falha` | O do ponto | `FalhaForm` | `200` · `SolicitacaoDetalheDTO` |
| `GET /resumo-gerencial` | A | `de`, `ate` | `200` · `ResumoGerencialDTO` |

### Paginação, filtros e ordenação

| Recurso | Comportamento verificado |
|---|---|
| Listas paginadas | `page` inteiro a partir de 0, padrão 0. `size` de 1 a 100, padrão 20. JSON `{conteudo, pagina, tamanho, totalElementos, totalPaginas}`. A interface pode mostrar páginas a partir de 1. `tamanho` representa a capacidade da página, inclusive na última. |
| Moradores, materiais, pontos e operadores | `id` ascendente. `situacao` filtra pontos e materiais somente com A. A consulta pública devolve apenas ativos. A lista de operadores aceita `pontoId` e retorna página vazia para ponto inexistente. |
| Pontos | Filtros combinados por E. `nome` por trecho, até 120 caracteres. `municipio` e `bairro` por igualdade, até 100 cada. Texto em branco é ignorado após validação, sem diferença de caixa, acentos significativos. `%` e `_` são literais. `materialId` filtra vínculo existente, material inexistente dá página vazia. `recolhimentoDomiciliar` é atributo da resposta e não filtro da API. |
| Solicitações | `criadaEm` descendente e `id` descendente no empate. `situacao`, `pontoId`, `de` e `ate` são combináveis e intersectam o escopo do ator. Período por criação. Sem filtro de protocolo, endereço, material, morador, operador ou data agendada. |
| Endereços | Array completo por morador em `id` ascendente, sem GET individual. `Location` de criação identifica a URL do endereço, que possui PUT e não GET. |
| Detalhe de solicitação e de ponto | Itens e vínculos por `materialId` ascendente. Histórico por `id` ascendente. Mostrar sequência do servidor e preservar agendamentos e motivos antigos. |
| Resumo gerencial | Sem paginação. Seis situações na ordem do enum, com zeros. Quantidades ordenadas por nome do material e unidade copiada. `de` e `ate` opcionais, independentes. |

Ao aplicar filtros, reiniciar em `page=0`, omitir parâmetros em branco e codificar valores com `Uri`. Consulta além da última página pode retornar conteúdo vazio com `totalElementos` ainda positivo. Após mutação ou perda de página, navegue para a última válida e faça uma leitura, ou mostre o estado vazio se `totalPaginas=0`. Para escolher ponto domiciliar, carregar opções paginadas e sinalizar as páginas com pontos sem recolhimento. A consulta e sua contagem representam o catálogo no servidor. [Controllers](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/controller) · [Serviços](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/service) · [Consultas](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/repository)

### Formulários e serialização

Os nomes abaixo são chaves JSON. Campos sem marca de opcional são obrigatórios. Limites de texto correspondem aos forms em Java. Em `PUT`, enviar os campos editáveis da representação inteira. Situação omitida preserva a anterior nos quatro cadastros. As ações de solicitação usam seus corpos específicos.

| Schema OpenAPI · form Java | Campos, formatos e observações |
|---|---|
| `MoradorEntrada` · `MoradorForm` | `nome` até 80, `email` válido até 150, `fone` opcional até 20, `situacao` opcional. API normaliza e-mail, garante unicidade e ignora situação válida no POST. Em PUT, só A pode mudá-la. |
| `EnderecoEntrada` · `EnderecoForm` | `logradouro` até 150, `numero` até 10 ou `S/N`, `bairro` e `municipio` até 100, `uf` duas letras ASCII maiúsculas, `cep` string com oito dígitos, `complemento` opcional até 100. Campos obrigatórios exigem conteúdo. |
| `MaterialEntrada` · `MaterialForm` | `nome` até 80, `descricao` até 500, `unidade` `KG`/`L`/`UN`, `orientacao` até 1.000, `situacao` opcional. Nome único sem distinguir caixa. Alteração de unidade em material com item ou vínculo é `409`. |
| `PontoEntrada` · `PontoColetaForm` | `nome` até 120, `endereco` com `EnderecoForm`. `contato` opcional até 150, `horarios` opcional até 500, `situacao` opcional. Enviar `recolhimentoDomiciliar` booleano explícito tanto em POST como PUT. O mapper trata ausência como `false`. |
| `VinculoEntrada` · `VinculoForm` | `observacoes` opcional até 500, `limite` numérico opcional maior que zero, até 9 dígitos inteiros e 3 decimais. `null` remove o limite. Material e ponto são da URL. No contrato atual o limite aceita fração inclusive para `UN`. Quantidades solicitadas ou recolhidas em `UN` são inteiras. |
| `OperadorEntrada` · `OperadorForm` | `nome` até 80, `email` válido até 150 e `pontoId` inteiro de ponto existente. `situacao` opcional. API aceita vínculo a ponto inativo e normaliza e-mail. |
| `SolicitacaoEntrada` · `SolicitacaoForm` | `pontoId`, `enderecoId` próprio, `observacoes` opcional até 500 e `itens` com ao menos um `ItemEntrada`. Morador vem do ator. |
| `SolicitacaoCorrecao` · `SolicitacaoCorrecaoForm` | `enderecoId`, `observacoes` opcional até 500, `itens` e `versao` não negativa. Ponto vem do registro. |
| `ItemEntrada` · `ItemForm` | `materialId`, `quantidadeEstimada` numérica positiva com até 9 dígitos inteiros e 3 decimais. `UN` exige inteiro, material único e limite do vínculo respeitado. |
| `VersaoEntrada` · `VersaoForm` | `versao` inteira a partir de 0, obtida da última leitura do detalhe. |
| `AgendamentoEntrada` · `AgendamentoForm` | `dataHora` ISO-8601 com offset, `versao`. A data é estritamente futura. |
| `MotivoEntrada` · `MotivoForm` | `motivo` com conteúdo e até 500, `versao`. |
| `ConclusaoEntrada` · `ConclusaoForm` / `ItemConclusaoForm` | `versao`, `itens` com uma entrada por item já gravado. Cada entrada contém `materialId` e `quantidadeEfetiva` numérica não negativa, com até 9 dígitos inteiros e 3 decimais. Uma positiva no mínimo, `UN` inteira pela unidade copiada. |
| `ReagendamentoEntrada` · `ReagendamentoForm` | `dataHora` futura, `motivo` com conteúdo até 500, `versao`. Deve mudar o instante marcado. |
| `FalhaEntrada` · `FalhaForm` | `motivo`, `informacoesTentativa` com conteúdo até 500 cada, `dataHora` futura, `versao`. O instante pode coincidir com o agendamento anterior. |

Use números JSON, como `1.25`, para quantidades e limites. Aceite a vírgula como separador de entrada em pt-BR, preserve o texto enquanto edita e valide a escala em milésimos inteiros. Serialize a partir do valor validado e teste os extremos `0.001` e `999999999.999`, inteiro com zeros decimais e entradas ambíguas. Em leitura, distinga quantidade efetiva ausente de zero. Use `int` para IDs e versões. Os agregados da API usam `BigDecimal` e as contagens usam `long`. Teste o intervalo do dataset e a precisão em Web. Se agregados além da precisão segura de `num` em Web precisarem de suporte, valide um decodificador decimal que preserve o token numérico antes de converter ou revise o contrato. Nunca derive os totais com aritmética dos itens no cliente. [Forms](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/form)

### DTOs para leitura

| DTO Java · schema | Campos a preservar |
|---|---|
| `MoradorDTO` · `Morador` | `id`, `nome`, `email`, `fone` opcional, `situacao`. |
| `EnderecoDTO` · `Endereco` | `id`, `moradorId` e campos de endereço. `EnderecoPontoDTO` tem apenas os campos de endereço. |
| `MaterialDTO` · `Material` | `id`, `nome`, `descricao`, `unidade`, `orientacao`, `situacao`. |
| `PontoColetaDTO` · `Ponto` | `id`, `nome`, `contato`, `horarios`, `recolhimentoDomiciliar`, `situacao`, `endereco` e `materiaisAceitos`. Contato e horários podem ser nulos. |
| `MaterialAceitoDTO` · `MaterialAceito` | `materialId`, `nomeMaterial`, `unidade`, `orientacao`, `observacoes` e `limite`. A disponibilidade do material é obtida pelo catálogo. |
| `OperadorDTO` · `Operador` | `id`, `nome`, `email`, `pontoId`, `situacao`. |
| `SolicitacaoResumoDTO` · `Solicitacao` | `id`, `protocolo`, `moradorId`, `pontoId`, `enderecoCopia`, `situacao`, `criadaEm`, `agendadaPara`, `concluidaEm`, `observacoes`, `versao`. Datas de agendamento/conclusão e observações podem ser nulas. |
| `SolicitacaoDetalheDTO` · `SolicitacaoDetalhe` | Campos do resumo mais `itens` e `historico`. Cada `ItemDTO` traz `materialId`, `nomeMaterial`, unidade copiada em `unidade`, `quantidadeEstimada` e `quantidadeEfetiva`. Cada `HistoricoDTO` traz `id`, `situacaoAnterior`, `situacaoNova`, `data`, `responsavel` com `tipo` e `id`, `motivo`, `agendamento` e `informacoesTentativa`. |
| `ResumoGerencialDTO` · `ResumoGerencial` | `solicitacoesPorSituacao` traz `situacao` e `total`. `quantidadesRecolhidas` traz `materialId`, `nomeMaterial`, `unidade` e `quantidade`. A soma é da quantidade efetiva não nula em solicitações `CONCLUIDA`. |

Leitores toleram chaves adicionais, nulo ou ausência nos campos opcionais, e sinalizam violação de formato em campo obrigatório. Enum novo desconhecido não habilita ação e aparece com rótulo neutro. Para nomes de pontos, usar resultados já carregados e resolver sob demanda no detalhe, com fallback `Ponto #id` inclusive para `404` público. Evitar consultas por linha. Histórico usa tipo e ID do responsável, e o nome de material que o servidor devolve é o nome atual do cadastro. O endereço e a unidade do item são as cópias preservadas. [DTOs](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/dto) · [Mappers](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/mapper)

Na página de ponto, mantenha os vínculos como dados cadastrados. Para o formulário de solicitação, use a interseção dos vínculos com materiais confirmados ativos pelo catálogo público. Reaproveite a página de ativos já em memória. Para material vinculado que ainda não apareceu no cache, confirme pelo `GET /materiais/{id}` sob demanda ao selecionar, com cache e deduplicação. `404` torna o item indisponível e erro de rede permite tentar confirmar novamente. Ausência numa página parcial do catálogo não comprova inatividade. No detalhe do ponto, o vínculo pode ser lido antes dessa verificação. Para link público com `materialId` vindo da URL, valide a referência no catálogo e ofereça limpar o filtro se o material estiver indisponível. A decisão definitiva para solicitar é do `POST` ou `PUT` de solicitação.

### Transições e operações de solicitação

| Estado atual | Comando | Ator | Estado após sucesso | Campos além de `versao` e detalhes |
|---|---|---|---|---|
| `SOLICITADA` | `PUT` de correção | M próprio | `SOLICITADA` | `enderecoId`, `itens`, `observacoes` opcionais. Revalida RN01. Altera versão e preserva sequência de histórico. |
| `SOLICITADA` | `POST /agendamento` | O do ponto | `AGENDADA` | `dataHora` futura. Histórico guarda a data marcada. |
| `SOLICITADA` | `POST /recusa` | O do ponto | `RECUSADA` | `motivo` obrigatório. |
| `SOLICITADA`, `AGENDADA` | `POST /cancelamento` | M próprio / O do ponto | `CANCELADA` | `motivo` obrigatório. Preserva agendamento se existia. |
| `AGENDADA` | `POST /inicio` | O do ponto | `EM_RECOLHIMENTO` | Apenas a versão. O serviço aceita início antes do horário agendado. |
| `AGENDADA` | `POST /reagendamento` | O do ponto | `AGENDADA` | `dataHora` futura e diferente em instante, `motivo`. Histórico registra a nova data e mantém as anteriores. |
| `EM_RECOLHIMENTO` | `POST /conclusao` | O do ponto | `CONCLUIDA` | `itens` com efetivas explícitas, uma por material do pedido. Data da conclusão e responsável são do servidor. |
| `EM_RECOLHIMENTO` | `POST /falha` | O do ponto | `AGENDADA` | `motivo`, `informacoesTentativa` e `dataHora` futura. Pode coincidir com o instante previamente marcado. |
| `CONCLUIDA`, `RECUSADA`, `CANCELADA` | Consulta | M próprio / O do ponto / A | Igual | Estados finais. |

Os sufixos `POST` são relativos a `/solicitacoes/{id}`. Estado e versão válidos são verificados pelo backend. Use o estado retornado para apresentar ações. Operador precisa estar ativo e vinculado. A atividade do ponto e compatibilidade dos materiais são revalidadas na criação e na correção. As transições existentes continuam possíveis para o operador do ponto, inclusive quando o ponto está inativo em cenários de dados legados ou de teste. `GET` fora do contexto do ator e ações sobre pedido alheio retornam `404`. Ator inválido ou perfil sem permissão recebe `403`. [Enum e regras](https://github.com/wilgnergomes/sistema-coleta-seletiva/tree/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/enumeration) · [Serviço de transições](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/service/TransicaoSolicitacaoService.java)

Exemplos de serialização com IDs ilustrativos. Em testes reais, use o ID de cada recurso, a versão devolvida pelo servidor e uma data futura em relação ao relógio do ambiente.

| Operação | Exemplo de JSON enviado |
|---|---|
| `POST /solicitacoes` | `{"pontoId":12,"enderecoId":34,"itens":[{"materialId":1,"quantidadeEstimada":1.25},{"materialId":2,"quantidadeEstimada":2}]}` |
| `POST /solicitacoes/{id}/agendamento` | `{"dataHora":"2027-02-01T09:00:00-03:00","versao":0}` |
| `POST /solicitacoes/{id}/conclusao` | `{"versao":2,"itens":[{"materialId":1,"quantidadeEfetiva":1.125},{"materialId":2,"quantidadeEfetiva":0}]}` |

### Datas e períodos

- Eventos trafegam em ISO-8601 com offset. Parseie como instante, com aceite de `Z` e de offsets explícitos. Exiba eventos e rótulo de fuso de negócio, inicialmente `America/Sao_Paulo`. Para agendar, combine o dia e a hora de parede nessa zona e serialize o instante em ISO-8601 com `Z` ou offset correto. Use a mesma zona configurada no backend. `DateTime.toIso8601String()` em hora local, sozinho, não acrescenta o offset exigido pelo contrato.
- Inicialize a base IANA ao primeiro uso de datas de negócio. Use a inicialização Web do pacote `timezone` com asset da mesma origem e a inicialização nativa compatível com Flutter nos demais alvos. Reuse a mesma API de conversão no app e mantenha o catálogo público independente do carregamento desses dados. [Inicialização do pacote `timezone`](https://pub.dev/packages/timezone#initialization-for-browser-environment)
- Filtros `de` e `ate` são datas civis `AAAA-MM-DD`. O backend aplica início do dia `de` inclusivo e início do dia seguinte a `ate` exclusivo. Envie a data escolhida sem conversão para UTC. Mostre `Data de criação` na lista. No resumo, a contagem usa criação e as quantidades usam conclusão. Um pedido criado em setembro e concluído em outubro participa de blocos diferentes sob o mesmo filtro.
- Use relógio injetável em testes e mensagem do servidor para a fronteira de data futura. Se uma zona configurada tiver horário local ambíguo ou inexistente, teste o comportamento e solicite uma hora inequívoca. [ADR 0014](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/adr/0014-datas-de-referencia-do-periodo-e-fuso-de-negocio.md)

### Erros, conflitos e tentativas

| HTTP e `codigo` | Comportamento no cliente |
|---|---|
| `400` `VALIDACAO` / `VALOR_INVALIDO` | Mostrar `mensagem`, mapear `erros[].campo` para o formulário ou filtro e manter o rascunho. Tratar chaves como `endereco.cep`, `itens[0].quantidadeEstimada`, `itens`, `dataHora`, `page` ou `ate`. Use o índice da lista capturada no envio. Campo desconhecido aparece no resumo de erros. Enum inválido no corpo pode vir sem campo. |
| `403` `ACESSO_NEGADO` | Informar operação indisponível. Em validação de ator, permitir corrigir seleção. Em detalhe, limpar conteúdo restrito e oferecer retorno. A mensagem é explicativa, sem fazer classificação por texto. |
| `404` `NAO_ENCONTRADO` | Mostrar registro indisponível. Em lista com referência, manter fallback de ID. Endereço de outro morador e solicitação fora do contexto recebem o mesmo tratamento de ausência. |
| `409` `CONFLITO_REGRA` | Exibir mensagem da API e preservar a edição. Em ação de solicitação, reler o detalhe uma vez para detectar mudança de situação, pois a regra pode ser checada antes da versão. Em RN05, oferecer consulta do ponto. Em incompatibilidade, atualizar referências necessárias. |
| `409` `CONFLITO_VERSAO` | Buscar `GET /solicitacoes/{id}`. Atualizar versão, histórico e ações. Preservar valores editados localmente para revisão se a ação continuar válida. Se itens mudaram, reconciliar pelo `materialId` com revisão explícita. Novo envio parte de uma escolha do usuário, com a versão da revisão. |
| `405` / `415` | Código atual devolve `VALIDACAO`. Tratar como incompatibilidade de método ou conteúdo, registrar diagnóstico técnico sem dados do usuário e preservar o rascunho. |
| `500`, outro status ou corpo inválido | Mensagem compreensível e fallback independente de JSON. Evitar mostrar HTML, corpo bruto, stack trace ou dados pessoais. Registrar rota parametrizada, status e latência. |
| Rede, CORS ou prazo | Em GET, oferecer nova tentativa. Em escrita com resultado incerto, manter rascunho e orientar a consultar o registro ou a lista antes de repetir. Navegadores podem apresentar CORS como falha genérica de rede. |

Use `{codigo, mensagem, erros}` com `erros` geralmente vazio em conflitos. Não extraia códigos de frases traduzidas. Começar com 10 s para leitura e 15 s para gravação no demo, ajustáveis após medição. Desabilitar reenvio em andamento. Prazo vencido no cliente pode ocorrer após a gravação do servidor. Criação não tem chave de idempotência, e ações dependem da versão. A regra é confirmar resultado pela leitura e deixar uma repetição consciente com o usuário. Em sucesso `204`, dispensar parsing. [Tratador global](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/src/main/java/br/edu/ifs/coletaseletiva/exception/TratadorGlobalExceptions.java)

## Ambientes e conectividade

### Backend existente e pré-requisitos

Execute Maven e Compose com diretório de trabalho `backend/`, conforme o ADR 0027. Prepare `backend/.env` a partir de `backend/.env.example` ou mova o `.env` local do checkout anterior. São necessários JDK 21, PostgreSQL 17 conforme o projeto e o Maven Wrapper incluído. Para o banco, use uma instalação local ou `docker compose up -d` em `backend/`, com Docker ativo. Para a suíte completa do backend, Docker é necessário mesmo com PostgreSQL local, pois a integração usa Testcontainers. Use um banco fictício dedicado para a apresentação. [README técnico](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/README.md) · [ADR 0027](https://github.com/wilgnergomes/sistema-coleta-seletiva/blob/dfa80517905a834cd1e7c3ca1be73af839f18e4c/backend/docs/adr/0027-backend-em-pasta-propria-no-repositorio.md)

| Passo previsto para executar o backend | PowerShell a partir da raiz | Bash a partir da raiz |
|---|---|---|
| Entrar no diretório técnico | `cd backend` | `cd backend` |
| Subir o Spring em loopback com o seed `demo` | `$env:SERVER_ADDRESS = '127.0.0.1'` e, em seguida, `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"` | `SERVER_ADDRESS=127.0.0.1 ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` |
| Verificar em outro terminal | `curl.exe http://127.0.0.1:8080/actuator/health` | `curl http://127.0.0.1:8080/actuator/health` |

A propriedade `server.address` vale para o processo de apresentação. Para desenvolvimento sem seed, omita o perfil `demo` e use o banco de desenvolvimento correspondente. Se mudar a porta HTTP do Spring, atualize também o upstream do proxy. A variável `DB_PORT` é a porta do PostgreSQL. [Propriedades do Spring Boot](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#application-properties.server.server.address)

### Web em mesma origem

Caddy continua adequado para servir a Web e encaminhar a API com uma única origem. O `WebConfig` atual registra resolução de ator e idioma. A auditoria do código não encontrou configuração de CORS para uso entre origens. O proxy preserva a API existente e evita depender de preflight no cenário principal. Precisa estar disponível junto do Flutter SDK estável fixado e de um navegador compatível. Para servir o build pronto no dia da apresentação, são suficientes o Caddy, a API em execução, o banco fictício e o navegador. [Caddy `handle_path`](https://caddyserver.com/docs/caddyfile/directives/handle_path) · [Flutter Web](https://docs.flutter.dev/platform-integration/web/setup) · [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS)

| Ambiente futuro | Origem que o usuário abre e caminho da API |
|---|---|
| Desenvolvimento Web no PC | A partir de `frontend/`, `flutter run -d web-server --web-hostname=127.0.0.1 --web-port=8081 --dart-define=APP_MODE=demo --dart-define=API_BASE_URL=/api`. O Caddy recebe em `http://127.0.0.1:8000`, encaminha `/api/*` ao Spring em `127.0.0.1:8080` e as rotas de desenvolvimento ao Flutter em `127.0.0.1:8081`. Abrir o endereço do Caddy. Confirmar recursos e WebSockets usados pelo SDK fixado. |
| Demonstração Web no PC | Em `frontend/`, gerar `flutter build web --release --dart-define=APP_MODE=demo --dart-define=API_BASE_URL=/api`, com `--no-web-resources-cdn` quando suportado pelo SDK fixado. Verificar o bootstrap sem internet externa. A configuração futura em `frontend/Caddyfile` serve `build/web` em `http://127.0.0.1:8000` e mantém o encaminhamento de `/api`. |
| Demonstração Web no navegador do celular | PC e telefone na mesma rede controlada. Fazer o Caddy escutar no endereço de rede do PC, por exemplo `192.168.1.50:8000`, e liberar a porta 8000 no firewall para a rede de apresentação. No celular, abrir `http://192.168.1.50:8000`. A base `/api` usa esse mesmo endereço e o Caddy continua acessando o Spring pelo loopback do PC. Conferir IP atual, roteamento e isolamento de clientes Wi-Fi. |
| Android Emulator, app nativo futuro | O alias `10.0.2.2` aponta para o loopback da máquina hospedeira. Usar base absoluta de desenvolvimento, como `http://10.0.2.2:8080` para o Spring ou `http://10.0.2.2:8000/api` para o Caddy. |
| Android físico, app nativo futuro | Usar a URL absoluta do proxy na LAN ou endpoint HTTPS de teste, com acesso de rede aplicável e manifesto `INTERNET`. HTTP de teste depende da política do build nativo. `localhost` no celular aponta para o próprio aparelho. |
| Desktop nativo futuro | Base absoluta de loopback quando rodar na mesma máquina do Spring ou URL HTTPS de teste. No macOS, habilitar o entitlement de cliente de rede quando o alvo for ativado. |

O futuro Caddy usa `handle_path` para remover `/api`, `reverse_proxy` para enviar método, query, corpo e cabeçalhos, e `file_server` para servir o build. Após criar e validar a configuração na etapa 01, execute `caddy run --config Caddyfile` com diretório de trabalho `frontend/`. Habilite compressão de arquivos Web com `encode`. Mantenha respostas restritas fora de cache compartilhado, limite a exposição da demonstração à origem e rede escolhidas e mantenha Spring e PostgreSQL acessíveis apenas onde necessário. O ambiente usa dados fictícios e os cabeçalhos de demonstração podem ser escolhidos pelo cliente. A rede controlada é parte do cenário de apresentação. [Bind do Caddy](https://caddyserver.com/docs/caddyfile/directives/bind) · [Proxy reverso](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) · [Build Web](https://docs.flutter.dev/deployment/web) · [Rede do Android Emulator](https://developer.android.com/studio/run/emulator-networking-address)

Passe `APP_MODE`, `API_BASE_URL` e `BUSINESS_TIME_ZONE` por `--dart-define`. A base relativa é resolvida contra a origem da página na Web, preservando o segmento `/api`. O fuso padrão de negócio é `America/Sao_Paulo`. Valores compilados são visíveis no cliente. Para Web em origens separadas, uma futura política do ambiente precisa autorizar a origem e os métodos `GET`, `POST`, `PUT` e `DELETE`, com preflight para cabeçalhos de ator e corpo JSON. Em HTTPS, mantenha a URL pública da API em HTTPS. O frontend obtém o identificador de criação do corpo JSON. A leitura do cabeçalho `Location` é opcional.

Na primeira integração, confira `health` no PC, `GET /api/pontos-coleta` no navegador, uma leitura restrita com cabeçalhos e `POST`, `PUT` e `DELETE` do roteiro em banco descartável. Na LAN, confirme que o celular carrega o frontend e consulta `/api` pela mesma origem. HTTP em um IP de rede pode limitar a área de transferência do navegador. Nesse caso, o protocolo continua disponível como texto selecionável. Para automação Web, fixe porta e origem de teste do proxy e confirme o modo de lançamento do ChromeDriver compatível com o SDK. [Clipboard na Web](https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/writeText) · [Integração Flutter](https://docs.flutter.dev/testing/integration-tests)

### Pontos de contrato para conferir na primeira integração

| Evidência | Tratamento no frontend |
|---|---|
| Esquema global de segurança do YAML lista dois cabeçalhos, e o resolver dispensa `X-Usuario-Id` para A. | Modele a permissão por operação e ator conforme ADR 0019 e os testes. |
| Leituras opcionais públicas podem devolver `403` com cabeçalhos inválidos, além das respostas publicadas no YAML. | Envie catálogo público sem identidade e mantenha tratamento genérico de `403`. |
| `405` e `415` aparecem no tratador e nos testes, com `VALIDACAO`, fora do catálogo original do ADR. | Trate o status HTTP sem confundir com erro preenchível de campo. |
| `situacao` em POST de ponto e operador é aceita pelo código e documentada, com decisão ainda anotada em `backend/docs/melhorias-pendentes.md`. | Pré-selecione Ativo e mantenha a opção de inativo para A conforme o contrato vigente. |
| Matriz ainda cita em um trecho a cópia de endereço de 400 caracteres. `V2__ampliar_copia_do_endereco.sql` e o YAML mostram 500. | Aceite os dados atuais e permita quebra do texto no detalhe. |
| Conflitos de unidade e RN05 têm `CONFLITO_REGRA`, e a API não expõe metadados estruturados sobre dependências. | Mostre a mensagem e ofereça ação contextual sem extrair contagens ou chaves da frase. |
| A data no protocolo vem do relógio do servidor e seu fuso, conforme `SolicitacaoService`. | Trate `protocolo` como identificador opaco. Para data e ordenação, use `criadaEm`. |
