# Evidências da etapa 15 · interações e navegação

Gravações reais do build release com `APP_MODE=demo` (Chrome 157 headless, screencast pelo CDP,
`tool/gravar_transicoes.py`), com a API Spring e o banco de demonstração. Cada passo tem uma
folha `*-quadros.png` com seis quadros entre o clique e o fim do movimento, marcados com o tempo
desde o clique. As transições principais também têm a gravação curta em `*.gif` (reduzida para
caber no repositório).

## Transições entre destinos

| Caminho | Largura | Esperado | Quadros |
|---|---|---|---|
| Início → Pontos | 1366 | entra pela direita | [folha](1366-inicio-pontos-quadros.png) · [gif](1366-inicio-pontos.gif) |
| Pontos → Materiais | 1366 | entra pela direita | [folha](1366-pontos-materiais-quadros.png) · [gif](1366-pontos-materiais.gif) |
| Materiais → Início | 1366 | entra pela esquerda | [folha](1366-materiais-inicio-quadros.png) · [gif](1366-materiais-inicio.gif) |
| Início → Pontos → Início | 1366 | direita, depois esquerda | [ida](1366-inicio-pontos-2-quadros.png) · [volta](1366-pontos-inicio-quadros.png) |
| Início → Materiais | 1366 | direita, sem passar por Pontos | [folha](1366-inicio-materiais-quadros.png) · [gif](1366-inicio-materiais.gif) |
| Início → Pontos → Materiais → Início | 768 | direita, direita, esquerda (pelo menu) | [1](768-inicio-pontos-quadros.png) · [2](768-pontos-materiais-quadros.png) · [3](768-materiais-inicio-quadros.png) |
| Início → Pontos → Materiais → Início | 390 | direita, direita, esquerda (pelo menu) | [1](390-inicio-pontos-quadros.png) · [2](390-pontos-materiais-quadros.png) · [gif](390-pontos-materiais.gif) · [3](390-materiais-inicio-quadros.png) |
| Morador: Início → Pontos → Solicitações → Conta → Início | 1366 | três vezes direita, depois esquerda | [1](1366-morador-inicio-pontos-quadros.png) · [2](1366-morador-pontos-solicitacoes-quadros.png) · [3](1366-morador-solicitacoes-conta-quadros.png) · [gif](1366-morador-solicitacoes-conta.gif) · [4](1366-morador-conta-inicio-quadros.png) |
| Morador pela barra inferior: Início → Solicitações → Pontos → Conta | 390 | direita, esquerda, direita | [1](390-morador-inicio-solicitacoes-quadros.png) · [gif](390-morador-inicio-solicitacoes.gif) · [2](390-morador-solicitacoes-pontos-quadros.png) · [3](390-morador-pontos-conta-quadros.png) |
| Detalhe do ponto: abrir e voltar | 1366 | entra pela direita e sai pelo mesmo lado; a lista não se move | [abrir](1366-detalhe-abrir-quadros.png) · [gif](1366-detalhe-abrir.gif) · [voltar](1366-detalhe-voltar-quadros.png) · [gif](1366-detalhe-voltar.gif) |
| Início → Pontos com movimento reduzido | 1366 | troca imediata, sem quadros intermediários | [folha](1366-reduzido-inicio-pontos-quadros.png) · [gif](1366-reduzido-inicio-pontos.gif) |

## Rolagem na página inicial

| Link | Largura | Quadros |
|---|---|---|
| Conhecer os materiais | 1366 | [folha](1366-rolagem-materiais-quadros.png) · [gif](1366-rolagem-materiais.gif) |
| Conhecer os materiais | 390 | [folha](390-rolagem-materiais-quadros.png) |
| Experimentar a demonstração | 1366 | [folha](1366-rolagem-demonstracao-quadros.png) |

## Hover e teclado

| Estado | Arquivo |
|---|---|
| Atalho Papel em hover: sobe, borda azul do material, sombra maior, rótulo mais escuro | [hover-1366-material-papel.png](hover-1366-material-papel.png) |
| Cartão de ponto em hover: sobe, borda verde e sombra maior | [hover-1366-cartao-ponto.png](hover-1366-cartao-ponto.png) |
| Botão "Demonstração" do cabeçalho em hover (pílula, mais escuro, sombra maior) | [hover-1366-botao-demonstracao.png](hover-1366-botao-demonstracao.png) |
| Foco de teclado no atalho Papel, depois de seis Tabs | [teclado-1366-material.png](teclado-1366-material.png) |

## Avaliação crítica

- **Direção:** correta em todos os caminhos gravados, nas três larguras e no morador. A nova
  página aparece sobre a anterior por volta de 140 ms, com a borda sombreada do lado de onde vem,
  e está no lugar perto de 300 ms. A anterior recua para o outro lado e perde destaque. É
  perceptível sem parecer carrossel, porque o deslocamento é parcial (30% da largura).
- **Estabilidade:** cabeçalho, faixa de simulação e navegação lateral não se movem em nenhuma
  gravação. No morador a 1366 px só a área de conteúdo desliza.
- **Detalhe:** entra com deslocamento curto e volta pelo mesmo lado, e a lista embaixo continua
  parada. É coerente com o destino, mas mais discreto, como convém a um passo dentro da mesma área.
- **Rolagem:** "Conhecer os materiais" passa pelas seções em cerca de 560 ms e termina com
  "Cada material tem o seu caminho." logo abaixo do cabeçalho; não há salto. "Experimentar a
  demonstração" termina com a frase de encerramento e a faixa da demonstração à vista.
- **Hover e seleção:** os atalhos continuam circulares e com as cores dos materiais; hover
  (borda fina na cor e elevação) e seleção (anel espesso e selo) são diferentes entre si.
- **Movimento reduzido:** a troca é imediata (o quadro de 133 ms já mostra a página nova).

Pontos de atenção encontrados nas gravações:

- Na chegada à página inicial, a entrada coordenada do herói roda junto com o deslizamento. O
  movimento fica um pouco mais longo nessa rota que nas demais. A composição aprovada foi mantida.
- A 390 px, o menu suspenso ainda está fechando quando a nova página começa a entrar (aparece
  nos primeiros quadros). Não atrapalha o uso.
- Selo de modalidade truncado nos cartões de ponto ("Recolhim…") e foco do Papel parecido com o
  hover: registrados em `backend/docs/melhorias-pendentes.md`.
- As gravações vêm do Chrome headless com renderização por software, a 40–80 quadros por
  segundo. A fluidez em hardware real, Firefox, Safari e celulares físicos não foi medida.
