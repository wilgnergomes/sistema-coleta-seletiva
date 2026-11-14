# Progresso da implementação do frontend

Registro de continuidade da implementação integral pela IA, autorizada pelo
estudante em 13/11/2026. Cada etapa nasce do último commit validado da etapa
anterior (branches cumulativas, `CLAUDE.md`).

## Estado atual

| Item | Valor |
|---|---|
| Branch atual | `frontend/15-interacoes-e-navegacao` |
| Base da etapa | `frontend/14-experiencia-publica` em `fa844bd` |
| Etapa em andamento | Nenhuma: interações e navegação concluídas, aguardando revisão do estudante |
| Próxima ação | Revisão do estudante comparando as branches; a etapa 12 (nativa) é opcional e depende de Android SDK e Visual Studio C++, não instalados |

## Ambiente

| Ferramenta | Versão | Origem |
|---|---|---|
| Flutter | 3.50.0 stable, Dart 3.14.0, DevTools 2.61.0 | Zip oficial `storage.googleapis.com/flutter_infra_release`, SHA-256 conferido, em `%USERPROFILE%\dev\flutter` |
| Caddy | 2.11.9 | Release oficial no GitHub, SHA-512 conferido, em `%USERPROFILE%\dev\caddy` |
| Chrome / Edge | 157.0.8103.51 / 155.0.4283.62 | Já instalados |
| JDK | Temurin 21.0.12.1 | Já instalado |
| Docker / PostgreSQL | Container `coleta-seletiva-db` (PostgreSQL 17) | Já existente, preservado |

Os dois diretórios foram acrescentados ao `Path` do usuário. A análise anônima do
Flutter e do Dart foi desativada. `flutter doctor -v`: Web e Chrome sem pendências.
Android SDK e workload C++ do Visual Studio ausentes, sem instalação, pois os alvos
nativos não fazem parte da entrega Web.

Banco da demonstração: `coleta_frontend_demo`, criado no mesmo container com
`createdb`, isolado dos bancos existentes (`coleta_seletiva`, `coleta_desempenho`,
`coleta_desempenho_xl`). O backend roda com `DB_NAME=coleta_frontend_demo`,
`SERVER_ADDRESS=127.0.0.1` e perfil `demo`.

## Etapas

| Etapa | Branch | Situação | Último commit |
|---|---|---|---|
| 01 | `frontend/01-estrutura` | Concluída e publicada | `d821e43` |
| 02 | `frontend/02-design-system` | Concluída e publicada | `03c9f94` |
| 03 | `frontend/03-catalogo-publico` | Concluída e publicada | `ec5e534` |
| 04 | `frontend/04-morador-e-enderecos` | Concluída e publicada | `742a9a9` |
| 05 | `frontend/05-solicitacoes-morador` | Concluída e publicada | `3a7fbcc` |
| 06 | `frontend/06-fila-e-agendamento` | Concluída e publicada | `f7ee21d` |
| 07 | `frontend/07-execucao-e-reagendamento` | Concluída e publicada | `74677c4` |
| 08 | `frontend/08-resumo-e-consulta-admin` | Concluída e publicada | `b9dbd2d` |
| 09 | `frontend/09-materiais-pontos-e-vinculos` | Concluída e publicada | `9b4226f` |
| 10 | `frontend/10-moradores-e-operadores-admin` | Concluída e publicada | `04dca01` |
| 11 | `frontend/11-qualidade-e-entrega-web` | Concluída e publicada | último commit da branch |
| 12 | `frontend/12-compatibilidade-nativa` | Não iniciada (opcional) | — |
| 13 | `frontend/13-refinamento-visual` | Concluída e publicada | `8850e0d` |
| 14 | `frontend/14-experiencia-publica` | Concluída e publicada | `fa844bd` |
| 15 | `frontend/15-interacoes-e-navegacao` | Concluída e publicada | último commit da branch |

## Etapa 01 · estrutura

Feito:

- Projeto Flutter só com alvo Web em `frontend/`, pacote `coleta_seletiva_web`.
- Os cinco documentos de planejamento em `frontend/docs/`.
- `ConfiguracaoApp` (`APP_MODE`, `API_BASE_URL`, `BUSINESS_TIME_ZONE`) com resolução da base relativa contra a origem da página.
- `ClienteApi` com acesso público ou de ator, prazos de 10 s (leitura) e 15 s (gravação), leitura UTF-8, falhas tipadas (`ErroHttp`, `FalhaRede`, `FalhaPrazo`, `RespostaInvalida`) e `Pagina<T>`.
- Página de estado da conexão, que lê `GET /api/pontos-coleta?page=0&size=1`.
- `Caddyfile` (build release e `/api`) e `Caddyfile.dev` (`flutter run` e `/api`).

Decisões:

- O Flutter 3.50 separou o Material no pacote `material_ui`. O app importa `package:material_ui/material_ui.dart`; `package:flutter/material.dart` está depreciado.
- O pacote `flutter_localizations` foi removido: `material_ui` já exporta `GlobalMaterialLocalizations`, e manter os dois gera importação ambígua.
- O corpo das respostas é decodificado como UTF-8 a partir dos bytes. O Spring envia `application/json` sem `charset`, e o pacote `http` usaria Latin-1.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `dart format --output=none --set-exit-if-changed .` | Sem alterações |
| `flutter analyze` | Nenhum problema |
| `flutter test` | 19 testes, todos aprovados |
| `flutter build web --release --no-web-resources-cdn` | Concluído em cerca de 32 s |
| Navegador (painel do app, Chromium) em `http://127.0.0.1:8000` | Leitura real `GET /api/pontos-coleta?page=0&size=1` → 200, 4 pontos ativos |
| Falha de conexão | Mensagem exibida com ação de nova tentativa (observada quando a base estava incorreta) |

