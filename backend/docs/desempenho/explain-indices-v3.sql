-- Avaliação dos índices candidatos da V3 (Issue #33), em volume ampliado e ISOLADO.
--
-- NÃO é a medição oficial do RNF04 (que usa 1.000 solicitações) e nunca se mistura com ela. Rode em um banco
-- DESCARTÁVEL com o dataset e o volume ampliado carregados (carga-dataset.sql + carga-volume-ampliado.sql, 100.000
-- solicitações). O script é uma só transação que termina em ROLLBACK: cria os índices candidatos só para medir e não deixa
-- nada no banco, então pode ser repetido.
--   docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho_xl -v ON_ERROR_STOP=1 < docs/desempenho/explain-indices-v3.sql
--
-- Etapa A: consultas reais (as do k6 mais as de detalhe, RN05 e ADR 0013) SEM os índices.
-- Etapa B: as mesmas consultas COM os índices candidatos. Compare o plano e o tempo de execução de cada uma.
-- Etapa C: filtros do RF06 sobre poc_ponto_coleta, que só tem 20 linhas; para ver o planejador escolher um índice de
--          expressão, a transação acrescenta 20.000 pontos sintéticos (descartados no ROLLBACK).
-- Etapa D: custo de escrita dos índices (inserção de solicitações com e sem eles), também descartada no ROLLBACK.

BEGIN;
SET LOCAL search_path TO coleta;
SET LOCAL TimeZone TO 'America/Sao_Paulo';
ANALYZE;


\echo '######## ETAPA A: SEM índices (volume ampliado, 100.000 solicitações)'
\echo '=== RF09 ADMIN situação CONCLUIDA (45% das linhas): página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'CONCLUIDA' order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN situação EM_RECOLHIMENTO (5%): página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'EM_RECOLHIMENTO' order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN situação EM_RECOLHIMENTO (5%): contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'EM_RECOLHIMENTO';

\echo '=== RF09 ADMIN por ponto: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN por ponto: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %');

\echo '=== RF09 ADMIN por período (2 meses): página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN por período (2 meses): contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03';

\echo '=== RF09 ADMIN sem filtro, página 3 de 100'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 300 rows fetch first 100 rows only;

\echo '=== RF09 ADMIN sem filtro: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0;

\echo '=== RF09 MORADOR sem filtro: página (500 linhas do morador)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 MORADOR sem filtro: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%');

\echo '=== RF09 MORADOR por situação: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'AGENDADA' and s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 OPERADOR sem filtro: página (5.000 linhas do ponto)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 OPERADOR por situação: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'CONCLUIDA' and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 OPERADOR por situação: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'CONCLUIDA' and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %');

\echo '=== RF09 OPERADOR por período: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF15 solicitações por situação, sem período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_tx_situacao, count(s1_0.sol_nr_id) from sol_solicitacao s1_0 group by 1;

\echo '=== RF15 solicitações por situação, com período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_tx_situacao, count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' group by 1;

\echo '=== RF15 quantidades recolhidas, sem período'
EXPLAIN (ANALYZE, BUFFERS)
select m1_0.mat_nr_id, m1_0.mat_tx_nome, is1_0.iso_tx_copia_unidade, sum(is1_0.iso_nr_quantidade_efetiva) from iso_item_solicitacao is1_0 join sol_solicitacao s1_0 on s1_0.sol_nr_id = is1_0.sol_nr_id join mat_material m1_0 on m1_0.mat_nr_id = is1_0.mat_nr_id where s1_0.sol_tx_situacao = 'CONCLUIDA' and is1_0.iso_nr_quantidade_efetiva is not null group by 1, 2, 3 order by 2, 3;

\echo '=== RF15 quantidades recolhidas, com período de conclusão'
EXPLAIN (ANALYZE, BUFFERS)
select m1_0.mat_nr_id, m1_0.mat_tx_nome, is1_0.iso_tx_copia_unidade, sum(is1_0.iso_nr_quantidade_efetiva) from iso_item_solicitacao is1_0 join sol_solicitacao s1_0 on s1_0.sol_nr_id = is1_0.sol_nr_id join mat_material m1_0 on m1_0.mat_nr_id = is1_0.mat_nr_id where s1_0.sol_tx_situacao = 'CONCLUIDA' and is1_0.iso_nr_quantidade_efetiva is not null and s1_0.sol_dt_conclusao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_conclusao < '2026-03-01 00:00:00-03' group by 1, 2, 3 order by 2, 3;

