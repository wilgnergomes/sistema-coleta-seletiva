# Frontend Web · Sistema de Coleta Seletiva

Aplicativo Flutter (Web responsiva) que consome a API REST do backend em
`../backend/`. O planejamento está em [`docs/`](docs/PLANO_MESTRE_FRONTEND.md) e o
andamento em [`docs/PROGRESSO_IMPLEMENTACAO.md`](docs/PROGRESSO_IMPLEMENTACAO.md).

## Requisitos

| Ferramenta | Versão usada | Observação |
|---|---|---|
| Flutter | 3.50.0 stable (Dart 3.14.0) | `flutter --version` |
| Caddy | 2.11.9 | Proxy de mesma origem para `/api` |
| Chrome ou Edge | atual | Alvo principal |
| Backend | `../backend/` | JDK 21 e PostgreSQL, conforme `backend/README.md` |

## Executar a demonstração (build release)

1. Suba o backend a partir de `backend/` (PowerShell), escutando só no loopback:

   ```powershell
   $env:SERVER_ADDRESS = '127.0.0.1'
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
   ```

   O perfil `demo` grava os dados fictícios do seed no banco configurado. Para não
   misturar com o banco de desenvolvimento, use um banco próprio definindo
   `$env:DB_NAME = 'coleta_frontend_demo'` antes do comando (crie-o uma vez com
   `createdb` no PostgreSQL).

2. Gere o build a partir de `frontend/` **no PowerShell ou no cmd**:

   ```powershell
   flutter build web --release --no-web-resources-cdn --dart-define=APP_MODE=demo --dart-define=API_BASE_URL=/api
   ```

   > No Git Bash, o MSYS converte `/api` em um caminho do Windows
   > (`C:/Program Files/Git/api`). Se precisar usar o Bash, prefixe o comando com
   > `MSYS_NO_PATHCONV=1`.

3. Sirva o build e o proxy a partir de `frontend/`:

   ```powershell
   caddy run --config Caddyfile
   ```

4. Abra <http://127.0.0.1:8000>.

Para a demonstração no celular pela rede controlada, defina `$env:CADDY_BIND` com
o IP do PC (por exemplo `192.168.1.50`) antes do passo 3, libere a porta 8000 no
firewall dessa rede e abra `http://192.168.1.50:8000` no celular. O Spring continua
acessível apenas pelo loopback.

## Desenvolvimento

```powershell
flutter run -d web-server --web-hostname=127.0.0.1 --web-port=8081 --dart-define=APP_MODE=demo --dart-define=API_BASE_URL=/api
caddy run --config Caddyfile.dev
```

Abra <http://127.0.0.1:8000>. O Caddy encaminha `/api/*` ao Spring e o restante ao
servidor do Flutter.

## Configuração por `--dart-define`

| Chave | Padrão | Uso |
|---|---|---|
| `APP_MODE` | `publico` | `demo` habilita a identidade simulada (ADR 0019). `publico` mostra só a área pública. |
| `API_BASE_URL` | `/api` | Relativa à origem da página na Web, ou absoluta para alvos nativos. |
| `BUSINESS_TIME_ZONE` | `America/Sao_Paulo` | Fuso de negócio, o mesmo do backend (`coleta.fuso-negocio`). |

Os valores ficam visíveis no cliente compilado. Não passe credenciais por aqui.

## Verificações

```powershell
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
```

Integração HTTP com a API real, pelo proxy (backend com o perfil `demo` e Caddy no ar;
cria solicitações no banco fictício):

```powershell
dart test test_integracao
```

Medição da primeira tela útil com o Chrome headless, nos perfis do planejamento:

```powershell
python -m pip install websocket-client==1.8.0
python tool/medir_desempenho.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"
```

Roteiro, identificadores do seed e resultados: [`docs/ROTEIRO_DEMONSTRACAO.md`](docs/ROTEIRO_DEMONSTRACAO.md).
Limitações e evoluções: [`docs/MELHORIAS_FUTURAS.md`](docs/MELHORIAS_FUTURAS.md).

## Áreas e rotas

| Perfil | Rotas principais |
|---|---|
| Público | `/pontos`, `/pontos/:id`, `/materiais`, `/materiais/:id`, `/cadastro`, `/demonstracao` |
| Morador | `/morador/inicio`, `/morador/solicitacoes` (`/nova`, `/:id`, `/:id/corrigir`), `/morador/conta`, `/morador/conta/enderecos` |
| Operador | `/operador/fila`, `/operador/fila/:id` |
| Administrador | `/administracao/resumo`, `/administracao/solicitacoes`, `/administracao/pontos`, `/administracao/materiais`, `/administracao/operadores`, `/administracao/moradores` |

A identidade é simulada por cabeçalhos (ADR 0019), fica só em memória e é pedida de novo
ao recarregar a página numa rota restrita.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `lib/app/` | Configuração, composição, rotas, sessão e tema |
| `lib/core/` | Cliente HTTP, páginas, falhas, JSON, datas e quantidades |
| `lib/ui/compartilhado/` | Componentes reutilizáveis |
| `lib/features/` | Funcionalidades, com `ui/` (Views e ViewModels) e `data/` (repositórios e modelos) |
| `test/` | Testes unitários e de widgets |
| `test_integracao/` | Integração HTTP com a API real |
| `tool/` | Script de medição de desempenho |
| `docs/` | Planejamento, progresso e evidências |