Piso de transferência do build mínimo (Caddy com zstd, medido com `curl`):

| Recurso | Bytes comprimidos |
|---|---:|
| `canvaskit/chromium/canvaskit.wasm` | 2.142.349 |
| `main.dart.js` | 690.000 |
| `Roboto-Regular.ttf` (fallback local) | 92.316 |
| `canvaskit.js`, bootstrap, `index.html`, ícones, manifesto | 37.286 |
| Resposta da API | 421 |
| **Total** | **2.962.372 (≈ 2,83 MiB)** |

Tempo observado: com cache quente, no loopback e sem limitação de rede, a
resposta da API terminou 192 ms após a navegação. Os perfis de referência (10 Mbps
e 4 Mbps com CPU desacelerada) ainda não foram medidos.

Problema encontrado: no Git Bash, `--dart-define=API_BASE_URL=/api` vira
`C:/Program Files/Git/api` pela conversão de caminhos do MSYS. O build passou a ser
feito no PowerShell, e o README registra o cuidado.

## Etapa 02 · design system

Feito:

- Tokens de cor, espaço, raio e largura (`lib/app/tema/tokens.dart`) e tema Material 3 (`lib/app/tema/tema.dart`) com Inter local, anel de foco `#1C5FB8` de 2 dp nos botões, estados de hover, pressionado e desabilitado, e `ThemeExtension` das situações.
- Inter 4.1 (OFL) reduzida ao conjunto latino com `pyftsubset` (fontTools 4.60.1 em venv temporário): 64 KB por peso, contra 411 KB do original. A licença está em `assets/fonts/Inter-LICENSE.txt`.
- Cores de materiais pela Resolução CONAMA 275/2001 (papel azul, plástico vermelho, vidro verde, metal amarelo, pilhas laranja) só em ícones e marcadores, sempre com o nome. A logomarca combina as quatro cores com o centro verde.
- Componentes em `lib/ui/compartilhado/`: shell adaptativo (barra inferior, gaveta, cabeçalho, trilho e navegação lateral), estados de carregamento, vazio, erro e aviso, selos de situação, paginador, campos com obrigatório e erro do servidor, diálogos de confirmação e formulário, layout de página e grade.
- `Carga<T>` com geração, para descartar respostas obsoletas após troca de filtro ou de ator.
- go_router com um shell público inicial.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 38 testes aprovados (19 novos): contraste calculado dos tokens, situações e ícones; limiares 600/1000 dp; composições a 360 e 1366 px; gaveta do administrador; texto a 200% sem transbordo; estado preservado ao redimensionar; `androidTapTargetGuideline`, `labeledTapTargetGuideline` e `textContrastGuideline`; descarte de resposta obsoleta |
| Build release | Concluído |
| Navegador a 360 × 740 | Inter e logomarca renderizadas; leitura real via `/api` |

## Etapa 03 · catálogo público

Feito:

- `/pontos` com barra de pesquisa larga (busca, nome e ícone de filtros dentro da barra), atalhos circulares de Papel, Plástico, Vidro e Metal, acesso a `Outros`, filtros secundários em folha inferior (nome, município, bairro, material), filtros aplicados removíveis, total da consulta, grade de cartões e paginação. Filtros e página ficam na URL (`/pontos?municipio=Lagarto&materialId=1&pagina=2`).
- A API aceita um único `materialId`, então a seleção de material é única. Pesquisa por vários materiais (condição E) fica como melhoria futura da API.
- `/pontos/:id` com endereço, horários, contato, modalidade e materiais aceitos (orientação, observação e limite com unidade). Campos ausentes recebem `Contato não informado` e `Horários não informados`. Material vinculado fora do catálogo ativo aparece como indisponível. A página de 100 materiais ativos é reaproveitada e, quando completa, dispensa consultas por material.
- `/materiais` e `/materiais/:id` com orientação completa e atalho `Ver pontos que aceitam`.
- Repositório com cache em memória limitado (30 páginas, 1 minuto), deduplicação de leituras de material e invalidação.
- `Quantidade` em milésimos inteiros, com leitura pt-BR (vírgula ou ponto, até 9 + 3 dígitos, inteiro em `UN`) e formatação com unidade.
- Rota desconhecida ou ID inválido mostram `Página não encontrada` com `Voltar para pontos`. `GoRouter.optionURLReflectsImperativeAPIs` mantém a URL do detalhe aberto por `push`.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 50 testes aprovados (12 novos): filtros na URL, quantidades, total, modalidade e paginação a 360 px, busca preservando filtros, atalho com um único `materialId`, vazio com e sem filtros, erro com nova tentativa, detalhe com padrões, limite e material inativo sem consulta extra, 404 |
| Navegador a 360 × 740 com API real | Catálogo com 4 pontos, atalho Papel → `?materialId=1` com 3 resultados, detalhe aberto e `Voltar` |
| Navegador a 1366 × 768 | Grade de 3 colunas, navegação lateral, `scrollWidth` igual a `clientWidth` (sem rolagem horizontal) |
| Chamadas na primeira tela | `GET /api/pontos-coleta?page=0&size=20` e `GET /api/materiais?page=0&size=100` |

Transferência da primeira tela real (comprimida): 3.234.108 bytes (≈ 3,08 MiB), contra 2,83 MiB do
piso da etapa 01. A diferença vem do código (`main.dart.js` de 690 KB para 986 KB) e das duas fontes
Inter (64 KB). Fica dentro do orçamento exploratório de 3,5 MiB, e o motor CanvasKit (2,04 MiB)
continua dominante.

