-- EXPLAIN (ANALYZE, BUFFERS) das consultas reais de RF06, RF09 e RF15 (Issue #58).
--
-- As instruções abaixo são as que o Hibernate enviou ao PostgreSQL (capturadas com log_statement='all' no banco de
-- desempenho), com os parâmetros trocados por literais. Rode sobre o dataset PERF carregado, ANTES e DEPOIS dos
-- índices da V3, para comparar os planos:
--   docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho < docs/desempenho/explain-consultas.sql
--
-- O EXPLAIN ANALYZE executa a consulta de verdade (só leituras aqui). Não faz parte da medição do k6.

SET search_path TO coleta;
SET TimeZone TO 'America/Sao_Paulo';

-- Estatísticas atualizadas, para o plano não depender do momento do autovacuum
ANALYZE;

\echo '=== RF06 (a) pontos por município'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id, pc1_0.poc_tx_nome from poc_ponto_coleta pc1_0
where 1=1 and lower(pc1_0.poc_tx_municipio) = 'lagarto' and pc1_0.poc_tx_situacao = 'ATIVO'
order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '=== RF06 (b) pontos por nome e bairro'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id, pc1_0.poc_tx_nome from poc_ponto_coleta pc1_0
where 1=1 and lower(pc1_0.poc_tx_nome) like '%perf%' escape '\' and lower(pc1_0.poc_tx_bairro) = 'centro'
  and pc1_0.poc_tx_situacao = 'ATIVO'
order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '=== RF06 (c) materiais aceitos dos pontos da página'
EXPLAIN (ANALYZE, BUFFERS)
select ma1_0.mat_nr_id, ma1_0.poc_nr_id, ma1_0.mac_nr_limite, m1_0.mat_tx_nome
from mac_material_aceito ma1_0 join mat_material m1_0 on m1_0.mat_nr_id = ma1_0.mat_nr_id
where ma1_0.poc_nr_id in (1, 9, 20)
order by ma1_0.mat_nr_id;

\echo '=== RF09 (a) página por situação'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.sol_tx_situacao = 'CONCLUIDA' and 1=1
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (a) contagem por situação'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where 1=1 and s1_0.sol_tx_situacao = 'CONCLUIDA' and 1=1;

\echo '=== RF09 (b) página por ponto'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.poc_nr_id = 1 and 1=1
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (b) contagem por ponto'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where 1=1 and s1_0.poc_nr_id = 1 and 1=1;

\echo '=== RF09 (c) página por período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' and 1=1
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (c) contagem por período'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0
where 1=1 and s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' and 1=1;

\echo '=== RF09 (d) página sem filtro (página 3 de 100)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and 1=1
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 300 rows fetch first 100 rows only;

\echo '=== RF15 (a) solicitações por situação, sem período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_tx_situacao, count(s1_0.sol_nr_id) from sol_solicitacao s1_0 group by 1;

\echo '=== RF15 (b) solicitações por situação, com período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_tx_situacao, count(s1_0.sol_nr_id) from sol_solicitacao s1_0
where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' group by 1;

\echo '=== RF15 (c) quantidades recolhidas, sem período'
EXPLAIN (ANALYZE, BUFFERS)
select m1_0.mat_nr_id, m1_0.mat_tx_nome, is1_0.iso_tx_copia_unidade, sum(is1_0.iso_nr_quantidade_efetiva)
from iso_item_solicitacao is1_0
join sol_solicitacao s1_0 on s1_0.sol_nr_id = is1_0.sol_nr_id
join mat_material m1_0 on m1_0.mat_nr_id = is1_0.mat_nr_id
where s1_0.sol_tx_situacao = 'CONCLUIDA' and is1_0.iso_nr_quantidade_efetiva is not null
group by 1, 2, 3 order by 2, 3;

\echo '=== RF15 (d) quantidades recolhidas, com período de conclusão'
EXPLAIN (ANALYZE, BUFFERS)
select m1_0.mat_nr_id, m1_0.mat_tx_nome, is1_0.iso_tx_copia_unidade, sum(is1_0.iso_nr_quantidade_efetiva)
from iso_item_solicitacao is1_0
join sol_solicitacao s1_0 on s1_0.sol_nr_id = is1_0.sol_nr_id
join mat_material m1_0 on m1_0.mat_nr_id = is1_0.mat_nr_id
where s1_0.sol_tx_situacao = 'CONCLUIDA' and is1_0.iso_nr_quantidade_efetiva is not null
  and s1_0.sol_dt_conclusao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_conclusao < '2026-03-01 00:00:00-03'
group by 1, 2, 3 order by 2, 3;

-- ---------------------------------------------------------------------------------------------------------
-- Variantes acrescentadas ao roteiro definitivo do k6 (SQL capturado do Hibernate com os mesmos parâmetros).
-- Os ids do ponto, do morador e do operador vêm de subconsultas "min(...)" sobre os registros PERF, então valem
-- mesmo depois de limpar e recarregar o dataset.
-- ---------------------------------------------------------------------------------------------------------

\echo '=== RF06 (e) pesquisa por material (EXISTS no vínculo)'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id, pc1_0.poc_tx_nome from poc_ponto_coleta pc1_0
where 1=1 and lower(pc1_0.poc_tx_nome) like '%perf%' escape '\'
  and exists(select 1 from mac_material_aceito ma1_0
             where ma1_0.poc_nr_id = pc1_0.poc_nr_id
               and ma1_0.mat_nr_id = (select min(mat_nr_id) from mat_material where mat_tx_nome like 'PERF %'))
  and pc1_0.poc_tx_situacao = 'ATIVO'
order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '=== RF06 (f) filtros combinados: nome, município, bairro e material'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id, pc1_0.poc_tx_nome from poc_ponto_coleta pc1_0
where 1=1 and lower(pc1_0.poc_tx_nome) like '%perf%' escape '\'
  and lower(pc1_0.poc_tx_municipio) = 'lagarto' and lower(pc1_0.poc_tx_bairro) = 'centro'
  and exists(select 1 from mac_material_aceito ma1_0
             where ma1_0.poc_nr_id = pc1_0.poc_nr_id
               and ma1_0.mat_nr_id = (select min(mat_nr_id) from mat_material where mat_tx_nome like 'PERF %'))
  and pc1_0.poc_tx_situacao = 'ATIVO'
order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '=== RF06 (g) paginação: página 1 de 5 pontos'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id, pc1_0.poc_tx_nome from poc_ponto_coleta pc1_0
where 1=1 and lower(pc1_0.poc_tx_nome) like '%perf%' escape '\' and pc1_0.poc_tx_situacao = 'ATIVO'
order by pc1_0.poc_nr_id offset 5 rows fetch first 5 rows only;

\echo '=== RF06 (g) paginação: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(pc1_0.poc_nr_id) from poc_ponto_coleta pc1_0
where 1=1 and lower(pc1_0.poc_tx_nome) like '%perf%' escape '\' and pc1_0.poc_tx_situacao = 'ATIVO';

\echo '=== RF09 (e) MORADOR sem filtro (contexto: só as do morador)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%')
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (f) MORADOR por situação'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.sol_tx_situacao = 'CONCLUIDA'
  and s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%')
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (g) MORADOR por período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03'
  and s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%')
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (h) OPERADOR sem filtro (contexto: só as do ponto do operador)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %')
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (h) OPERADOR sem filtro: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0
where 1=1 and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %');

\echo '=== RF09 (i) OPERADOR por situação'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.sol_tx_situacao = 'CONCLUIDA'
  and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %')
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 (i) OPERADOR por situação: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0
where 1=1 and s1_0.sol_tx_situacao = 'CONCLUIDA'
  and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %');

\echo '=== RF09 (j) OPERADOR por período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0
where 1=1 and s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03'
  and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %')
order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;
