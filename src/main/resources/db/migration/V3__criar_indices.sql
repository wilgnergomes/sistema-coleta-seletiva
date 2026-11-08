-- =====================================================================
-- V3: índices de desempenho (RNF04, Issue #33).
--
-- Cada índice abaixo foi escolhido por um EXPLAIN (ANALYZE, BUFFERS) das consultas reais da aplicação. A justificativa
-- original veio de um volume ampliado e isolado (100.000 solicitações), onde os candidatos foram avaliados criados em
-- conjunto. Na medição oficial do RNF04, com 1.000 solicitações, o PostgreSQL também passou a usar alguns desses
-- índices (por exemplo, em sol_solicitacao), embora o ganho ponta a ponta não seja distinguível da variação entre
-- execuções. Os índices de poc_ponto_coleta só são usados com muitos pontos (a tabela real tem 20 linhas).
--
-- Candidatos avaliados e descartados nesta migração, para o cenário atual (não é uma proibição permanente):
--   idx_sol_conclusao: ganho limitado (cerca de 20% no RF15 com período);
--   idx_iso_mat: ganho restrito a uma operação administrativa (verificação do ADR 0013), sem efeito nas listagens;
--   idx_mac_mat: sem ganho relevante (101 linhas; o RF06 já usa a chave primária composta).
-- Evidência e detalhes: docs/desempenho/analise-indices-v3.md e docs/desempenho/relatorio-rnf04.md.
--
-- As colunas de data e de id seguem DESC para casar com o ORDER BY das listagens (mais recentes primeiro, id desempata),
-- o que dispensa a ordenação. A V1 e a V2 já aplicadas não são editadas.
-- =====================================================================

-- RF09, operador (contexto fixo do ponto), filtro pontoId do administrador e RN05 (solicitações abertas do ponto).
CREATE INDEX idx_sol_poc_criacao
    ON coleta.sol_solicitacao (poc_nr_id, sol_dt_criacao DESC, sol_nr_id DESC);

-- RF09, morador (contexto fixo do morador).
CREATE INDEX idx_sol_mor_criacao
    ON coleta.sol_solicitacao (mor_nr_id, sol_dt_criacao DESC, sol_nr_id DESC);

-- RF09 do administrador sem filtro e por período; RF15 (solicitações por situação com período).
CREATE INDEX idx_sol_criacao
    ON coleta.sol_solicitacao (sol_dt_criacao DESC, sol_nr_id DESC);

-- RF09 do administrador filtrando por situação.
CREATE INDEX idx_sol_situacao_criacao
    ON coleta.sol_solicitacao (sol_tx_situacao, sol_dt_criacao DESC, sol_nr_id DESC);

-- Detalhe da solicitação (RF09): histórico por solicitação, ordenado por his_nr_id (HistoricoRepository).
CREATE INDEX idx_his_sol
    ON coleta.his_historico (sol_nr_id);

-- RF06: pesquisa de pontos por município, igualdade sem diferenciar maiúsculas (lower(...) = ...).
CREATE INDEX idx_poc_municipio_lower
    ON coleta.poc_ponto_coleta (lower(poc_tx_municipio));

-- RF06: pesquisa de pontos por bairro, igualdade sem diferenciar maiúsculas (lower(...) = ...).
CREATE INDEX idx_poc_bairro_lower
    ON coleta.poc_ponto_coleta (lower(poc_tx_bairro));