Problema encontrado e corrigido: o Caddy servia `main.dart.js` sem `Cache-Control`, e o navegador
aplicou cache heurístico, carregando um build antigo. O `Caddyfile` agora envia `Cache-Control:
no-cache` nos arquivos estáticos (revalidação com `ETag`).

## Etapa 04 · morador e endereços

Feito:

- `Sessao` em memória com o ator simulado (`X-Perfil`, `X-Usuario-Id`), geração, endereço de referência e limpeza de caches na troca. O `ClienteApi` lê os cabeçalhos da sessão só nas chamadas de ator; o catálogo continua público.
- `/demonstracao` (`Identidade de demonstração`): perfil, identificador para morador e operador, aviso de simulação e validação pela primeira chamada do perfil (`GET /moradores/{id}`, `GET /solicitacoes?situacao=SOLICITADA&page=0&size=20` ou `GET /resumo-gerencial`). A resposta é guardada para reaproveitamento. Morador inativo é recusado.
- Guard central: rota restrita sem ator vai para `/demonstracao?retorno=…`. Perfil diferente vai para `/acesso-restrito`. Na compilação pública (`APP_MODE=publico`), as rotas restritas e a demonstração voltam a `/pontos`. O retorno aceita só rota interna do mesmo perfil.
- Shell por contexto: público com cabeçalho e menu, morador com barra inferior de quatro destinos (Início, Pontos, Solicitações, Conta), operador com cabeçalho e administrador com gaveta, todos com trilho ou navegação lateral no amplo. Faixa `Simulação de identidade · Morador #3 · Maria Silva` com `Trocar`.
- `/cadastro`: `MoradorForm` sem situação, validação local, `409` de e-mail duplicado preservando os campos, confirmação e `Usar este cadastro na demonstração`.
- `/morador/inicio`: seletor compacto do endereço de referência (cartão arredondado com folha de escolha e `Gerenciar endereços`), saudação, barra de pesquisa larga, `Pontos em [município]` com `Ver todos em [município]` e atalhos por material. Não há referência a localização: o endereço só define o município da pesquisa. Sem endereço, a pesquisa funciona sem município.
- `/morador/conta`: dados pessoais com edição (`PUT /moradores/{id}`, sem situação), acesso aos endereços e configurações (respeito à escala de texto, ao zoom e à redução de movimento; troca de identidade).
- `/morador/conta/enderecos`: lista e formulário com os sete campos do `EnderecoForm`, `Sem número (S/N)`, UF em maiúsculas, CEP com máscara aceita e enviado com 8 dígitos, erros do servidor por campo. O componente é reutilizável pela administração.
- Detalhe do ponto mostra `Solicitar recolhimento` só para o morador e só em ponto ativo com recolhimento domiciliar.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 64 testes aprovados (14 novos): cabeçalhos por perfil, invalidação na troca, área pela rota, retorno seguro, guard sem ator, perfil diferente, modo público, entrada pela demonstração com cabeçalhos, morador inativo, validação do operador, cadastro com `409` e sucesso, validação local, endereço S/N e erro do servidor, CEP incompleto |
| Navegador a 360 × 740 com API real | `/morador/inicio` sem ator → demonstração; morador 3 → início com `Avenida Principal, S/N · Lagarto`; atalho Papel → `/pontos?municipio=Lagarto&materialId=1` com 1 resultado; Conta com `GET /api/moradores/3` |

Correção: a logomarca no cabeçalho transbordava com fonte larga; o nome agora encolhe com reticências.

Limitação temporária desta etapa: os perfis operador e administrador já são validados, mas suas áreas
chegam nas etapas 06 e 08.

## Etapa 05 · solicitações do morador

Feito:

- `FusoNegocio` com a base IANA (`timezone`, `latest_10y`) por importação adiada: o build gera `main.dart.js_1.part.js` (≈ 210 KB sem compressão), baixado só no primeiro uso de datas de negócio. Eventos exibidos em `America/Sao_Paulo`; agendamentos combinam dia e hora de parede na zona e são enviados como instante UTC (`…Z`). Horário inexistente ou ambíguo é recusado. Filtros de período usam datas civis `AAAA-MM-DD`.
- Modelos e `RepositorioSolicitacoes` com criação, correção e as sete ações de transição (usadas a partir da etapa 06).
- `/morador/solicitacoes`: filtro rápido de situação, período de criação, total, cartões e paginação na URL.
- `/morador/solicitacoes/:id`: protocolo selecionável e `Copiar protocolo`, situação, endereço copiado, ponto, datas, observações, itens com unidade copiada (estimada e recolhida; ausente diferente de zero) e histórico com responsável, motivo, data marcada e tentativa. `Corrigir solicitação` em `SOLICITADA`; `Cancelar solicitação` em `SOLICITADA` ou `AGENDADA`, com motivo obrigatório e confirmação que identifica o protocolo.
- `/morador/solicitacoes/nova?pontoId=` e `/:id/corrigir`: seções Ponto e endereço, Materiais e quantidades, Observações. Materiais do ponto confirmados como ativos, sem repetição, unidade e limite junto da quantidade, `UN` inteira, cadastro de endereço dentro da jornada. Na correção, o ponto é fixo, a cópia do endereço é exibida e a escolha do endereço atual é explícita.
- Política de conflito (RN06): ação com `409` relê o detalhe e atualiza situação, versão e ações; `CONFLITO_VERSAO` na correção relê e preserva a edição para novo envio consciente; `CONFLITO_REGRA` atualiza ponto e catálogo; falha de rede em gravação orienta a conferir a lista antes de repetir.
- Nomes de pontos: uma leitura pública de até 100 pontos ativos alimenta os rótulos; pontos inativos aparecem como `Ponto #id` e o detalhe tenta resolver o nome sob demanda.
- Início do morador: `Pontos usados recentemente` (até dois, sem repetição, só ativos com recolhimento domiciliar, filtrados pelo município do endereço selecionado; sem resultado, a seção não aparece) e `Solicitações recentes` (três), ambos a partir de uma única página de 20 solicitações e no máximo 4 leituras de ponto com cache. Limitação documentada: pontos usados antes das 20 solicitações mais recentes não entram.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `dart format --set-exit-if-changed`, `flutter analyze` | Sem alterações; nenhum problema |
| `flutter test` | 77 testes aprovados (13 novos): fuso e serialização, data civil, filtro `TODAS`, pontos recentes (repetição, limite, 404, município, teto de consultas), criação com JSON exato, `UN` fracionária, limite, erro `itens[0]` na linha, correção com `409 CONFLITO_VERSAO` e reenvio com versão nova, ações por situação, cancelamento, `409 CONFLITO_REGRA` com releitura, 404 |
| Navegador a 1366 × 768 com API real | Morador 3: nova solicitação na Cooperativa Verde Vida (Papel 3,5 kg) → `201`, protocolo `SOL-20261113-M6HPRU`, horário 04:04 em UTC-03; motivo vazio recusado; cancelamento → `Cancelada` e ações retiradas |

