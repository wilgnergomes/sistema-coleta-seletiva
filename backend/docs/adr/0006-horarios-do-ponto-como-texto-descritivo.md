# 6. Horários de funcionamento do ponto como texto descritivo

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

O cadastro do ponto de coleta inclui os horários de funcionamento (RF04), e a pesquisa de pontos deve exibi-los (RF06). A especificação não exige filtrar pontos por horário. O primeiro rascunho do modelo gerado no pgAdmin guardava os horários como lista de textos (`varchar[]`).

Foram consideradas as seguintes alternativas: lista de textos em uma coluna, que quebra a primeira forma normal e dificulta consulta e validação; tabela filha com dia da semana, hora de abertura e hora de fechamento, que permite filtrar pontos abertos em determinado momento, mas aumenta a quantidade de tabelas, endpoints e regras de validação; e uma coluna de texto descritivo. Escolheu-se a terceira porque o requisito pede apenas exibir os horários, e o texto livre atende isso com o menor custo.

## Decisão

Os horários ficam em `poc_tx_horarios`, uma coluna `varchar(500)` opcional com texto descritivo (por exemplo, "Segunda a sexta, das 8h às 17h"). Quando informada, não pode ser em branco (`CHECK`); o backend normaliza texto vazio para `NULL`. O contato (`poc_tx_contato`) segue a mesma regra de opcionalidade, pois o aceite do RF04 exige apenas nome e endereço.

## Consequências

- **Positivas:** modelo e API simples; nenhum tratamento de horário inválido ou sobreposto; atende ao RF04 e ao RF06 sem estrutura adicional.
- **Negativas:** não é possível pesquisar ou ordenar por "aberto agora"; o formato do texto não é padronizado, então a qualidade depende de quem cadastra.
- **Cuidados:** se uma extensão exigir filtro por horário, criar uma migração que adicione a tabela filha e converta o texto existente, mantendo a coluna até a conversão terminar. A interface deve tratar a ausência de horário, por exemplo exibindo "horário não informado".
