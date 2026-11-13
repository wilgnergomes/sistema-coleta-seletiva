# Telas e navegação

[Plano mestre](PLANO_MESTRE_FRONTEND.md) · [Contratos, campos e permissões](ARQUITETURA_E_API.md) · [Design system](DESIGN_SYSTEM.md)

As rotas abaixo são da interface. Os métodos HTTP apontam para o contrato em `backend/docs/api/openapi.yaml`. Os formulários referenciam a tabela de campos da integração. Os [wireframes do design system](DESIGN_SYSTEM.md#wireframes-para-aprovação) mostram o catálogo, a fila e o resumo em duas larguras.

## Mapa de destinos

| Área | Destino e rota sugerida | Acesso | Compartilhamento |
|---|---|---|---|
| Pública e morador | `/` abre Pontos de coleta em `/pontos` · detalhe `/pontos/:id` | Qualquer visitante | Mesma rota e mesmos dados públicos. A navegação do morador e as ações de solicitação acompanham o contexto ativo. |
| Pública | `/materiais` · detalhe `/materiais/:id` | Qualquer visitante | Catálogo compartilhado. Editor administrativo usa a mesma apresentação dos dados. |
| Pública | `/cadastro` | Visitante e pessoa que acessou o catálogo | Cadastro de morador. |
| Demonstração | `/demonstracao` | Ambiente de desenvolvimento ou demonstração | Entrada e troca de ator, acessível pelo cabeçalho. |
| Morador | `/morador/inicio` | Morador ativo no modo de demonstração | Início por tarefas. |
| Morador | `/morador/conta` · subseção `/morador/conta/enderecos` | Próprio morador ativo | Dados pessoais e componente de endereços reutilizado na administração. |
| Morador | `/morador/solicitacoes` · `/morador/solicitacoes/:id` | Próprio morador ativo | Lista e detalhe com contexto do morador. |
| Morador | `/morador/solicitacoes/nova` · `/morador/solicitacoes/:id/corrigir` | Próprio morador ativo | Formulário de solicitação com modo de criação ou correção. |
| Operador | `/operador/fila` · `/operador/fila/:id` | Operador ativo do ponto | Lista e detalhe com composição operacional. Ações abrem a partir do detalhe. |
| Administrador | `/administracao/resumo` | Administrador de demonstração | Entrada da área gerencial. |
| Administrador | `/administracao/solicitacoes` · `/administracao/solicitacoes/:id` | Administrador de demonstração | Consulta ampla, com o mesmo detalhe base e sem comandos de situação. |
| Administrador | `/administracao/pontos` · `/administracao/pontos/novo` · `/administracao/pontos/:id` | Administrador de demonstração | Detalhe e editor do ponto com seção de materiais aceitos. |
| Administrador | `/administracao/materiais` · `/administracao/materiais/novo` · `/administracao/materiais/:id` | Administrador de demonstração | Lista e editor. |
| Administrador | `/administracao/operadores` · `/administracao/operadores/novo` · `/administracao/operadores/:id` | Administrador de demonstração | Lista e editor. |
| Administrador | `/administracao/moradores` · `/administracao/moradores/:id` · `/administracao/moradores/:id/enderecos` | Administrador de demonstração | Lista, edição e endereços. O cadastro público atende criação de morador. |

### Menus, histórico e rotas de exceção

- A área pública abre diretamente a pesquisa de pontos e oferece `Materiais`, `Cadastrar morador` e `Demonstração` quando habilitada. O visitante e o morador usam a mesma lista e o mesmo detalhe, com consultas anônimas de catálogo. Com contexto de morador, preserve a navegação da sua área e use o detalhe do ponto como entrada para solicitar recolhimento. Os links públicos continuam acessíveis pelo cabeçalho.
- Morador vê `Início`, `Pontos`, `Solicitações` e `Conta`. `Materiais` fica junto de `Pontos`. Operador entra direto na `Fila do ponto`, com filtros e histórico no próprio detalhe. Administração separa `Resumo`, `Solicitações`, `Pontos`, `Materiais`, `Operadores` e `Moradores`.
- Abrir um detalhe cria uma entrada navegável. Voltar recupera página, filtros e, dentro da sessão, a posição ou o item visível na lista. A URL guarda os filtros aplicados e a paginação. Um filtro ainda em edição permanece no estado da tela até `Aplicar filtros`. Mantenha identidade simulada, observações, endereços e rascunhos fora da URL. Valide parâmetros de rota antes de fazer HTTP e dê precedência às rotas estáticas `nova` e `novo`.
- Um detalhe aberto diretamente busca o registro no contexto atual. A rota restrita sem ator encaminha à seleção de demonstração, quando habilitada, com retorno apenas a rotas internas validadas. Perfil diferente recebe orientação para voltar ao seu espaço ou trocar ator. O modo público oferece retorno a `/pontos`. A API continua aplicando autorização em cada operação.
- Ao trocar ator, encerre o contexto anterior, invalide consultas e rascunhos restritos e volte à página inicial do perfil selecionado. Confirme antes de descartar edição. Após troca de perfil, o histórico do navegador passa novamente pelo guard. Erro de rota desconhecida oferece `Voltar para pontos` ou a seção inicial do perfil. Um `404` de recurso tem mensagem de recurso indisponível, sem distinguir registros alheios.
- Formulários curtos e ações com confirmação abrem diálogo. Em tela estreita ou baixa, use folha rolável ou rota de tela cheia mantendo título e ação. Formulários de cadastro com vários campos são seções de página. O detalhe da fila tem uma rota e se apresenta no painel lateral ou na tela inteira. Redimensionar preserva seleção, filtros e formulário em edição.

### Percursos prioritários

| Ator | Percurso de aceite | Decisão de usabilidade |
|---|---|---|
| Morador | `Pontos` → detalhe → `Solicitar recolhimento` → confirmar pedido | O formulário recebe o ponto já escolhido. Se faltar endereço, cadastre-o dentro da jornada e use o `EnderecoDTO` retornado. Mostre protocolo e histórico no detalhe após o `201`. |
| Operador | `Fila` → abrir pendente → `Agendar` → conferir histórico | Em desktop amplo, fila e detalhe ficam visíveis. No celular, o detalhe abre com ação principal e atalho `Ver histórico`. O botão Voltar restaura a fila sem perder posição ou filtros. |
| Administrador | `Resumo` → aplicar período → abrir solicitações de uma situação | A contagem leva à consulta RF09 com a mesma situação e o mesmo período de criação. Quantidades ficam no resumo com período de conclusão e unidade por linha. Cadastros permanecem no mesmo menu administrativo. |

### Estados comuns

| Estado | Comportamento padrão |
|---|---|
| Primeira leitura | Indicador rotulado e área estável. Uma lista vazia aparece só depois de uma resposta bem-sucedida. |
| Atualização | Preservar o conteúdo anterior ainda autorizado, sinalizar atualização e manter filtros editáveis quando fizer sentido. Ignorar resposta superada por outra consulta. |
| Sem dados | Mensagem contextual. Se houver filtros aplicados, indicar ausência de correspondência e oferecer limpar. Se for falta de cadastro, oferecer a ação permitida para resolver. |
| Gravação | Indicar envio, evitar duplo clique e atualizar a partir da resposta HTTP. Preservar os dados do formulário diante de erro e aplicar a política de conflito documentada. |
| Erro | Campo junto ao rótulo, falha de leitura com nova tentativa, `403` com orientação de contexto, `404` com retorno à lista e `409` com mensagem e revisão. Em detalhe restrito com `403` ou `404`, retirar dados anteriores. |

## Área pública e compartilhada

### Pontos de coleta e detalhe · RF04–RF06

| Item | Especificação |
|---|---|
| Objetivo e acesso | Encontrar um ponto ativo, consultar informações de descarte e descobrir se oferece recolhimento domiciliar. Consulta pública e reaproveitamento na jornada do morador. |
| Lista `/pontos` | Cartões com nome, município e bairro, endereço resumido e rótulo `Recolhimento domiciliar` ou `Entrega no local`. Filtros `nome`, `municipio`, `bairro`, `materialId`, com aplicar, limpar e paginação. Município e bairro exigem nome completo, e acentos contam. O seletor de material consulta apenas o catálogo ativo. Mostre o total da consulta e os filtros aplicados junto aos resultados. |
| Detalhe `/pontos/:id` | Exibir `nome`, endereço embutido, `contato`, `horarios`, recolhimento e vínculos com orientação, observação e limite por unidade. Campos ausentes recebem `Contato não informado` ou `Horários não informados`. Horários são texto. Rótulos de material seguem a confirmação de disponibilidade descrita na integração. |
| Ações | Abrir detalhes, consultar material, voltar aos resultados e, para morador, `Solicitar recolhimento` com o ponto pré-selecionado em `/morador/solicitacoes/nova?pontoId=:id` quando ativo e domiciliar. Em ponto sem recolhimento, mantenha a informação de entrega e o acesso ao catálogo. |
| HTTP e dados | `GET /pontos-coleta`, `GET /pontos-coleta/{id}`, `GET /materiais` para filtro e disponibilidade, `GET /materiais/{id}` sob demanda. `Pagina<PontoColetaDTO>`, `PontoColetaDTO`, `EnderecoPontoDTO`, `MaterialAceitoDTO`, `MaterialDTO`. |
| Carregamento, vazio e erros | Carregar primeiro a lista ou o detalhe. Resolver material sob demanda, sem bloquear endereço e horários. Diferenciar catálogo sem pontos de filtro sem resultado. Em detalhe `404`, informar indisponibilidade e retornar à pesquisa. Se faltarem vínculos, informar `Nenhum material informado neste ponto`. |
| Responsividade | No compacto, nome fica visível com `Buscar` e `Filtros`. `Filtros` reúne município, bairro e material. Buscar aplica o nome junto dos demais filtros já aplicados. No amplo, campos podem compartilhar uma linha. Cartões em coluna ou grade conforme espaço. Contato e horários quebram linha, e o CTA ocupa a largura disponível no detalhe compacto. |
| Aceite | Filtrar por material ativo retorna só pontos ativos vinculados. Paginar, abrir detalhe e voltar preservam filtros. Um ponto inativo não abre no catálogo. O usuário identifica endereço e modalidade de atendimento na lista e encontra horários e orientações no detalhe em 360 e 1366 px. |

### Materiais e detalhe · RF03, RF06

| Item | Especificação |
|---|---|
| Objetivo e acesso | Consultar materiais ativos, unidades e preparo. Público, com acesso a partir do ponto ou menu. |
| Informações e componentes | `/materiais` lista nome, unidade e descrição curta. `/materiais/:id` mostra nome, descrição e orientação de preparo completas. Links a partir dos materiais do ponto levam ao detalhe quando disponível. |
| Formulário e ações | Paginação e navegação para detalhes. Os campos de busca textual são reservados a recursos que a API permite pesquisar. |
| HTTP e dados | `GET /materiais` → `Pagina<MaterialDTO>`, `GET /materiais/{id}` → `MaterialDTO`. Pode abrir `/pontos?materialId=:id` para pesquisar locais. |
| Carregamento, vazio e erros | Indicador na lista e no detalhe. Ausência de ativos recebe mensagem própria. `404` de material inativo ou removido do escopo público retorna ao catálogo. |
| Responsividade | Cartões ou lista em uma coluna com leitura integral no detalhe, sem truncar a orientação. |
| Aceite | `KG`, `L` e `UN` aparecem como `kg`, `L` e `un.`. Material inativo fica indisponível na visão pública, sem quebrar a navegação de pontos. |

### Cadastro de morador · RF01

| Item | Especificação |
|---|---|
| Objetivo e acesso | Criar cadastro de morador. `/cadastro`, público. |
| Informações e componentes | Formulário com `nome`, `email`, `fone` opcional e ação `Cadastrar morador`. No sucesso, mostrar cadastro concluído e próxima ação. |
| Ações e validação | Validar obrigatórios e e-mail, com limites do `MoradorForm`. O usuário pode voltar ao catálogo. Em ambiente demo, oferecer `Usar este cadastro na demonstração` depois de sucesso explícito. |
| HTTP e dados | `POST /moradores` sem identificação, `MoradorForm` / `MoradorEntrada` → `201 MoradorDTO`. Usar o `id` devolvido se o usuário optar pela demonstração. O cadastro nasce ativo. |
| Carregamento, vazio e erros | Tela inicia com campos vazios. Mostrar progresso no envio e preservar campos em `400` ou `409`. Mensagem de duplicidade orienta a revisar o e-mail. |
| Responsividade | Uma coluna no celular e formulário de até 720 dp em tela ampla. |
| Aceite | Um cadastro válido retorna confirmação. Um e-mail duplicado é rejeitado pelo servidor. Nenhum campo de situação aparece no cadastro público. |

### Seleção de identidade para demonstração · RNF06, ADR 0019

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/demonstracao` permite escolher a identidade em ambiente habilitado. O título é `Identidade de demonstração`. Mostrar `Simulação de identidade` também durante o uso. |
| Campos, informações e ações | Seleção de `Morador`, `Operador` ou `Administrador`. Pedir ID positivo apenas para morador ou operador. Mostrar contexto escolhido, `Usar este ator`, `Voltar à consulta pública` e saída do contexto ativo. O roteiro fornece IDs obtidos pela pessoa responsável pela demo. |
| HTTP e dados | Confirmar morador com `GET /moradores/{id}` (`MoradorDTO`), operador com `GET /solicitacoes?situacao=SOLICITADA&page=0&size=20` (`Pagina<SolicitacaoResumoDTO>`, escopo do ator) e administrador com `GET /resumo-gerencial` (`ResumoGerencialDTO`). Reaproveitar a resposta no contexto correspondente. A entrada administrativa é explícita. |
| Carregamento, vazio e erros | Indicador de verificação. Uma fila vazia `200` valida a seleção do operador. `403` permite corrigir perfil ou ID, e falha de rede oferece nova leitura. Valores de cadastro fictício podem chegar a partir do sucesso do cadastro. |
| Responsividade | Formulário curto em coluna, seleção por rótulos e operação inteira por teclado ou toque. |
| Aceite | Cabeçalhos são os do ator selecionado, apenas nas chamadas apropriadas. Alterar ator limpa dados anteriores. A compilação pública sem demo não mostra entrada para áreas restritas. |

## Área do morador

### Início do morador · RF02, RF06, RF08, RF09

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/morador/inicio` apresenta próximas tarefas ao próprio morador ativo. |
| Informações e componentes | Título `O que você quer fazer?`, ações `Encontrar pontos`, `Solicitar recolhimento`, `Minhas solicitações` e `Meus endereços`. Mostrar até três solicitações recentes com protocolo, situação e agendamento quando houver. |
| Formulário, ações e API | Sem formulário. `GET /solicitacoes?page=0&size=3` → `Pagina<SolicitacaoResumoDTO>`. Exibir atalho para detalhe e `Ver todas`. Nome do morador pode vir do resultado já carregado na seleção. |
| Carregamento, vazio e erros | Atalhos aparecem imediatamente. O bloco de recentes carrega e falha isoladamente. Se vazio, mostrar `Você ainda não tem solicitações` com ação de explorar pontos. |
| Responsividade e aceite | Empilhar atalhos e cartões no compacto. Em qualquer tamanho, é possível iniciar um pedido e acompanhar um recente sem entrar na gestão de cadastro. |

### Minha conta e endereços · RF01–RF02

| Item | Especificação |
|---|---|
| Objetivo e acesso | O próprio morador ativo consulta e edita seus dados e endereços em `/morador/conta` e `/morador/conta/enderecos`. A administração reutiliza as seções em sua própria área. |
| Perfil | Nome, e-mail e telefone. Edição completa com `MoradorForm`, sem controle de situação na área do morador. `GET /moradores/{id}` e `PUT /moradores/{id}` → `MoradorDTO`. |
| Endereços | Lista com logradouro, número, complemento quando existir, bairro, município/UF e CEP. Ações `Cadastrar endereço` e `Editar`. Formulário com os sete campos do `EnderecoForm`, incluindo controle `Sem número` que grava `S/N`. CEP pode ter máscara visual e segue como oito dígitos no JSON. |
| HTTP e dados de endereço | `GET /moradores/{moradorId}/enderecos` → `List<EnderecoDTO>`. `POST` no mesmo caminho → `201 EnderecoDTO`. `PUT /moradores/{moradorId}/enderecos/{id}` → `200 EnderecoDTO`. Carregar dados para editar a partir da lista. |
| Carregamento, vazio e erros | Carregar cada seção quando aberta. Para lista vazia, CTA de cadastro. Edição duplicada de e-mail mostra `409` e preserva dados. Endereço ausente orienta voltar e atualizar a lista. |
| Responsividade | Cartões de endereço em celular e formulário em página ou folha rolável. Em espaço amplo, campos curtos podem compartilhar linha. |
| Aceite | O próprio morador atualiza nome, e-mail e fone e gerencia endereços. Endereço incompleto recebe erro no campo. A UI permite voltar ao rascunho de solicitação após cadastrar um endereço. |

### Minhas solicitações e detalhe · RF08–RF09, RF12, RN03–RN06

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/morador/solicitacoes` e `/:id` acompanham pedidos do dono ativo. |
| Lista | Protocolo, situação, datas de criação e agendamento, ponto quando identificado e resumo de endereço quando útil. Formulário opcional de `situacao`, `pontoId`, `de` e `ate`, com paginação e ordem de criação mais recente. Em histórico de ponto indisponível, aceitar filtrar pelo `pontoId` a partir do registro exibido. |
| Detalhe | Protocolo, situação, endereço copiado, observações, datas, ponto com nome conhecido ou ID, itens com unidade e estimada/efetiva, histórico em ordem com tipo e ID do responsável. Protocolo selecionável e botão `Copiar protocolo` quando disponível no navegador. `Corrigir solicitação` só em `SOLICITADA`. `Cancelar solicitação` em `SOLICITADA` ou `AGENDADA`. |
| Formulário de cancelamento | Motivo obrigatório até 500 caracteres e confirmação explícita com o protocolo. Enviar `versao` lida do detalhe. Preservar eventual data agendada no detalhe após cancelamento. |
| HTTP e dados | `GET /solicitacoes` → `Pagina<SolicitacaoResumoDTO>`, `GET /solicitacoes/{id}` → `SolicitacaoDetalheDTO` com `ItemDTO`, `HistoricoDTO`, `ResponsavelDTO`. `POST /solicitacoes/{id}/cancelamento` usa `MotivoForm` → `SolicitacaoDetalheDTO`. |
| Carregamento, vazio e erros | Atualização da lista preserva filtros. Sem pedidos, oferecer `Solicitar recolhimento`. `404` remove o detalhe e permite voltar. `409` segue recarga e revisão. Fim anterior ao início recebe erro no campo. |
| Responsividade | Cartões com protocolo e situação no compacto e tabela quando houver espaço. Histórico vertical e formulário de cancelamento rolável. |
| Aceite | Morador vê somente seus pedidos. Itens e histórico abrem com endereço histórico. Ações aparecem por estado, e uma nova leitura após conflito remove ações que deixaram de ser válidas. |

### Nova solicitação e correção · RF08, RN01, RN02, RN04, RN06

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/morador/solicitacoes/nova` cria pedido do ator. `/morador/solicitacoes/:id/corrigir` reabre o formulário de pedido do próprio morador quando `SOLICITADA`. |
| Campos e componentes | Formulário único em seções `Ponto e endereço`, `Materiais e quantidades` e `Observações`. Criação permite escolher `pontoId` de ponto ativo com recolhimento domiciliar e `enderecoId` do morador. Na correção, ponto fixo, cópia de endereço exibida e seleção explícita do endereço atual. Itens dinâmicos por `materialId` e `quantidadeEstimada`, com unidade, limite e remoção de linha. Observações opcionais até 500. |
| Ações e validação | Permitir cadastrar endereço dentro do fluxo, selecionar o endereço criado e manter os demais campos. Mostrar a unidade e o limite junto à quantidade. Na mudança de ponto, sinalizar itens que exigem revisão. Oferecer materiais ativos vinculados, quantidade positiva no formato da API e `UN` inteira. Evitar repetição. Se um item anterior ficou indisponível, preservá-lo visível para decisão explícita de remover ou ajustar. |
| HTTP e dados | `GET /pontos-coleta` e `GET /pontos-coleta/{id}`, `GET /materiais` ou `GET /materiais/{id}`, `GET` e `POST /moradores/{moradorId}/enderecos`. Correção lê `GET /solicitacoes/{id}`. Criação usa `POST /solicitacoes`, `SolicitacaoForm` / `SolicitacaoEntrada` → `201 SolicitacaoDetalheDTO`. Correção usa `PUT /solicitacoes/{id}`, `SolicitacaoCorrecaoForm` / `SolicitacaoCorrecao` com `versao` → `200 SolicitacaoDetalheDTO`. |
| Carregamento e vazio | Carregar ponto e endereços de forma independente. Se faltarem endereços, abrir cadastro. Se faltarem materiais disponíveis ou pontos domiciliares, orientar a voltar ao catálogo e manter o rascunho enquanto a sessão permitir. |
| Erros | Campos próximos à mensagem, conflito de regra com revisão dos dados de referência, versão com recarga e revisão explícita, e resultado incerto com orientação para consultar `Minhas solicitações`. |
| Responsividade | Uma coluna e cartões de item em celular. Em área ampla, quantidade e unidade podem ficar na mesma linha. Usar o próprio formulário como revisão antes de enviar. O campo focado, o erro e o envio devem continuar alcançáveis com teclado virtual ou zoom. |
| Aceite | Enviar dois materiais compatíveis devolve protocolo e histórico inicial. Incompatibilidade é rejeitada sem gravação parcial. Correção altera cópias e itens com ponto preservado e versão retornada. Nenhuma linha de histórico de situação é criada pela correção. |

## Área do operador

### Fila do ponto · RF07, RF09

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/operador/fila` é a área de trabalho do operador ativo. A API aplica o vínculo de ponto. |
| Informações e componentes | Protocolo, situação, ponto identificado quando houver, datas e endereço copiado. Filtro rápido inicia em `Solicitada`, com opção `Todas` e as seis situações individuais. Período é de criação. Exibir total da consulta pelo `totalElementos`. |
| Campos e ações | `situacao` com seleção única. `de` e `ate` ficam em `Filtros de data` no compacto. Oferecer limpar, paginar, atualizar e abrir detalhe. O ponto é determinado pelo contexto da API. Nome do ponto é exibido quando resolvido, com fallback para o ID. |
| HTTP e dados | `GET /solicitacoes` → `Pagina<SolicitacaoResumoDTO>` com ator operador. Se a verificação inicial já trouxe uma página `SOLICITADA` com a mesma consulta, reaproveitá-la. |
| Carregamento, vazio e erros | Estado local da lista. Para fila sem pendências, oferecer `Ver todas`. Se vínculo ou ator não estiver habilitado, orientar a revisar a identidade. Ponto indisponível no catálogo não bloqueia a leitura das solicitações do contexto. |
| Responsividade e aceite | Em compacto, lista de cartões que abre detalhe em tela inteira. Em amplo, painel com lista e detalhe. Operador de outro ponto não encontra os pedidos e não amplia seu alcance por filtros ou URL. |

### Detalhe operacional e formulários de ação · RF09–RF14

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/operador/fila/:id` reúne decisão e execução para operador ativo do ponto. |
| Informações | Protocolo, situação e endereço copiado no topo. Depois, itens e unidades, observações, datas e histórico. `Ver histórico` navega dentro do detalhe e leva o foco à seção. O operador pode ver o ID do morador quando útil. Nome e telefone ficam fora dessa experiência conforme a permissão da API. |
| Ações por estado | Em `SOLICITADA`, oferecer `Agendar`, `Recusar` e `Cancelar`. Em `AGENDADA`, oferecer `Iniciar recolhimento`, `Reagendar` e `Cancelar`. Em `EM_RECOLHIMENTO`, oferecer `Concluir recolhimento` e `Registrar falha`. Situações finais apresentam dados e histórico. Agendar, iniciar ou concluir são primárias quando disponíveis. |
| Formulários | Agendar pede data e hora. Recusar e cancelar pedem motivo. Iniciar pede confirmação com protocolo. Concluir pede uma quantidade efetiva explícita por item, estimada separada e unidade copiada. Reagendar pede nova data e hora com motivo. Registrar falha pede motivo, informações da tentativa e nova data e hora. A `versao` vem do detalhe. |
| HTTP e dados | `GET /solicitacoes/{id}` e as sete ações `POST /solicitacoes/{id}/…` na tabela de transições. `AgendamentoForm`, `MotivoForm`, `VersaoForm`, `ConclusaoForm` com `ItemConclusaoForm`, `ReagendamentoForm`, `FalhaForm` → `SolicitacaoDetalheDTO`. |
| Carregamento e estados vazios | Ações aguardam a primeira leitura do detalhe. Durante envio, manter o contexto e desabilitar ações concorrentes no mesmo registro. Se a ação retirar o item do filtro atual, atualizar a fila e manter o detalhe com novo estado até o operador voltar. |
| Validações e erros | Agendamento futuro, motivo não vazio, efetivas não negativas com pelo menos uma positiva, unidade `UN` inteira. `409` recarrega estado e versão. `404` de outro ponto tem a mesma apresentação de registro ausente. O relógio do servidor decide se a data é futura. |
| Responsividade | Ação principal visível perto da situação. No compacto, ações secundárias ficam em `Mais ações`. No amplo, exiba o grupo de ações quando houver largura. Conclusão é formulário rolável com linha por item. Fila e detalhe mantêm foco e rolagem independentes no amplo. |
| Aceite | Operador agenda ou recusa uma pendente, inicia uma agendada mesmo antes da hora se decidir fazê-lo, conclui com efetivas ou registra falha e reagenda. A data e o autor da conclusão vêm da resposta. O histórico preserva tentativas, motivos e agendamentos anteriores. |

## Área administrativa

### Resumo gerencial · RF15

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/administracao/resumo` apresenta indicadores oficiais ao administrador. |
| Campos e informações | `de` e `ate` opcionais, inicialmente todo o período. Seis situações com total e rótulo `Solicitações criadas no período`. Quantidades com `nomeMaterial`, `unidade` e `quantidade`, rótulo `Recolhido em atendimentos concluídos no período`. Usar o fuso de negócio como referência. |
| Componentes e ações | Seis linhas de situação com números e barras discretas. Cada linha dá acesso à consulta de solicitações da situação com o período de criação aplicado. Tabela ou lista para quantidade por material e unidade. `Aplicar período` e `Limpar período` atualizam os dois blocos. Mostre o período aplicado em texto. |
| HTTP e dados | `GET /resumo-gerencial?de=…&ate=…` → `ResumoGerencialDTO`, `SituacaoTotalDTO`, `QuantidadeRecolhidaDTO`. |
| Carregamento, vazio e erros | Uma consulta atualiza os dois blocos. Se todas as situações tiverem zero, mostrar seis zeros. Quantidades podem vir vazias independentemente. Período invertido recebe erro em `ate`, rede falha com nova tentativa. |
| Responsividade | Em celular, um controle de período com filtros de data acessíveis e blocos empilhados. Em desktop, filtros visíveis e dois painéis quando couberem. Mantenha as datas de referência no cabeçalho de cada bloco nas duas composições. |
| Aceite | Comparar a tela com o JSON, inclusive zeros, estimadas diferentes das efetivas e limite de dia. Nenhuma soma mistura kg, litros e peças. |

### Consulta administrativa de solicitações · RF09

| Item | Especificação |
|---|---|
| Objetivo e acesso | `/administracao/solicitacoes` e `/:id` permitem ao administrador consultar todos os registros. |
| Informações, formulários e ações | Lista e detalhe base com `situacao`, `pontoId`, `de`, `ate` e paginação, sem formulários de transição. Pontos do seletor incluem inativos. Detalhe apresenta endereço copiado, itens e histórico e pode abrir o cadastro autorizado do morador ou operador a partir do ID. |
| HTTP e dados | `GET /solicitacoes`, `GET /solicitacoes/{id}`, `GET /pontos-coleta` para o seletor administrativo. `Pagina<SolicitacaoResumoDTO>`, `SolicitacaoDetalheDTO`, `PontoColetaDTO`. Cadastros vinculados são carregados somente ao abrir. |
| Carregamento, vazio e erros | Usar estados da lista e detalhe. Uma busca sem correspondência permite limpar filtros. `404` do cadastro associado não substitui o detalhe da solicitação. |
| Responsividade | Tabela onde couber, cartões rotulados no compacto. Linha do tempo conserva leitura de motivos longos. |
| Aceite | Consulta de escopo global paginada e filtrável. Retornar ao detalhe após abrir cadastro preserva contexto. As transições permanecem nas experiências dos atores autorizados. |

### Pontos e materiais aceitos · RF04–RF06, RN05

| Item | Especificação |
|---|---|
| Objetivo e acesso | Administrador lista em `/administracao/pontos` e abre `novo` ou `:id` para cadastro e vínculos. |
| Lista e filtros | Nome, município, situação, recolhimento domiciliar, ações e filtros `nome`, `municipio`, `bairro`, `materialId`, `situacao` mais paginação. |
| Editor | `nome`, `contato`, `horarios`, `recolhimentoDomiciliar`, `situacao` e `endereco` com campos do `EnderecoForm`. Situação inicia em Ativo na UI. `PUT` envia a representação completa e sempre inclui o booleano de recolhimento. Confirmar inativação. |
| Materiais aceitos | No detalhe persistido, seção com `materialId`, nome, unidade, orientação, `observacoes` e `limite`. Adicionar vínculo com seletor administrativo paginado de materiais. Editar observações e limite opcional. Remover com confirmação que identifica material e ponto. Vínculos de material inativo continuam gerenciáveis. |
| HTTP e dados | `GET` e `POST /pontos-coleta`, `GET` e `PUT /pontos-coleta/{id}` com `PontoColetaForm`, `EnderecoForm` e `PontoColetaDTO`. `GET /materiais` para opções. `PUT /pontos-coleta/{pontoId}/materiais/{materialId}` usa `VinculoForm` e devolve `MaterialAceitoDTO`. `DELETE` no mesmo caminho devolve `204`. |
| Carregamento, vazio e erros | Indicador no editor e seletor. Lista vazia oferece cadastrar ponto, seção vazia oferece vincular material. Em conflito de regra ao inativar ou remover vínculo, apresentar mensagem do servidor e atalho `Ver solicitações do ponto`. A lista abre com filtro de ponto e permite consultar uma situação por vez. A remoção de vínculo pode depender de itens que só aparecem no detalhe de cada solicitação. |
| Responsividade | Cartões no compacto, editor em seções e vínculos abaixo dos dados do ponto. Painel amplo pode dividir dados e vínculos, mantendo o formulário em largura legível. |
| Aceite | Cadastrar um ponto e associar dois materiais. Editar o mesmo vínculo mantém uma linha. Inativação e remoção afetadas por solicitação aberta são rejeitadas e a UI mantém o estado confirmado. |

### Materiais administrativos · RF03, ADR 0013

| Item | Especificação |
|---|---|
| Objetivo e acesso | Administrador gerencia catálogo em `/administracao/materiais`, `novo` e `:id`. |
| Informações e campos | Lista paginada com nome, unidade, situação e filtro `situacao`. Editor com `nome`, `descricao`, `unidade`, `orientacao`, `situacao`. Descrição e orientação obrigatórias. Situação inicial proposta Ativo, com escolha explícita do administrador. |
| Ações e HTTP | Criar com `POST /materiais`, consultar com `GET /materiais` e `GET /materiais/{id}`, editar ou inativar com `PUT /materiais/{id}` e confirmação. `MaterialForm` → `MaterialDTO`, `Pagina<MaterialDTO>` na lista. |
| Carregamento, vazio e erros | Estado vazio com ação de cadastro, resultados filtrados com limpar. `409` de nome repetido ou troca de unidade bloqueada exibe a mensagem e mantém os dados editados. |
| Responsividade e aceite | Editor com textos longos em largura legível. Após inativação o catálogo público deixa de listar o material. Dependências impedem troca de unidade e os demais campos podem ser salvos com a unidade original. |

### Operadores · RF07

| Item | Especificação |
|---|---|
| Objetivo e acesso | Administrador gerencia operadores em `/administracao/operadores`, `novo` e `:id`. |
| Informações e campos | Lista paginada com nome, e-mail, ponto e situação. Filtro `pontoId`. Formulário com `nome`, `email`, `pontoId` e `situacao`. Seletor de ponto administrativo pesquisável por nome e paginado, incluindo inativos com rótulo. |
| Ações e HTTP | `GET` e `POST /operadores`, `GET` e `PUT /operadores/{id}`, `GET /pontos-coleta`. `OperadorForm` → `OperadorDTO`, `Pagina<OperadorDTO>` na lista. Confirmar inativação ou alteração de vínculo quando houver. |
| Carregamento, vazio e erros | Preservar filtro de ponto ao voltar da edição. Lista vazia pode limpar filtro ou cadastrar. E-mail duplicado `409`, ponto ausente `404`, validações `400` junto aos campos. |
| Responsividade e aceite | Cartão identifica ponto e situação em tela estreita. Dois operadores podem compartilhar ponto. Alterar vínculo muda a fila visível para o operador conforme a API, em nova leitura. |

### Moradores administrativos e endereços · RF01–RF02

| Item | Especificação |
|---|---|
| Objetivo e acesso | Administrador consulta `/administracao/moradores`, edita `:id` e gerencia `:id/enderecos`. |
| Informações e campos | Lista paginada com nome, e-mail, telefone quando houver e situação. A API fornece `page` e `size`. Editor com os campos do perfil mais `situacao`. Seção de endereços usa os campos e componentes descritos em Minha conta. |
| Ações e HTTP | `GET /moradores`, `GET` e `PUT /moradores/{id}`, `GET` e `POST /moradores/{moradorId}/enderecos`, `PUT /moradores/{moradorId}/enderecos/{id}`. `MoradorForm`, `MoradorDTO`, `Pagina<MoradorDTO>`, `EnderecoForm`, `EnderecoDTO`. Link `Cadastrar morador` reaproveita o cadastro público. Confirmar inativação. |
| Carregamento, vazio e erros | Seções têm carregamento próprio. Sem endereços, oferecer cadastro. Tratar duplicidade de e-mail, acesso e endereço inexistente junto da ação correspondente. |
| Responsividade e aceite | Cartões e controles de paginação legíveis no celular. Administrador altera a situação de um morador e gerencia seus endereços. O contexto de morador ativo continua isolado ao próprio cadastro. |