Problema de teste encontrado: um `Future` concluído fora da zona do `testWidgets` não avança no tempo
simulado. Os testes de widget criam o próprio `FusoNegocio`.

## Etapa 06 · fila e agendamento

Feito:

- `/operador/fila` e `/operador/fila/:id`, com filtros na URL e situação inicial `Solicitada` (`Todas` vira `situacao=TODAS` para não voltar ao padrão). As duas rotas compartilham a chave de página: abrir ou fechar um detalhe não recria a tela, e a lista (mantida por ator) não é relida.
- A primeira página da fila reaproveita a resposta da validação do operador: nenhuma chamada repetida ao entrar.
- Amplo (conteúdo ≥ 960 dp e altura ≥ 480): lista de 360 dp e detalhe lado a lado, com rolagens independentes. Compacto: lista; com seleção, o detalhe ocupa a tela com `Voltar para fila`.
- Detalhe operacional com protocolo, situação, endereço copiado, `Morador #id` (sem nome nem telefone, que o operador não recebe da API), itens, histórico e `Ver histórico`.
- Ações por situação: em `SOLICITADA`, `Agendar` (principal), `Recusar` e `Cancelar`; em `AGENDADA`, `Cancelar` (as ações de execução chegam na 07). No compacto, as secundárias ficam em `Mais ações`.
- Formulário de data e hora no fuso de negócio (seletor de data a partir de hoje, seletor de hora, recusa local de horário passado, inexistente ou ambíguo; o servidor decide). Motivo obrigatório para recusa e cancelamento.
- Após a ação, o detalhe mostra o novo estado e a fila é relida; o item pode sair do filtro atual enquanto o detalhe continua aberto.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 86 testes aprovados (9 novos): ações por situação, agendamento com relógio fixo (hoje 09:00 recusado; 20/11 09:00 → `2026-11-20T12:00:00.000Z` com a versão lida), recusa com motivo obrigatório, 404 de outro ponto, `Mais ações` no compacto, fila lado a lado a 1366 px e detalhe em tela cheia a 360 px |
| Navegador a 1366 × 768 com API real | Operador 1 (Cooperativa Verde Vida): fila `Solicitada` com 1 pedido sem nova chamada; detalhe; `Agendar` para 16/11/2026 09:00 → `POST /api/solicitacoes/3/agendamento` 200, situação `Agendada`, fila relida com 0 pendentes e detalhe preservado |

## Etapa 07 · execução e reagendamento

Feito:

- `AGENDADA`: `Iniciar recolhimento` (principal, com confirmação que mostra protocolo e data agendada; o servidor aceita início antes do horário), `Reagendar` (nova data e motivo, recusando localmente o mesmo instante já agendado) e `Cancelar`.
- `EM_RECOLHIMENTO`: `Concluir recolhimento` (principal) e `Registrar falha` (motivo, informações da tentativa e nova data, que pode coincidir com a anterior).
- Conclusão (RN02): uma quantidade efetiva explícita por item gravado, com a estimada e a unidade copiada ao lado; não negativa, pelo menos uma positiva, `UN` inteira; zero é enviado como `0`. Data e responsável vêm do servidor.
- Todas as ações usam a versão lida; conflito relê o detalhe sem repetir a ação.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 92 testes aprovados (6 novos e 1 caso atualizado): ações de `AGENDADA` e `EM_RECOLHIMENTO`, início só com a versão, conclusão (vazia, todas zero, fração em peças, JSON com `11.125` e `0`), reagendamento com mesmo instante recusado e JSON correto, falha com motivo, tentativa e data, `409 CONFLITO_VERSAO` relendo o estado sem repetir |
| Navegador a 1366 × 768 com API real | Operador 1, solicitação 3: `Iniciar recolhimento` → `Em recolhimento`; `Concluir` com Papel 12 e Plástico 0 → `Concluída`; histórico com criação, agendamento (data marcada 16/11/2026 09:00), início e conclusão pelo Operador #1 |

## Etapa 08 · resumo e consulta administrativa

Feito:

