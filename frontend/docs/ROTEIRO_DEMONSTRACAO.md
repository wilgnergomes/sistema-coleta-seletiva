# Roteiro de demonstração e evidências

Roteiro da entrega Web, com os dados fictícios do perfil `demo` e os resultados
medidos na etapa 11. O planejamento está em [PLANO_MESTRE_FRONTEND.md](PLANO_MESTRE_FRONTEND.md).

## Preparação

1. Banco fictício dedicado (uma vez), no container do projeto:

   ```powershell
   docker exec coleta-seletiva-db sh -c 'createdb -U "$POSTGRES_USER" coleta_frontend_demo'
   ```

2. Backend em `backend/`, só no loopback, com o seed `demo`:

   ```powershell
   $env:DB_NAME = 'coleta_frontend_demo'
   $env:SERVER_ADDRESS = '127.0.0.1'
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
   ```

3. Build e proxy em `frontend/` (PowerShell):

   ```powershell
   flutter build web --release --no-web-resources-cdn --dart-define=APP_MODE=demo --dart-define=API_BASE_URL=/api
   caddy run --config Caddyfile
   ```

4. Abrir <http://127.0.0.1:8000>. Para o celular na mesma rede controlada, defina
   `$env:CADDY_BIND` com o IP do PC antes do passo 3 e libere a porta 8000 no
   firewall dessa rede (não foi feito nem testado nesta implementação).

Os identificadores variam entre bancos. No banco desta implementação, o seed criou:

| Ator | Identificador | Observação |
|---|---|---|
| Maria Silva (morador) | 3 | Dois endereços em Lagarto |
| João Pereira (morador) | 4 | Para isolamento entre moradores |
| Beatriz Rocha (morador) | 1 | Inativo: a demonstração recusa a seleção |
| Carla Nunes, Paulo Santos (operadores) | 1, 2 | Cooperativa Verde Vida |
| Rita Alves (operador) | 3 | Ecoponto Centro, sem recolhimento domiciliar |
| Administrador | — | Não exige identificador |

Em outro banco, confira os IDs em `/administracao/moradores` e `/administracao/operadores`
ou no Swagger.

## Percurso sugerido

| Passo | Onde | O que mostrar | RF, RN e RNF |
|---|---|---|---|
| 1 | `/pontos` (visitante) | Barra de pesquisa, atalho Papel, filtros de município e bairro, total, paginação e detalhe com horários, contato e materiais | RF03–RF06, RNF02 |
| 2 | `/cadastro` | Cadastro de morador; e-mail repetido recusado com o rascunho preservado | RF01, RNF03 |
| 3 | `Demonstração` → Morador 3 | Faixa de simulação, início com endereço selecionado, `Pontos em Lagarto`, pontos usados recentemente e solicitações recentes | RNF06, RF02, RF06, RF09 |
| 4 | Detalhe da Cooperativa Verde Vida → `Solicitar recolhimento` | Materiais aceitos com unidade e limite, quantidade `3,5`, protocolo e histórico após o envio | RF08, RN01, RN02 |
| 5 | Detalhe da solicitação | `Corrigir solicitação` (escolha explícita do endereço) e `Cancelar solicitação` com motivo | RF09, RF12, RN04, RN06 |
| 6 | `Trocar` → Operador 1 | Fila `Solicitada`, detalhe ao lado (1366 px) ou em tela cheia (360 px), `Agendar` com data futura | RF07, RF09, RF10 |
| 7 | Mesmo detalhe | `Iniciar recolhimento`, `Registrar falha`, `Reagendar`, `Concluir recolhimento` com efetivas (uma pode ser 0) | RF13, RF14, RN02, RN03 |
| 8 | `Trocar` → Administrador | Resumo com as seis situações e as quantidades por unidade; período; contagem → consulta filtrada | RF15, RF09 |
| 9 | `Pontos` → Cooperativa Verde Vida | Tentar inativar: `409` da RN05 com `Ver solicitações do ponto` | RF04, RN05 |
| 10 | `Materiais`, `Operadores`, `Moradores` | Edição com confirmação, vínculos com limite, troca de ponto do operador, endereços do morador | RF03, RF05, RF07, RF01–RF02 |

## Evidências executadas

### Testes automatizados