\echo '=== Detalhe da solicitação: histórico (HistoricoRepository.findBySolicitacaoIdOrderById)'
EXPLAIN (ANALYZE, BUFFERS)
select h1_0.his_nr_id, h1_0.his_tx_situacao_nova, h1_0.his_dt_data from his_historico h1_0 where h1_0.sol_nr_id = 50000 order by h1_0.his_nr_id;

\echo '=== RN05: abertas do ponto com item do material (SolicitacaoRepository.contarPorPontoESituacaoComMaterial)'
EXPLAIN (ANALYZE, BUFFERS)
select count(distinct s.sol_nr_id) from sol_solicitacao s join iso_item_solicitacao i on i.sol_nr_id = s.sol_nr_id where s.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') and s.sol_tx_situacao in ('SOLICITADA','AGENDADA','EM_RECOLHIMENTO') and i.mat_nr_id = (select min(mat_nr_id) from mat_material);

\echo '=== RN05: abertas do ponto (countByPontoIdAndSituacaoIn)'
EXPLAIN (ANALYZE, BUFFERS)
select count(s.sol_nr_id) from sol_solicitacao s where s.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') and s.sol_tx_situacao in ('SOLICITADA','AGENDADA','EM_RECOLHIMENTO');

\echo '=== ADR 0013: itens do material (MaterialRepository.contarItensDeSolicitacao)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*) FROM iso_item_solicitacao WHERE mat_nr_id = (select min(mat_nr_id) from mat_material);

\echo '=== ADR 0013: vínculos do material (MaterialRepository.contarVinculosComPontos)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*) FROM mac_material_aceito WHERE mat_nr_id = (select min(mat_nr_id) from mat_material);

\echo '######## ETAPA B: criação dos índices candidatos'
\echo '--- I1 idx_sol_poc_criacao'
CREATE INDEX idx_sol_poc_criacao ON sol_solicitacao (poc_nr_id, sol_dt_criacao DESC, sol_nr_id DESC);
\echo '--- I2 idx_sol_mor_criacao'
CREATE INDEX idx_sol_mor_criacao ON sol_solicitacao (mor_nr_id, sol_dt_criacao DESC, sol_nr_id DESC);
\echo '--- I3 idx_sol_criacao'
CREATE INDEX idx_sol_criacao ON sol_solicitacao (sol_dt_criacao DESC, sol_nr_id DESC);
\echo '--- I4 idx_sol_situacao_criacao'
CREATE INDEX idx_sol_situacao_criacao ON sol_solicitacao (sol_tx_situacao, sol_dt_criacao DESC, sol_nr_id DESC);
\echo '--- I5 idx_sol_conclusao (parcial: CONCLUIDA)'
CREATE INDEX idx_sol_conclusao ON sol_solicitacao (sol_dt_conclusao) WHERE sol_tx_situacao = 'CONCLUIDA';
\echo '--- I6 idx_iso_mat'
CREATE INDEX idx_iso_mat ON iso_item_solicitacao (mat_nr_id);
\echo '--- I7 idx_his_sol'
CREATE INDEX idx_his_sol ON his_historico (sol_nr_id);
\echo '--- I8 idx_mac_mat'
CREATE INDEX idx_mac_mat ON mac_material_aceito (mat_nr_id);
ANALYZE;

\echo '######## ETAPA B: COM os índices candidatos I1 a I8'
\echo '=== RF09 ADMIN situação CONCLUIDA (45% das linhas): página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'CONCLUIDA' order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN situação EM_RECOLHIMENTO (5%): página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'EM_RECOLHIMENTO' order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN situação EM_RECOLHIMENTO (5%): contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'EM_RECOLHIMENTO';

\echo '=== RF09 ADMIN por ponto: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN por ponto: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %');

\echo '=== RF09 ADMIN por período (2 meses): página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 ADMIN por período (2 meses): contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03';

\echo '=== RF09 ADMIN sem filtro, página 3 de 100'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 300 rows fetch first 100 rows only;

\echo '=== RF09 ADMIN sem filtro: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0;

\echo '=== RF09 MORADOR sem filtro: página (500 linhas do morador)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 MORADOR sem filtro: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%');