- `/administracao/resumo` (RF15) com período na URL (`de`, `ate`), `Alterar período` e `Limpar período`. Bloco `Solicitações criadas no período` (pela data de criação) com as seis situações, barra discreta e valor em texto; cada linha abre `/administracao/solicitacoes` com a mesma situação e o mesmo período. Bloco `Recolhido em atendimentos concluídos` (pela data de conclusão) com material, quantidade e unidade por linha. Nada é somado no cliente. Lado a lado a partir de 744 dp (e escala de texto até 130%), empilhados abaixo disso. A validação do administrador é reaproveitada como primeira leitura. Período invertido mostra o erro do servidor em `ate`.
- `/administracao/solicitacoes` com situação, período de criação, ponto (seletor administrativo pesquisável e paginado, com inativos identificados) e paginação. `/administracao/solicitacoes/:id` com o detalhe base, sem comandos de situação, e `Abrir cadastro` do morador e dos responsáveis do histórico (as telas de cadastro chegam na 10).

Defeito corrigido nesta etapa: o roteador reconstrói as rotas quando a sessão notifica (por exemplo, ao trocar o endereço selecionado). As páginas liam o `ViewModel` do widget a cada build e recebiam uma instância nova, ainda vazia. Agora cada página fixa o `ViewModel` na criação do `State` e o descarta no `dispose`. Há um teste de regressão.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 97 testes aprovados (5 novos): resumo a 1366 px com valores e unidades da API e navegação por situação, 360 px com zeros e lista vazia e período na consulta, erro de período em `ate`, reaproveitamento sem chamada, regressão da troca de endereço no início |
| Navegador a 1366 × 768 com API real | Administrador: resumo com 2, 1, 1, 5, 1, 2 e Papel 88 kg (76 do seed mais os 12 concluídos na etapa 07); `Concluída` → `/administracao/solicitacoes?situacao=CONCLUIDA` com 5 solicitações |

## Etapa 09 · materiais, pontos e vínculos

Feito:

- `/administracao/pontos`: a mesma pesquisa do catálogo com identidade administrativa, filtro de situação, selo `Inativo` e `Novo ponto`. A lista é relida ao voltar de um editor.
- `/administracao/pontos/novo` e `/:id`: nome, contato, horários, `Faz recolhimento domiciliar` (enviado sempre de forma explícita), situação e endereço (`EnderecoForm` embutido, erros `endereco.*` associados ao campo). Inativação com confirmação. Em `409` da RN05, mensagem do servidor, edição preservada e `Ver solicitações do ponto` (abre a consulta com `pontoId`).
- Materiais aceitos no ponto persistido: `Vincular material` (seletor paginado com inativos identificados e vinculados desabilitados), edição de observações e limite (`null` remove o limite; o contrato aceita fração inclusive em `UN`) e remoção com confirmação que identifica material e ponto; conflito de remoção mostra a mensagem e o atalho.
- `/administracao/materiais`: lista paginada com filtro de situação. `/novo` e `/:id`: nome, descrição, unidade, orientação e situação, com confirmação de inativação e mensagem do servidor quando a troca de unidade é bloqueada (ADR 0013), preservando os dados.

Defeito corrigido: os erros do servidor eram exibidos com `forceErrorText`, que faz `Form.validate()` falhar enquanto o erro estiver presente; o usuário não conseguia reenviar depois de corrigir. Agora o erro vai para `InputDecoration.errorText`.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 101 testes aprovados (4 novos): novo ponto com booleano explícito, endereço embutido, erro `endereco.cep` e reenvio; inativação com confirmação e RN05; vínculo com limite nulo e remoção em conflito; troca de unidade bloqueada com JSON completo |
| Navegador a 1366 × 768 com API real | Administrador: editar Cooperativa Verde Vida para Inativo → confirmação → `409` (2 solicitações abertas), mensagem e `Ver solicitações do ponto`; ponto continua ativo no servidor |

## Etapa 10 · moradores e operadores

Feito:

- `/administracao/operadores` com filtro de ponto preservado na URL (`pontoId`), nome do ponto em cada cartão e paginação; `/novo` e `/:id` com nome, e-mail, ponto (seletor administrativo, aceita ponto inativo como a API) e situação. Inativação ou troca de ponto pede confirmação explicando o efeito na fila. Erros do servidor (e-mail duplicado `409`, ponto ausente `404`, validações `400`) aparecem no formulário.
- `/administracao/moradores` paginada com nome, e-mail, telefone e situação, mais `Cadastrar morador` (reaproveita `/cadastro`). `/:id` edita o `MoradorForm` com situação (inativação confirmada) e gerencia os endereços com o mesmo componente da conta do morador. `/:id/enderecos` abre a mesma página posicionada nos endereços.
- O detalhe administrativo da solicitação já abre esses cadastros a partir do morador e dos responsáveis do histórico.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `flutter analyze` | Nenhum problema |
| `flutter test` | 105 testes aprovados (4 novos): lista de operadores filtrada por ponto, troca de ponto com confirmação e `409` de e-mail, novo operador sem ponto bloqueado, morador administrativo com inativação confirmada e endereços |
| Navegador a 360 × 740 com API real | Administrador: `/administracao/operadores` com 3 operadores, nome do ponto, gaveta e `scrollWidth` 360 (sem rolagem horizontal) |

## Etapa 11 · qualidade e entrega Web

Feito:

