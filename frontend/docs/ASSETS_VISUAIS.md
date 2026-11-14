# Assets visuais definitivos

A logomarca e as ilustrações atuais são **provisórias**. A interface já tem espaços com
proporção definida para receber os assets escolhidos pelo estudante, sem mudar as telas.

| Asset | Onde aparece | Proporção e tamanho sugeridos | Campo em `AssetsVisuais` |
|---|---|---|---|
| Imagem principal | Hero da página inicial | 4:3, mínimo 1200 × 900 | `heroi` |
| Recolhimento domiciliar | Seção "O ponto vai até a sua casa." | 5:4, mínimo 1000 × 800 | `recolhimento` |
| Etapas de "Como funciona" | Reservado para as quatro etapas | 1:1, mínimo 512 × 512 | `etapas` |
| Logomarca | Cabeçalho, rodapé e página inicial | quadrada, 512 × 512, fundo transparente | `logomarca` (a ligar ao `Marca`) |

Para inserir:

1. Copie os arquivos para `frontend/assets/imagens/` (PNG ou WebP otimizados).
2. Declare a pasta no `pubspec.yaml`, em `flutter: assets: - assets/imagens/`.
3. Preencha o caminho em `lib/ui/compartilhado/espaco_imagem.dart` (`AssetsVisuais`).
4. Rode `flutter build web` e confira as capturas com `tool/capturar_pagina_inteira.py`.

`EspacoImagem` mantém a proporção, arredonda os cantos e volta ao conteúdo provisório se o
arquivo não carregar. Imagens são decorativas para leitores de tela; o texto ao lado já
descreve cada seção.
