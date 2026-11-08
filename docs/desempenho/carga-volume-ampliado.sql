-- Volume ampliado, ISOLADO, só para avaliar planos de execução e justificar índices (Issue #33).
--
-- NÃO faz parte da medição oficial do RNF04, que usa exatamente 1.000 solicitações (carga-dataset.sql). Os resultados
-- deste volume nunca se misturam com os da medição oficial. Rode em um banco DESCARTÁVEL (por exemplo,
-- coleta_desempenho_xl), depois da carga-dataset.sql:
--   docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho_xl -v ON_ERROR_STOP=1 < docs/desempenho/carga-volume-ampliado.sql
--
-- Reproduz as 1.000 solicitações do dataset 99 vezes (total 100.000), com itens e histórico coerentes: cada cópia
-- mantém morador, ponto, situação, itens e a sequência de histórico da original, com todas as datas deslocadas pelo
-- mesmo número de minutos (k), o que preserva a ordem cronológica e as restrições de datas. O protocolo recebe o sufixo
-- -Xkkk. Determinístico (sem random). As proporções por situação, ponto e morador são as do dataset (cada ponto passa a
-- ter 5.000 solicitações e cada morador, 500). A limpeza padrão (limpar-dataset.sql) também remove as cópias.
--
-- Transação única; recusa uma segunda execução.

BEGIN;

SET LOCAL search_path TO coleta;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM sol_solicitacao WHERE sol_tx_protocolo LIKE '%-X%') THEN
        RAISE EXCEPTION 'O volume ampliado já foi carregado neste banco.';
    END IF;
    IF (SELECT count(*) FROM sol_solicitacao) <> 1000 THEN
        RAISE EXCEPTION 'Carregue antes o dataset de 1.000 solicitações (carga-dataset.sql) em um banco vazio.';
    END IF;
END $$;

INSERT INTO sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao,
                             sol_dt_criacao, sol_dt_agendamento, sol_dt_conclusao, sol_tx_observacoes, sol_nr_versao)
SELECT o.sol_tx_protocolo || '-X' || lpad(k::text, 3, '0'),
       o.mor_nr_id, o.poc_nr_id, o.sol_tx_endereco_copia, o.sol_tx_situacao,
       o.sol_dt_criacao + k * INTERVAL '1 minute',
       o.sol_dt_agendamento + k * INTERVAL '1 minute',
       o.sol_dt_conclusao + k * INTERVAL '1 minute',
       o.sol_tx_observacoes, o.sol_nr_versao
FROM sol_solicitacao o
CROSS JOIN generate_series(1, 99) AS k
ORDER BY k, o.sol_nr_id;

CREATE TEMP TABLE perf_copia ON COMMIT DROP AS
SELECT o.sol_nr_id AS id_original, n.sol_nr_id AS id_copia, k
FROM generate_series(1, 99) AS k
JOIN sol_solicitacao o ON o.sol_tx_protocolo NOT LIKE '%-X%'
JOIN sol_solicitacao n ON n.sol_tx_protocolo = o.sol_tx_protocolo || '-X' || lpad(k::text, 3, '0');

INSERT INTO iso_item_solicitacao (sol_nr_id, mat_nr_id, iso_tx_copia_unidade, iso_nr_quantidade_estimada,
                                  iso_nr_quantidade_efetiva)
SELECT c.id_copia, i.mat_nr_id, i.iso_tx_copia_unidade, i.iso_nr_quantidade_estimada, i.iso_nr_quantidade_efetiva
FROM iso_item_solicitacao i
JOIN perf_copia c ON c.id_original = i.sol_nr_id
ORDER BY c.id_copia, i.mat_nr_id;

INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento, his_tx_informacoes_tentativa)
SELECT c.id_copia, h.his_tx_situacao_anterior, h.his_tx_situacao_nova,
       h.his_dt_data + c.k * INTERVAL '1 minute', h.ope_nr_id, h.mor_nr_id, h.his_tx_motivo,
       h.his_dt_agendamento + c.k * INTERVAL '1 minute', h.his_tx_informacoes_tentativa
FROM his_historico h
JOIN perf_copia c ON c.id_original = h.sol_nr_id
ORDER BY c.id_copia, h.his_nr_id;

DO $$
BEGIN
    IF (SELECT count(*) FROM sol_solicitacao) <> 100000 THEN
        RAISE EXCEPTION 'Volume incompleto: esperadas 100000 solicitações.';
    END IF;
END $$;

COMMIT;

ANALYZE;