- Integração HTTP com a API real (`test_integracao/`, 10 testes) usando os repositórios do app pelo proxy: RF06, isolamento, RN01 sem gravação parcial, RN02, data passada, operador de outro ponto, administrador sem transições, ciclo completo RF08–RF14 com conflito de versão e histórico em ordem, RN05, RF15 e `403` de perfil inválido.
- Acessibilidade automatizada em sete rotas principais a 360 e 1366 px (alvo, rótulo e contraste), texto a 200% no catálogo e navegação por teclado até a busca e os filtros.
- Marca `primeira-tela-util` (Web) e `tool/medir_desempenho.py` (Chrome/Edge headless via CDP, perfis de 10 Mbps e de 4 Mbps com CPU 4×, 15 repetições frias e quentes). Todas as metas iniciais atendidas; detalhes em `ROTEIRO_DEMONSTRACAO.md`.
- `intl` removido (não era usado); `web` acrescentado para a marca de desempenho; `test` acrescentado para a integração.
- Documentação: `ROTEIRO_DEMONSTRACAO.md` (preparação, IDs do seed, percurso e evidências), `MELHORIAS_FUTURAS.md` (limitações da API e pendências) e READMEs atualizados.

Defeitos corrigidos nesta etapa, encontrados pelos testes de acessibilidade:

- Linhas de `SelectableText` expunham alvos de toque menores que 48 dp. O texto de detalhe passou a ser selecionável por uma `SelectionArea` na página.
- Destinos do trilho estendido tinham 44 dp de altura. A navegação lateral persistente (a partir de 1000 dp) passou a usar uma gaveta fixa de 232 dp com destinos de 56 dp, rótulos com reticências e troca para gaveta modal com texto acima de 150%.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `dart format --set-exit-if-changed`, `flutter analyze` | Sem alterações; nenhum problema |
| `flutter test` | 121 testes aprovados |
| `dart test test_integracao` | 10 testes aprovados contra a API real |
| `flutter build web --release --no-web-resources-cdn` | Concluído em cerca de 30 s; `main.dart.js` com 3,97 MB sem compressão |
| Desempenho (Chrome 157 headless, 15 repetições) | p75 frio 3,38 s (computador) e 8,42 s (entrada); quente 0,58 s e 1,75 s; 3,38 MiB frios; 2 chamadas de API |
| Backend | `git diff main..HEAD -- backend/` vazio: nenhum arquivo do backend alterado |

## Etapa 13 · refinamento visual e experiência

Pedido do estudante em 14/11/2026: elevar a qualidade visual sem reconstruir o frontend e sem alterar o backend.

Feito:

- **Página inicial pública (`/`)**, que deixa de abrir direto nos pontos: apresentação ("Reciclar ficou mais simples.") com ações reais (pontos, materiais, demonstração quando habilitada, cadastro), Como funciona em quatro passos, Materiais recicláveis com os dados da API (papel, plástico, vidro e metal primeiro), Recolhimento domiciliar com ilustração vetorial original (`CustomPainter`) e a jornada do morador, Encontre um ponto (busca e atalhos que abrem o catálogo real), encerramento e rodapé. As seções surgem ao rolar (`Revelar`), a ilustração e o símbolo têm entrada única, e tudo respeita o movimento reduzido.
- **Logomarca**: símbolo universal da reciclagem com três setas que dobram nos cantos (lado de baixo mais escuro e recorte de sobreposição), em azul, vermelho e verde, com a dobra amarela. Conferida em 24, 32, 48 e 256 px e usada no cabeçalho, na página inicial e no rodapé; tocar nela leva a `/`.
- **Cartões padronizados**: `CartaoInterativo` (sombra suave, hover com elevação, pressionamento, transição de 220 ms) com altura fixa por grade multiplicada pela escala de texto. `CartaoPonto`, `CartaoMaterial` e `CartaoSolicitacao` limitam linhas (detalhes completos no detalhe), reservam a mesma área de ícone e alinham as ações embaixo.
- **Tema**: cartões com elevação suave e transição `FadeForwards` entre páginas.
- **Navegação**: a área pública usa links no cabeçalho, como um site (menu no compacto); faixa da demonstração com **Trocar perfil** e **Sair da demonstração** (menu no celular), que chamam `Sessao.sair()` (limpa caches e respostas restritas) e voltam a `/`. A conta do morador tem as mesmas ações.
- **Fila do operador**: no amplo, o detalhe ocupa um painel elevado e largo, com troca suave entre solicitações; o cabeçalho do detalhe destaca a situação em cor; cartões com faixa lateral da situação. No celular, o detalhe continua como página própria com `Voltar para fila`.
- **Filtros e diálogos**: as folhas presas na parte de baixo foram trocadas por diálogos centralizados de largura controlada (escolha de material, de ponto, de endereço de referência) e, para formulários longos no celular, tela própria (filtros de pontos). O conteúdo rola e acompanha o teclado virtual.
- **Administração**: moradores com o total da API (`totalElementos`) em destaque, cartões resumidos (nome, número e situação; e-mail e telefone só no cadastro) e **Abrir morador pelo número** (`GET /moradores/{id}`); operadores com filtro de ponto em chip (seletor pesquisável pelo nome do ponto), total e **Abrir operador pelo número**. As telas avisam que a API não tem pesquisa por nome e que a identidade é simulada.
- **Demonstração por teclado**: quando o perfil vem na URL, o foco vai ao identificador (ou ao botão, no administrador); basta digitar e pressionar Enter.

Defeito corrigido: o atraso da revelação usava `Future.delayed` e continuava pendente depois que o widget saía da árvore; agora é um `Timer` cancelado no `dispose`.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `dart format --set-exit-if-changed`, `flutter analyze` | Sem alterações; nenhum problema |
| `flutter test` | 130 testes aprovados (9 novos: página inicial, rotas dos botões, modo público, sair e trocar perfil, menu da faixa no celular, moradores com total, privacidade e abertura pelo número; `/` nas diretrizes de acessibilidade) |
| `dart test test_integracao` | 10 testes aprovados contra a API real |
| `flutter build web --release --no-web-resources-cdn` | Concluído |
| `tool/capturar_telas.py` (Chrome 157 headless) | 6 telas (início, pontos, materiais, início do morador, fila do operador, moradores) em 360, 390, 768 e 1366 px; `scrollWidth` igual à largura em todas as 24 capturas |
| Inspeção visual das capturas | Logomarca, página inicial, cartões de mesma altura, fila com painel principal, faixa com sair e trocar perfil, administração com total e aviso |