\echo '=== RF09 MORADOR por situação: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'AGENDADA' and s1_0.mor_nr_id = (select min(mor_nr_id) from mor_morador where mor_tx_email like 'perf.morador.%') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 OPERADOR sem filtro: página (5.000 linhas do ponto)'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 OPERADOR por situação: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'CONCLUIDA' and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF09 OPERADOR por situação: contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_tx_situacao = 'CONCLUIDA' and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %');

\echo '=== RF09 OPERADOR por período: página'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_nr_id, s1_0.sol_tx_protocolo, s1_0.sol_tx_situacao, s1_0.sol_dt_criacao from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' and s1_0.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') order by s1_0.sol_dt_criacao desc, s1_0.sol_nr_id desc offset 0 rows fetch first 50 rows only;

\echo '=== RF15 solicitações por situação, sem período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_tx_situacao, count(s1_0.sol_nr_id) from sol_solicitacao s1_0 group by 1;

\echo '=== RF15 solicitações por situação, com período'
EXPLAIN (ANALYZE, BUFFERS)
select s1_0.sol_tx_situacao, count(s1_0.sol_nr_id) from sol_solicitacao s1_0 where s1_0.sol_dt_criacao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_criacao < '2026-03-01 00:00:00-03' group by 1;

\echo '=== RF15 quantidades recolhidas, sem período'
EXPLAIN (ANALYZE, BUFFERS)
select m1_0.mat_nr_id, m1_0.mat_tx_nome, is1_0.iso_tx_copia_unidade, sum(is1_0.iso_nr_quantidade_efetiva) from iso_item_solicitacao is1_0 join sol_solicitacao s1_0 on s1_0.sol_nr_id = is1_0.sol_nr_id join mat_material m1_0 on m1_0.mat_nr_id = is1_0.mat_nr_id where s1_0.sol_tx_situacao = 'CONCLUIDA' and is1_0.iso_nr_quantidade_efetiva is not null group by 1, 2, 3 order by 2, 3;

\echo '=== RF15 quantidades recolhidas, com período de conclusão'
EXPLAIN (ANALYZE, BUFFERS)
select m1_0.mat_nr_id, m1_0.mat_tx_nome, is1_0.iso_tx_copia_unidade, sum(is1_0.iso_nr_quantidade_efetiva) from iso_item_solicitacao is1_0 join sol_solicitacao s1_0 on s1_0.sol_nr_id = is1_0.sol_nr_id join mat_material m1_0 on m1_0.mat_nr_id = is1_0.mat_nr_id where s1_0.sol_tx_situacao = 'CONCLUIDA' and is1_0.iso_nr_quantidade_efetiva is not null and s1_0.sol_dt_conclusao >= '2026-01-01 00:00:00-03' and s1_0.sol_dt_conclusao < '2026-03-01 00:00:00-03' group by 1, 2, 3 order by 2, 3;

\echo '=== Detalhe da solicitação: histórico (HistoricoRepository.findBySolicitacaoIdOrderById)'
EXPLAIN (ANALYZE, BUFFERS)
select h1_0.his_nr_id, h1_0.his_tx_situacao_nova, h1_0.his_dt_data from his_historico h1_0 where h1_0.sol_nr_id = 50000 order by h1_0.his_nr_id;

\echo '=== RN05: abertas do ponto com item do material (SolicitacaoRepository.contarPorPontoESituacaoComMaterial)'
EXPLAIN (ANALYZE, BUFFERS)
select count(distinct s.sol_nr_id) from sol_solicitacao s join iso_item_solicitacao i on i.sol_nr_id = s.sol_nr_id where s.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') and s.sol_tx_situacao in ('SOLICITADA','AGENDADA','EM_RECOLHIMENTO') and i.mat_nr_id = (select min(mat_nr_id) from mat_material);

\echo '=== RN05: abertas do ponto (countByPontoIdAndSituacaoIn)'
EXPLAIN (ANALYZE, BUFFERS)
select count(s.sol_nr_id) from sol_solicitacao s where s.poc_nr_id = (select min(poc_nr_id) from poc_ponto_coleta where poc_tx_nome like 'PERF %') and s.sol_tx_situacao in ('SOLICITADA','AGENDADA','EM_RECOLHIMENTO');

\echo '=== ADR 0013: itens do material (MaterialRepository.contarItensDeSolicitacao)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*) FROM iso_item_solicitacao WHERE mat_nr_id = (select min(mat_nr_id) from mat_material);

\echo '=== ADR 0013: vínculos do material (MaterialRepository.contarVinculosComPontos)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*) FROM mac_material_aceito WHERE mat_nr_id = (select min(mat_nr_id) from mat_material);

