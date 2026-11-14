# Capturas da etapa 14 · experiência pública

Capturas reais do build release (Chrome 157 headless, `tool/capturar_pagina_inteira.py`),
com a API Spring e o banco de demonstração. "Antes" é a branch `frontend/13-refinamento-visual`
(`8850e0d`), gerada numa worktree temporária; "depois" é `frontend/14-experiencia-publica`.

## Página inicial inteira

| Largura | Antes | Depois |
|---|---|---|
| 390 px | [antes-390-inteira.png](antes-390-inteira.png) | [depois-390-inteira.png](depois-390-inteira.png) |
| 768 px | [antes-768-inteira.png](antes-768-inteira.png) | [depois-768-inteira.png](depois-768-inteira.png) |
| 1366 px | [antes-1366-inteira.png](antes-1366-inteira.png) | [depois-1366-inteira.png](depois-1366-inteira.png) |

Primeira dobra: `antes-*-dobra.png` e `depois-*-dobra.png`.

## Estados de interação

| Estado | Arquivo |
|---|---|
| Hover do botão principal (1366 px): fundo mais escuro, elevação de 2 dp, sombra maior e seta avançando | [depois-1366-hover.png](depois-1366-hover.png) |
| Filtros abertos ao chegar da página inicial (`/pontos?filtros=1&nome=Verde`), com o nome já preenchido | [filtros-1366-dobra.png](filtros-1366-dobra.png), [filtros-390-dobra.png](filtros-390-dobra.png) |
| Detalhe do ponto (destino da transição lista → detalhe) | [detalhe-1366-dobra.png](detalhe-1366-dobra.png), [detalhe-390-dobra.png](detalhe-390-dobra.png) |

As transições em si (fade de 260 ms entre destinos; deslizamento de 32 dp com fade de 320 ms
para detalhes, e o caminho inverso ao voltar) não aparecem numa imagem estática. Estão em
`lib/ui/compartilhado/movimento.dart`.

## Avaliação crítica

- **Seções diferentes entre si:** sim. Hero assimétrico com camadas, percurso em linha com
  etapas alternadas acima e abaixo (vertical no celular), blocos de materiais com o Papel em
  destaque, recolhimento em fundo verde com camada de profundidade, busca centralizada em
  pílula e encerramento tipográfico em verde profundo.
- **Deixou de parecer uma sequência de cartões:** em grande parte. Os materiais ainda usam
  blocos, mas com cor própria, tamanhos diferentes e forma decorativa, sem a grade uniforme
  anterior.
- **Hierarquia:** título de 68 px no desktop (44 px no celular), uma ação principal evidente e
  links secundários discretos.
- **Hero equilibrado:** sim no desktop (55/45). O conteúdo visual é provisório: o símbolo atual
  sobre um degradê, até a imagem definitiva.
- **Movimento:** entrada coordenada no hero (selo, título, texto, ação, links e visual, com
  atrasos de cerca de 130 ms) e entradas variadas por seção (subir, da esquerda, da direita,
  escala). É perceptível sem atrapalhar a leitura, e desligado com movimento reduzido.
- **Celular com composição própria:** sim. Texto e ação primeiro, visual depois, percurso
  vertical conectado e blocos empilhados.

Pontos que ainda podem melhorar com os assets definitivos: o espaço do hero e o da seção de
recolhimento ficam mais expressivos com ilustrações reais; o bloco grande do Papel tem área
vazia no celular, que uma ilustração do material pode ocupar.
