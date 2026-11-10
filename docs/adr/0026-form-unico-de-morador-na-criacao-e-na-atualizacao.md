# 26. Form único de morador na criação e na atualização

**Data:** 2026-11-10

**Status:** Aceita (decisão aprovada pelo estudante em 2026-11-10). Substitui as decisões 1, 2 e 4 do ADR 0021.

## Contexto

O ADR 0021 separou o cadastro do morador em dois formulários e dois schemas: `MoradorCriacaoForm` (`MoradorCriacao`, sem `situacao`) e `MoradorAtualizacaoForm` (`MoradorEntrada`, com `situacao` opcional). Na prática os dois eram idênticos, exceto pelo campo `situacao`, e o `MoradorService` mantinha dois métodos `normalizar` iguais.

Em 2026-11-10, numa refatoração de padronização de Forms e Mappers (branch `refactor/padroniza-forms-e-mappers`), o estudante pediu a unificação em um único `MoradorForm`, sem alterar as regras de negócio do morador.

O ADR 0021 já havia considerado a alternativa de um formulário único com `situacao` opcional e a rejeitou, porque o campo ficaria sem efeito na criação e o contrato descreveria algo que o código ignora. Esse risco continua existindo e é aceito, com os cuidados abaixo.

Alternativas consideradas:

1. **Manter os dois formulários (ADR 0021).** O contrato impede o campo por construção, mas duplica o registro, o `normalizar` e os schemas, para uma diferença de um campo opcional.
2. **Form único; a criação ignora `situacao` no mapper (escolhida).** Reduz a duplicação; a regra "nasce ATIVO" fica explícita e testada em um só lugar, e o contrato precisa documentá-la.
3. **Form único; a criação rejeita `situacao` com 4xx.** Deixa a regra visível ao cliente, mas torna inválido um corpo que hoje é aceito e muda o comportamento da API sem necessidade.

## Decisão

1. O `POST /moradores` e o `PUT /moradores/{id}` usam o mesmo form (`MoradorForm`) e o mesmo schema (`MoradorEntrada`), com `nome`, `email`, `fone` e `situacao` opcional. `MoradorCriacaoForm` e o schema `MoradorCriacao` foram removidos.
2. Na criação, `situacao` é sempre ignorada: o `MoradorMapper.paraEntidade` usa setters explícitos para `nome`, `email` e `fone` e não copia `situacao`, preservando o padrão ATIVO da entidade. Todo morador nasce ATIVO, seja qual for o JSON.
3. Um valor de `situacao` que não seja ATIVO nem INATIVO continua rejeitado com 400 `VALOR_INVALIDO`, pois a conversão do enum acontece antes do serviço. Antes da unificação o `POST` ignorava qualquer valor, inclusive inválido.
4. Permanecem como no ADR 0021: a decisão 3 (só o administrador altera a situação, verificado no serviço) e a decisão 5 (normalização do e-mail, nome e telefone e verificação de duplicidade no serviço). O `PUT` sem `situacao` mantém a situação atual.
5. O OpenAPI descreve a regra no `POST /moradores`: o corpo é `MoradorEntrada`, `situacao` é ignorada e o morador nasce ATIVO.
6. Esta decisão vale só para o morador. Material (RF03, `POST` aceita `situacao` opcional, inclusive INATIVO), ponto de coleta e operador não foram alterados.

## Consequências

- **Positivas:** um form, um schema e um `normalizar` a menos; a regra de situação inicial fica em um ponto só (o mapper) e é coberta por testes no serviço, na API e no banco.
- **Negativas:** o contrato do `POST` passa a exibir um campo que a criação ignora; o único modo de manter o contrato fiel é a descrição no OpenAPI e os testes.
- **Cuidados:** os testes devem cobrir `POST` com `situacao: INATIVO` criando morador ATIVO (resposta e banco) e `POST` com valor inválido retornando 400 sem gravar. Quem alterar `MoradorMapper.paraEntidade` não deve passar a copiar `situacao`.
- **Relações:** substitui as decisões 1, 2 e 4 do ADR 0021, que permanece como registro histórico. A menção a `MoradorCriacaoForm` e `MoradorAtualizacaoForm` no ADR 0020 deve ser lida como histórica.
