# 7. Itens e histórico como tabelas ligadas à solicitação

**Data:** 2026-10-12

**Status:** Proposta

## Contexto

A especificação exige que itens e histórico permaneçam relacionados à solicitação (seção 10). Cada solicitação tem um ou mais itens, com material e quantidade (RF08), e cada mudança de situação deve ser registrada com situação anterior e nova, data, responsável e motivo (RN03). O mesmo material não pode aparecer duas vezes na mesma solicitação (RN01), e os totais por material precisam somar apenas atendimentos concluídos, sem misturar unidades (RF15).

Foram consideradas as seguintes alternativas: guardar itens e histórico como lista ou JSON dentro da solicitação, o que impede somar por material e filtrar com consultas simples; usar chave própria nos itens com um `UNIQUE` separado em solicitação e material; e usar chave primária composta nos itens. Para as FKs, considerou-se também a exclusão em cascata. Escolheram-se tabelas separadas, chave composta nos itens e nenhuma cascata.

## Decisão

1. `iso_item_solicitacao` tem chave primária composta `(sol_nr_id, mat_nr_id)`, que garante o vínculo único por solicitação e material (RN01) sem uma segunda constraint.
2. `his_historico` tem chave própria e FK para a solicitação. Os registros são apenas inseridos: o backend nunca altera nem apaga uma linha de histórico.
3. Todas as FKs usam `ON DELETE RESTRICT`. Não há exclusão em cascata, e solicitações não são excluídas fisicamente (RN03).
4. A criação da solicitação, dos itens e do histórico inicial ocorre em uma única transação (ver ADR 9).

## Consequências

- **Positivas:** os totais do RF15 saem de consultas diretas; a unicidade do material por solicitação é garantida pelo banco; o histórico preserva todas as tentativas, inclusive reagendamentos (RF14).
- **Negativas:** a leitura completa de uma solicitação envolve três tabelas; é preciso cuidado para evitar consultas N+1 nas listagens.
- **Cuidados:** como não há cascata, nenhuma rotina de limpeza pode apagar uma solicitação sem antes remover itens e histórico, e isso não deve ser feito. O histórico deve ser tratado como imutável também na camada de serviço (sem métodos de atualização ou exclusão). Índices em `iso_item_solicitacao (mat_nr_id)` e `his_historico (sol_nr_id, his_dt_data)` sustentam o RF15 e a exibição do histórico (RNF04).
