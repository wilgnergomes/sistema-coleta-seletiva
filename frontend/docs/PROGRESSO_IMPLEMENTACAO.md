# Progresso da implementação do frontend

Registro de continuidade da implementação integral pela IA, autorizada pelo
estudante em 13/11/2026. Cada etapa nasce do último commit validado da etapa
anterior (branches cumulativas, `CLAUDE.md`).

## Estado atual

| Item | Valor |
|---|---|
| Branch atual | `frontend/02-design-system` |
| Base da etapa | `frontend/01-estrutura` em `d821e43` |
| Etapa em andamento | 02 · design system |
| Próxima ação | Abrir `frontend/03-catalogo-publico` a partir do último commit da 02 |

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
| 02 | `frontend/02-design-system` | Em andamento | — |

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

## Pendências e problemas conhecidos

- Medições formais de desempenho nos perfis definidos (etapa 11).
