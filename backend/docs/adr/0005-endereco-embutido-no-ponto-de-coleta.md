# 5. Endereço do ponto de coleta embutido no cadastro do ponto

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

A especificação define o endereço como entidade ligada ao morador e permite que o endereço do ponto de coleta seja embutido no cadastro do ponto (seção 10). Era preciso decidir como representar o endereço do ponto no banco.

Foram consideradas as seguintes alternativas: reaproveitar a tabela `end_endereco`, o que exigiria tornar o morador dono opcional ou criar um segundo tipo de dono, com uma regra extra para garantir que existe exatamente um dono; criar uma tabela de endereços compartilhada pelas duas entidades, com FK em cada uma, o que acrescenta um join às consultas de pontos; e guardar o endereço em colunas do próprio ponto. Escolheu-se a terceira porque o ponto possui exatamente um endereço, que não é compartilhado com nenhuma outra entidade, e porque a pesquisa por município e bairro (RF06) passa a usar apenas a tabela do ponto.

## Decisão

O endereço do ponto fica em colunas de `poc_ponto_coleta`: `poc_tx_logradouro`, `poc_tx_numero`, `poc_tx_bairro`, `poc_tx_municipio`, `poc_tx_uf`, `poc_tx_cep` e `poc_tx_complemento` (opcional). As regras são as mesmas do endereço do morador: número informado ou `S/N` de forma explícita, sem valor padrão; UF com duas letras maiúsculas; CEP com oito dígitos, sem máscara. Todos os campos, exceto o complemento, são obrigatórios, conforme o aceite do RF04 (ponto sem endereço é rejeitado).

## Consequências

- **Positivas:** modelo mais simples para o ponto, sem tabela extra nem dono opcional; consultas de pesquisa e exibição (RF06) sem join; o endereço do ponto aparece junto do restante do cadastro.
- **Negativas:** as regras de formato (UF, CEP, número) existem em duas tabelas e precisam ser mantidas iguais; se um ponto passar a ter mais de um endereço, será necessária uma migração para extrair uma tabela própria.
- **Cuidados:** ao alterar uma regra de endereço (por exemplo, o formato do CEP), atualizar `end_endereco` e `poc_ponto_coleta` juntas, tanto na migração quanto no Bean Validation. A mesma normalização do backend (CEP só com dígitos, `S/N` padronizado) deve valer para as duas.