| Conjunto | Comando (em `frontend/`) | Resultado |
|---|---|---|
| Formatação | `dart format --output=none --set-exit-if-changed .` | Sem alterações |
| Análise estática | `flutter analyze` | Nenhum problema |
| Unitários e de widget | `flutter test` | 121 testes aprovados |
| Integração HTTP com a API real (pelo proxy) | `dart test test_integracao` | 10 testes aprovados |

Os testes de widget usam o repositório real sobre um `MockClient` e conferem o JSON
enviado, os cabeçalhos de identidade e as respostas de erro. Os de acessibilidade
aplicam `androidTapTargetGuideline`, `labeledTapTargetGuideline` e
`textContrastGuideline` em sete rotas principais, a 360 e a 1366 px, além de texto a
200% e navegação por teclado no catálogo.

Os testes de integração exercitam, contra o Spring e o banco fictício: filtro RF06 por
município e material, isolamento entre moradores, recusa de material incompatível sem
gravação parcial (RN01), peça fracionária (RN02), agendamento no passado, operador de
outro ponto (`404`), administrador sem transições (`403`), o ciclo completo com
correção, conflito de versão (`CONFLITO_VERSAO`), falha, conclusão e histórico em ordem
(RF08–RF14, RN03, RN06), RN05 ao inativar ponto com abertas, RF15 e `403` de perfil
inválido. Criam solicitações novas e cancelam as que ficam abertas.

### Navegador

| Verificação | Ambiente | Resultado |
|---|---|---|
| Catálogo, filtros, detalhe | Painel do app (Chromium), 360 × 740 e 1366 × 768 | Funcionando; `scrollWidth = clientWidth` nas duas larguras |
| Morador: guard, demonstração, início, conta, nova solicitação, cancelamento | Idem, API real | `201`, protocolo, histórico, motivo obrigatório |
| Operador: fila com reaproveitamento da validação, agendamento, início, conclusão | Idem, API real | Sem chamada repetida ao entrar; histórico com responsável |
| Administrador: resumo, consulta por situação, RN05, operadores a 360 px | Idem, API real | Valores iguais ao JSON; `409` com atalho |
| Primeira tela útil, dois perfis | Chrome 157 e Edge 157 headless via CDP (`tool/medir_desempenho.py`) | Tabela abaixo |

Não foram testados: Firefox, Safari, celular físico, rede local com outro aparelho,
leitores de tela (NVDA, TalkBack, VoiceOver) e alvos nativos.

### Desempenho

Build release padrão (JavaScript, CanvasKit local com `--no-web-resources-cdn`), Caddy com
zstd, Windows 11, Chrome 157 headless. A marca `primeira-tela-util` é registrada depois do
quadro que apresenta a primeira lista do catálogo. Quinze repetições por cenário. Cache frio
com cache e armazenamento da origem limpos; cache quente com os recursos do app disponíveis.
O backend estava iniciado e no mesmo PC.

| Perfil | Cenário | p50 | p75 | p95 | Meta (p75) |
|---|---|---:|---:|---:|---:|
| Computador (10 Mbps, RTT 50 ms) | Frio | 3,35 s | 3,38 s | 3,45 s | 5 s |
| Computador | Quente | 0,56 s | 0,58 s | 0,68 s | 2 s |
| Entrada (4 Mbps, RTT 150 ms, CPU 4×) | Frio | 8,40 s | 8,42 s | 8,43 s | 9 s |
| Entrada | Quente | 1,67 s | 1,75 s | 1,79 s | 4 s |

- Transferência fria até a primeira tela útil: 3.546.027 bytes (3,38 MiB), 15 requisições.
  Orçamento exploratório: 3,5 MiB. O motor CanvasKit (≈ 2,04 MiB comprimidos) domina.
- Chamadas de API na primeira tela: `GET /api/pontos-coleta?page=0&size=20` (principal) e
  `GET /api/materiais?page=0&size=100` (atalhos e filtros). Nenhuma busca por linha.
- A base de fusos (`main.dart.js_1.part.js`) só é baixada nas áreas com datas.
- O Edge 157 headless, com 3 repetições, teve valores equivalentes (p75 frio de 3,46 s e
  8,41 s).
- O perfil de entrada é uma simulação de laboratório no mesmo PC, não um aparelho real.
  Fluidez de rolagem e memória não foram medidas.

A margem do perfil de entrada frio é pequena (cerca de 0,6 s). Se o build crescer, a
primeira ação recomendada é comparar com `--wasm` e avaliar o carregamento adiado das
áreas restritas.