## Etapa 14 · experiência pública

Pedido do estudante em 14/11/2026: mudar a composição e a experiência visual da página
inicial, sem investir agora nos elementos gráficos definitivos.

Feito:

- **A · Composição.** Página inicial redesenhada: hero assimétrico (55/45 no desktop) com
  título em três linhas, uma ação principal (`BotaoDestaque`) e links discretos, e composição
  visual em camadas (placa verde girada, espaço da imagem e etiquetas sobrepostas); percurso de
  "Como funciona" em linha com etapas alternadas acima e abaixo (vertical conectado no celular e
  no tablet); materiais em composição assimétrica com o primeiro tradicional em destaque e blocos
  coloridos, os demais em "Também aceitos"; recolhimento em fundo verde com camada de
  profundidade; busca central em pílula com `Filtros` e `Buscar`; encerramento tipográfico em
  verde profundo; rodapé discreto.
- **Assets provisórios.** `EspacoImagem` e `AssetsVisuais` reservam proporções para a imagem
  principal, o recolhimento, as etapas e a logomarca (guia em `ASSETS_VISUAIS.md`).
- **B · Movimento e interação.** `EntradaCoordenada` (um controlador, intervalos escalonados) no
  hero; `Revelar` com efeitos variados (subir, da esquerda, da direita, escala) e semântica
  sempre presente; `BotaoDestaque` com hover (escurece, sobe 2 dp, sombra maior, seta avança),
  compressão ao pressionar e anel de foco; `LinkDiscreto` com sublinhado que cresce; blocos de
  material com elevação, borda na cor e seta animada.
- **Transições por função.** Investigado: o tema define `FadeForwards`, mas o go_router usa a
  página padrão de cada rota e o efeito acompanha a página empurrada, inclusive em `context.go`.
  Para controlar, as rotas públicas passaram a `CustomTransitionPage`: destinos (`/`, `/pontos`,
  `/materiais`, `/cadastro`, `/demonstracao`) com fade de 260 ms; detalhes (`/pontos/:id`,
  `/materiais/:id`) com deslizamento de 32 dp e fade de 320 ms, voltando pelo mesmo caminho.
  Diálogos entram com fade e escala de 0,96 a 1 em 240 ms. Tudo respeita
  `MediaQuery.disableAnimationsOf`. As áreas internas mantêm a transição do tema.
- **Filtros a partir da página inicial.** `Filtros` leva a `/pontos?filtros=1&nome=…`; o catálogo
  abre o painel de filtros com o nome preenchido e retira o parâmetro da URL.
- **C · Acabamento.** Tipografia de títulos com tracking negativo, sobretítulos em caixa alta,
  margens por formato (20, 40 e 56 dp), espaço negativo amplo e recorte arredondado nos blocos
  (a forma decorativa vazava pelos cantos, corrigido após a primeira captura).

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `dart format --set-exit-if-changed`, `flutter analyze` | Sem alterações; nenhum problema |
| `flutter test` | 131 testes aprovados (novos: seções da página inicial, rotas da ação principal, atalho de filtros) |
| `dart test test_integracao` | 10 testes aprovados contra a API real |
| `flutter build web --release --no-web-resources-cdn` | Concluído |
| Capturas (`tool/capturar_pagina_inteira.py`) | Página inteira e primeira dobra em 390, 768 e 1366 px, antes e depois; hover; filtros; detalhe. Em `docs/capturas/etapa-14/` com avaliação crítica |

Ajustes feitos durante a verificação: textos das etapas limitados a 2 e 3 linhas (a altura fixa
estourava com quebras maiores); conteúdo ainda não revelado mantido na árvore de acessibilidade.

## Etapa 15 · interações e navegação

Pedido do estudante em 14/11/2026: levar o acabamento interativo aprovado na página inicial ao
resto do sistema, trocar o fade entre páginas por um deslizamento direcional e rolar a página
inicial até as próprias seções, sem refazer a composição aprovada.

Feito:

- **Transição direcional entre destinos.** `MemoriaDestinos` (`lib/app/navegacao.dart`) guarda o
  último destino principal e compara a sua posição com a do novo, na ordem do menu da área:
  público `Início → Pontos → Materiais`, morador `Início → Pontos → Solicitações → Conta` e as seis
  entradas do administrador. Destino à direita entra pela direita; à esquerda, pela esquerda
  (Início → Materiais entra direto pela direita). A nova página desliza 30% da largura (no máximo
  360 dp) sobre a anterior em 340 ms, com fundo opaco e sombra que aparece no meio do movimento; a
  anterior recua até 96 dp, encolhe 2% e perde 40% de opacidade. Troca de perfil, redirecionamento,
  cadastro, demonstração e acesso restrito usam fade neutro com escala mínima. O cabeçalho e a
  navegação lateral ficam parados, porque só as páginas do `ShellRoute` animam.
- **Por que funciona com `context.go`.** Investigado no `DefaultTransitionDelegate` do Flutter: ao
  trocar `[A]` por `[B]`, `B` é empurrada com animação e `A` é concluída sem animação, mas continua
  na árvore até `B` terminar; por isso a anterior aparece por baixo e recebe a animação
  secundária. Um teste confirma que a página inicial ainda existe a 136 ms e sai ao fim.
