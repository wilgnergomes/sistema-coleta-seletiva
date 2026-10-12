# 10. Vínculo 1:N entre ponto de coleta e operador

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

O modelo sugerido descreve o operador com vínculos com pontos de coleta (seção 10). O operador consulta e atua apenas nas solicitações do seu ponto (seção 3 e RF07), e o administrador gerencia os responsáveis.

Foram consideradas três alternativas: relação 1:1, que limita cada ponto a um único operador; relação 1:N, em que cada operador pertence a um único ponto, mas um ponto pode ter vários operadores; e relação N:N, que permite que operadores atendam a múltiplos pontos.

A relação 1:N atende à necessidade de permitir vários operadores no mesmo ponto, sem exigir que um operador esteja vinculado a mais de um ponto. Além disso, simplifica a estrutura do banco, eliminando a necessidade de uma tabela associativa.

## Decisão

A coluna `poc_nr_id` passa a integrar diretamente a tabela `ope_operador`, como campo obrigatório (`NOT NULL`) e chave estrangeira para `poc_ponto_coleta`, com `ON DELETE RESTRICT`.

A tabela `opp_operador_ponto` é removida do modelo. Um índice não único, denominado `idx_ope_poc`, é criado sobre `ope_operador.poc_nr_id` para otimizar consultas de operadores vinculados a determinado ponto.

O relacionamento resultante é 1:N: cada operador pertence obrigatoriamente a um único ponto de coleta, enquanto um ponto pode possuir vários operadores.

A autorização para atuação continua sendo validada no backend. O vínculo estrutural com o ponto não substitui a verificação de que o operador está ativo nem as regras de acesso às solicitações (RF07).

## Consequências

- **Positivas:** estrutura mais simples; ausência de tabela associativa; integridade referencial garantida pela chave estrangeira; índice que facilita a consulta dos operadores de um ponto.
- **Negativas:** um operador não pode estar vinculado simultaneamente a mais de um ponto; a mudança futura para um relacionamento N:N exigirá alteração da estrutura do banco e migração dos dados.
- **Cuidados:** em toda operação restrita, de leitura ou escrita, o serviço deve identificar o ponto da solicitação carregada do banco, nunca confiar em um valor de ponto enviado diretamente no corpo da requisição, e confirmar que o operador está ativo e autorizado a atuar naquele ponto (RF07). Os testes de permissão exigidos pelo RNF06 e RNF08 devem ser mantidos. A chave estrangeira com `ON DELETE RESTRICT` preserva a integridade referencial e impede a exclusão física de pontos que possuam operadores vinculados. A inativação deve ser utilizada quando aplicável, conforme RN03.