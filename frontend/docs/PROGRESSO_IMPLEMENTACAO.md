# Progresso da implementação do frontend

Registro de continuidade da implementação integral pela IA, autorizada pelo
estudante em 13/11/2026. Cada etapa nasce do último commit validado da etapa
anterior (branches cumulativas, `CLAUDE.md`).

## Estado atual

| Item | Valor |
|---|---|
| Branch atual | `frontend/05-solicitacoes-morador` |
| Base da etapa | `frontend/04-morador-e-enderecos` em `742a9a9` |
| Etapa em andamento | 05 · solicitações do morador |
| Próxima ação | Abrir `frontend/06-fila-e-agendamento` a partir do último commit da 05 |

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
| 05 | `frontend/05-solicitacoes-morador` | Em andamento | — |

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

## Pendências e problemas conhecidos

- Medições formais de desempenho nos perfis definidos (etapa 11).