- **O que não anima.** O go_router dá a mesma chave de página a `/pontos` e `/pontos?nome=…`;
  filtros, paginação e parâmetros da mesma tela atualizam a página sem nova transição. A direção
  é calculada uma vez por chave, então reconstruções (por exemplo, da sessão) não a alteram.
  Detalhes e formulários (`/pontos/:id`, `/materiais/:id`, endereços, solicitação, editores do
  administrador) mantêm o deslizamento curto de entrada e o retorno pelo mesmo caminho; a página
  de baixo não se desloca. A fila do operador continua sem transição entre lista e detalhe, que
  são a mesma página.
- **Rolagem na página inicial.** "Conhecer os materiais" rola até a seção de materiais (560 ms,
  `easeInOutCubic`, título 40 px abaixo do topo da área rolável, que já começa abaixo do
  cabeçalho) e põe o foco do teclado numa âncora da seção: o próximo Tab vai a "Ver todos os
  materiais", que continua abrindo `/materiais`. "Experimentar a demonstração" rola até uma faixa
  nova no fim do encerramento, que explica a demonstração em duas linhas e oferece "Escolher um
  perfil" (abre `/demonstracao`, e recebe o foco ao chegar). Com movimento reduzido, a rolagem é
  imediata. O botão "Demonstração" do cabeçalho e o link do rodapé continuam diretos, então a
  rolagem não acrescenta uma etapa obrigatória para quem já sabe o que quer.
- **Botões.** Tema em pílula para todos os botões. Ações cheias com sombra por estado (2 em
  repouso, 6 no hover, 1 ao pressionar) e `Elevavel` (sobe 2 dp no hover, encolhe 3% ao pressionar,
  ignora botões desabilitados) nas 34 ações principais; setas com `IconeDirecional` avançam no
  hover. Secundários (contorno) sem sombra: borda primária de 1,5 dp e fundo verde suave no hover.
  Destrutivos com `estiloDestrutivo`: vermelho, hover e pressionado mais escuros e sombra
  avermelhada. Foco de teclado em azul, como antes.
- **Atalhos circulares de materiais.** Continuam circulares e com as cores do design system.
  Repouso com sombra suave sobre um anel branco; hover sobe 3 dp, borda de 2 dp na cor do material,
  sombra maior e ícone 6% maior; pressionar encolhe 7%; seleção com anel de 3 dp, selo de
  confirmação e rótulo em negrito. Valem no catálogo, na página inicial e no início do morador.
- **Cartões.** `CartaoInterativo` ganha compressão de 1,5% ao pressionar (o retorno ao toque no
  celular). As listas clicáveis que ainda eram `Card` + `ListTile` (operadores, solicitações
  recentes do morador, "Meus endereços") passam a usá-lo. Superfícies não clicáveis não mudaram.
- **Entradas internas.** `Aparecer`: fade com 12 dp de subida, uma vez por posição na árvore.
  `PaginaRolavel` escalona os filhos (45 ms entre eles); os resultados de `ConteudoCarga` e os
  avisos de confirmação e erro entram ao surgir. Novas respostas da API, campos alterados e
  reconstruções não reiniciam a animação, e nada bloqueia o toque durante a entrada.

Verificações executadas:

| Verificação | Resultado |
|---|---|
| `dart format --set-exit-if-changed`, `flutter analyze` | Sem alterações; nenhum problema |
| `flutter test` | 147 testes aprovados (novos: direção entre destinos público e do morador, página anterior por baixo, filtro sem transição, detalhe de ida e volta, troca de perfil neutra, movimento reduzido, rolagem até materiais e demonstração, elevação de botões, destrutivo, atalhos circulares, entrada única) |
| `dart test test_integracao` | 10 testes aprovados contra a API real |
| `flutter build web --release --no-web-resources-cdn` (público e `APP_MODE=demo`) | Concluídos |
| Gravações no Chrome headless (`tool/gravar_transicoes.py`) | Quadros intermediários de todas as transições pedidas a 390, 768 e 1366 px, rolagens, hover, foco de teclado e movimento reduzido, em `docs/capturas/etapa-15/` |

Achado durante a validação: o Chrome headless informa `prefers-reduced-motion: reduce`, e o app
trocava de página sem animação (o comportamento correto para essa preferência). A ferramenta de
gravação passou a declarar a preferência explicitamente; com `--reduzido`, a gravação confirma a
troca imediata.

## Pendências e problemas conhecidos

- Assets definitivos (logomarca, imagem principal, recolhimento e etapas) a cargo do estudante.
- Selo de modalidade truncado nos cartões de ponto e foco do atalho Papel parecido com o hover:
  registrados em `backend/docs/melhorias-pendentes.md` (seção Frontend).
- A entrada coordenada do herói roda de novo a cada visita à página inicial, junto do
  deslizamento de chegada. Foi mantida porque a composição está aprovada; fica para avaliação.

- Não testados: Firefox, Safari, celular físico, acesso pela rede local, leitores de tela, fluidez de rolagem e memória. A demonstração pelo celular exige `CADDY_BIND` e liberação da porta no firewall, que não foram alterados.
- Persistência após reinício da API não foi exercitada pelo frontend (o backend tem evidência própria).
- A etapa 12 (alvos nativos) não foi iniciada: Android SDK e workload C++ do Visual Studio não estão instalados e não foram instalados, conforme a orientação.
- Limitações da API e evoluções sugeridas em `MELHORIAS_FUTURAS.md`.
- O banco `coleta_frontend_demo` recebeu as solicitações criadas nos testes de navegador e de integração; o banco de desenvolvimento não foi usado.