\echo '######## ETAPA C: poc_ponto_coleta com 20.020 pontos sintéticos'
INSERT INTO poc_ponto_coleta (poc_tx_nome, poc_tx_situacao, poc_bl_rec_domiciliar, poc_tx_logradouro, poc_tx_numero,
                              poc_tx_bairro, poc_tx_municipio, poc_tx_uf, poc_tx_cep)
SELECT 'SINT Ponto ' || n, 'ATIVO', TRUE, 'Rua ' || n, n::text,
       'Bairro ' || (n % 300), 'Municipio ' || (n % 75), 'SE', '49400000'
FROM generate_series(1, 20000) AS n;
INSERT INTO poc_ponto_coleta (poc_tx_nome, poc_tx_situacao, poc_bl_rec_domiciliar, poc_tx_logradouro, poc_tx_numero,
                              poc_tx_bairro, poc_tx_municipio, poc_tx_uf, poc_tx_cep)
VALUES ('SINT Alvo', 'ATIVO', TRUE, 'Rua Alvo', '1', 'Centro', 'Lagarto', 'SE', '49400000');
ANALYZE poc_ponto_coleta;

\echo '######## ETAPA C: pontos SEM índices de expressão'
\echo '=== RF06 por município (lower)'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id from poc_ponto_coleta pc1_0 where lower(pc1_0.poc_tx_municipio) = 'lagarto' and pc1_0.poc_tx_situacao = 'ATIVO' order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '=== RF06 por bairro (lower)'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id from poc_ponto_coleta pc1_0 where lower(pc1_0.poc_tx_bairro) = 'centro' and pc1_0.poc_tx_situacao = 'ATIVO' order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '--- I9 idx_poc_municipio_lower'
CREATE INDEX idx_poc_municipio_lower ON poc_ponto_coleta (lower(poc_tx_municipio));
\echo '--- I10 idx_poc_bairro_lower'
CREATE INDEX idx_poc_bairro_lower ON poc_ponto_coleta (lower(poc_tx_bairro));
ANALYZE poc_ponto_coleta;

\echo '######## ETAPA C: pontos COM índices de expressão I9 e I10'
\echo '=== RF06 por município (lower)'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id from poc_ponto_coleta pc1_0 where lower(pc1_0.poc_tx_municipio) = 'lagarto' and pc1_0.poc_tx_situacao = 'ATIVO' order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '=== RF06 por bairro (lower)'
EXPLAIN (ANALYZE, BUFFERS)
select pc1_0.poc_nr_id from poc_ponto_coleta pc1_0 where lower(pc1_0.poc_tx_bairro) = 'centro' and pc1_0.poc_tx_situacao = 'ATIVO' order by pc1_0.poc_nr_id offset 0 rows fetch first 20 rows only;

\echo '######## ETAPA D: custo de escrita (1.000 inserções de solicitação, descartadas no ROLLBACK)'
\echo '=== COM os índices I1 a I5 em sol_solicitacao'
EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao,
                             sol_dt_criacao, sol_tx_observacoes, sol_nr_versao)
SELECT o.sol_tx_protocolo || '-Y1', o.mor_nr_id, o.poc_nr_id, o.sol_tx_endereco_copia, 'SOLICITADA',
       o.sol_dt_criacao, o.sol_tx_observacoes, 0
FROM sol_solicitacao o WHERE o.sol_tx_protocolo NOT LIKE '%-X%' AND o.sol_tx_protocolo NOT LIKE '%-Y%'
ORDER BY o.sol_nr_id LIMIT 1000;

DROP INDEX idx_sol_poc_criacao, idx_sol_mor_criacao, idx_sol_criacao, idx_sol_situacao_criacao, idx_sol_conclusao;
\echo '=== SEM os índices (somente a PK e a UNIQUE do protocolo)'
EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao,
                             sol_dt_criacao, sol_tx_observacoes, sol_nr_versao)
SELECT o.sol_tx_protocolo || '-Y2', o.mor_nr_id, o.poc_nr_id, o.sol_tx_endereco_copia, 'SOLICITADA',
       o.sol_dt_criacao, o.sol_tx_observacoes, 0
FROM sol_solicitacao o WHERE o.sol_tx_protocolo NOT LIKE '%-X%' AND o.sol_tx_protocolo NOT LIKE '%-Y%'
ORDER BY o.sol_nr_id LIMIT 1000;

ROLLBACK;
