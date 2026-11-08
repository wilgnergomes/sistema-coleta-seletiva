-- Dataset de desempenho do RNF04 (Issue #58): 1.000 solicitações com dados de apoio coerentes.
--
-- NÃO faz parte do seed de demonstração e nunca é executado automaticamente. É carregado sob demanda, em um banco
-- dedicado e vazio (veja docs/desempenho/README.md), com:
--   docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho -v ON_ERROR_STOP=1 < docs/desempenho/carga-dataset.sql
--
-- Tudo é DETERMINÍSTICO (sem random): cada carga produz exatamente os mesmos dados, o que permite comparar a medição
-- antes e depois dos índices. Tudo leva o marcador PERF, usado pela limpeza e pela guarda contra carga repetida:
--   moradores   e-mail 'perf.morador.NNN@exemplo.com'      (200, com um endereço cada)
--   pontos      nome 'PERF Ponto NN'                       (20, ativos, com recolhimento domiciliar)
--   materiais   nome 'PERF ...'                            (8: KG, L e UN)
--   operadores  e-mail 'perf.operador.NN@exemplo.com'      (1 por ponto)
--   vínculos    4 a 6 materiais por ponto                  (101 no total)
--   solicitações 1.000, com 2 a 4 itens cada (3.000 itens) e histórico coerente (2.850 linhas)
--
-- Regras respeitadas (RN01, RN03, ADR 0008, 0010 e 0011): ponto ativo e domiciliar; materiais aceitos pelo ponto;
-- limites do vínculo; unidade e endereço copiados; histórico completo até a situação atual, com exatamente um responsável
-- por linha e operador do ponto; quantidade efetiva e data de conclusão só nas CONCLUIDA, com ao menos uma positiva.
--
-- Ponto, morador e situação são independentes entre si: cada ponto tem as 6 situações e cada morador distribui as 5
-- solicitações por 5 pontos e mais de uma situação (corrige o dataset original, em que cada ponto e cada morador tinham
-- uma única situação; veja resultados-iniciais.md).
--
-- Distribuição global por situação (a cada 20 solicitações): CONCLUIDA 9 (45%), SOLICITADA 3 (15%), AGENDADA 3 (15%),
-- RECUSADA 2 (10%), CANCELADA 2 (10%) e EM_RECOLHIMENTO 1 (5%). Criadas ao longo de 12 meses (nov/2025 a out/2026).
--
-- Transação única: se qualquer etapa falhar, nada é gravado. Para repetir a carga, execute antes limpar-dataset.sql.

BEGIN;

SET LOCAL search_path TO coleta;

-- 0. Guarda: recusa uma segunda carga
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM mor_morador WHERE mor_tx_email LIKE 'perf.morador.%')
       OR EXISTS (SELECT 1 FROM poc_ponto_coleta WHERE poc_tx_nome LIKE 'PERF %')
       OR EXISTS (SELECT 1 FROM mat_material WHERE mat_tx_nome LIKE 'PERF %')
       OR EXISTS (SELECT 1 FROM ope_operador WHERE ope_tx_email LIKE 'perf.operador.%') THEN
        RAISE EXCEPTION 'O dataset de desempenho já foi carregado. Execute limpar-dataset.sql antes de carregar de novo.';
    END IF;
END $$;

-- 1. Materiais (8)
INSERT INTO mat_material (mat_tx_nome, mat_tx_descricao, mat_tx_unidade, mat_tx_orientacao, mat_tx_situacao) VALUES
    ('PERF Papel',            'Dataset de desempenho', 'KG', 'Dataset de desempenho', 'ATIVO'),
    ('PERF Plástico',         'Dataset de desempenho', 'KG', 'Dataset de desempenho', 'ATIVO'),
    ('PERF Vidro',            'Dataset de desempenho', 'KG', 'Dataset de desempenho', 'ATIVO'),
    ('PERF Metal',            'Dataset de desempenho', 'KG', 'Dataset de desempenho', 'ATIVO'),
    ('PERF Óleo de cozinha',  'Dataset de desempenho', 'L',  'Dataset de desempenho', 'ATIVO'),
    ('PERF Pilhas',           'Dataset de desempenho', 'UN', 'Dataset de desempenho', 'ATIVO'),
    ('PERF Isopor',           'Dataset de desempenho', 'KG', 'Dataset de desempenho', 'ATIVO'),
    ('PERF Eletrônicos',      'Dataset de desempenho', 'UN', 'Dataset de desempenho', 'ATIVO');

-- 2. Moradores (200) e um endereço para cada um
INSERT INTO mor_morador (mor_tx_nome, mor_tx_email, mor_tx_fone, mor_tx_situacao)
SELECT 'PERF Morador ' || lpad(n::text, 3, '0'),
       'perf.morador.' || lpad(n::text, 3, '0') || '@exemplo.com',
       '79999' || lpad(n::text, 6, '0'),
       'ATIVO'
FROM generate_series(1, 200) AS n
ORDER BY n;

INSERT INTO end_endereco (mor_nr_id, end_tx_logradouro, end_tx_numero, end_tx_bairro, end_tx_municipio, end_tx_uf,
                          end_tx_cep, end_tx_complemento)
SELECT m.mor_nr_id,
       'Rua PERF ' || m.rn,
       ((m.rn % 300) + 1)::text,
       (ARRAY['Centro', 'Jardim', 'Industrial', 'Santa Maria', 'Siqueira Campos'])[((m.rn - 1) % 5) + 1],
       (ARRAY['Lagarto', 'Aracaju', 'Itabaiana', 'Estância', 'São Cristóvão', 'Nossa Senhora do Socorro',
              'Simão Dias', 'Tobias Barreto'])[((m.rn - 1) % 8) + 1],
       'SE',
       '494' || lpad(m.rn::text, 5, '0'),
       NULL
FROM (SELECT mor_nr_id, row_number() OVER (ORDER BY mor_nr_id) AS rn
      FROM mor_morador WHERE mor_tx_email LIKE 'perf.morador.%') AS m
ORDER BY m.rn;

-- 3. Pontos de coleta (20), todos ativos e com recolhimento domiciliar, em 8 municípios e 5 bairros
INSERT INTO poc_ponto_coleta (poc_tx_nome, poc_tx_contato, poc_tx_horarios, poc_tx_situacao, poc_bl_rec_domiciliar,
                              poc_tx_logradouro, poc_tx_numero, poc_tx_bairro, poc_tx_municipio, poc_tx_uf, poc_tx_cep,
                              poc_tx_complemento)
SELECT 'PERF Ponto ' || lpad(n::text, 2, '0'),
       '(79) 3333-' || lpad(n::text, 4, '0'),
       'Segunda a sexta, das 8h às 17h',
       'ATIVO',
       TRUE,
       'Avenida PERF ' || n,
       n::text,
       (ARRAY['Centro', 'Jardim', 'Industrial', 'Santa Maria', 'Siqueira Campos'])[((n - 1) % 5) + 1],
       (ARRAY['Lagarto', 'Aracaju', 'Itabaiana', 'Estância', 'São Cristóvão', 'Nossa Senhora do Socorro',
              'Simão Dias', 'Tobias Barreto'])[((n - 1) % 8) + 1],
       'SE',
       '495' || lpad(n::text, 5, '0'),
       NULL
FROM generate_series(1, 20) AS n
ORDER BY n;

-- Tabelas temporárias com a posição (rn) de cada registro do dataset; somem no fim da transação
CREATE TEMP TABLE perf_material ON COMMIT DROP AS
SELECT mat_nr_id, mat_tx_unidade, row_number() OVER (ORDER BY mat_nr_id) AS rn
FROM mat_material WHERE mat_tx_nome LIKE 'PERF %';

CREATE TEMP TABLE perf_ponto ON COMMIT DROP AS
SELECT poc_nr_id, row_number() OVER (ORDER BY poc_nr_id) AS rn
FROM poc_ponto_coleta WHERE poc_tx_nome LIKE 'PERF %';

CREATE TEMP TABLE perf_morador ON COMMIT DROP AS
SELECT mor_nr_id, row_number() OVER (ORDER BY mor_nr_id) AS rn
FROM mor_morador WHERE mor_tx_email LIKE 'perf.morador.%';

-- 4. Materiais aceitos: o ponto n aceita 4 + (n mod 3) materiais, com limite nos materiais 3 e 6
INSERT INTO mac_material_aceito (poc_nr_id, mat_nr_id, mac_tx_observacoes, mac_nr_limite)
SELECT p.poc_nr_id, m.mat_nr_id, NULL,
       CASE WHEN m.rn % 3 = 0 THEN (CASE WHEN m.mat_tx_unidade = 'UN' THEN 20.000 ELSE 50.000 END) END
FROM perf_ponto p
CROSS JOIN perf_material m
WHERE ((m.rn - 1 + p.rn) % 8) < (4 + (p.rn % 3))
ORDER BY p.rn, m.rn;

-- 5. Operadores (1 por ponto)
INSERT INTO ope_operador (ope_tx_nome, ope_tx_email, ope_tx_situacao, poc_nr_id)
SELECT 'PERF Operador ' || lpad(p.rn::text, 2, '0'),
       'perf.operador.' || lpad(p.rn::text, 2, '0') || '@exemplo.com',
       'ATIVO',
       p.poc_nr_id
FROM perf_ponto p
ORDER BY p.rn;

-- 6. Plano das 1.000 solicitações: situação, datas e protocolo de cada uma (i = 1 a 1000)
CREATE TEMP TABLE perf_base ON COMMIT DROP AS
SELECT x.i,
       ((x.i - 1) % 200) + 1 AS rn_mor,
       -- Ponto: com i - 1 = 200q + 20a + b (q de 0 a 4, a de 0 a 9, b de 0 a 19), o ponto é (b + a + q) mod 20.
       -- Cada ponto recebe 50 solicitações, e as 5 de um morador (q = 0 a 4) caem em 5 pontos diferentes
       ((((x.i - 1) % 20) + (((x.i - 1) % 200) / 20) + ((x.i - 1) / 200)) % 20) + 1 AS rn_poc,
       x.situacao,
       x.criada,
       x.criada + (1 + x.i % 5) * INTERVAL '1 day' + INTERVAL '1 hour' AS agendada,
       x.criada + (1 + x.i % 5) * INTERVAL '1 day' + INTERVAL '1 hour' + INTERVAL '5 minutes' AS iniciada,
       x.criada + (1 + x.i % 5) * INTERVAL '1 day' + INTERVAL '3 hours' + (x.i % 60) * INTERVAL '1 minute' AS concluida,
       'SOL-' || to_char(x.criada AT TIME ZONE 'America/Sao_Paulo', 'YYYYMMDD') || '-P' || lpad(x.i::text, 5, '0') AS protocolo
FROM (
    -- Situação: depende de j = (i + 7q) mod 20, que muda de uma volta (q) para outra. Assim a situação não
    -- acompanha o morador nem o ponto, e a proporção global continua exata (cada resíduo de j ocorre 50 vezes)
    SELECT i,
           CASE WHEN j <= 8 THEN 'CONCLUIDA'
                WHEN j <= 11 THEN 'SOLICITADA'
                WHEN j <= 14 THEN 'AGENDADA'
                WHEN j <= 16 THEN 'RECUSADA'
                WHEN j <= 18 THEN 'CANCELADA'
                ELSE 'EM_RECOLHIMENTO' END AS situacao,
           TIMESTAMPTZ '2025-11-01 08:00:00-03' + ((i * 53) % 365) * INTERVAL '1 day'
               + ((i * 17) % 600) * INTERVAL '1 minute' AS criada
    FROM (SELECT i, (i + 7 * ((i - 1) / 200)) % 20 AS j FROM generate_series(1, 1000) AS i) AS g
) AS x;

-- 7. Solicitações. A versão é o número de transições já feitas; a cópia do endereço segue o texto da aplicação (ADR 0008)
INSERT INTO sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao,
                             sol_dt_criacao, sol_dt_agendamento, sol_dt_conclusao, sol_tx_observacoes, sol_nr_versao)
SELECT b.protocolo,
       m.mor_nr_id,
       p.poc_nr_id,
       e.end_tx_logradouro || ', ' || e.end_tx_numero || ' - ' || e.end_tx_bairro || ' - ' || e.end_tx_municipio
           || '/' || e.end_tx_uf || ' - CEP ' || e.end_tx_cep,
       b.situacao,
       b.criada,
       CASE WHEN b.situacao IN ('AGENDADA', 'EM_RECOLHIMENTO', 'CONCLUIDA')
                 OR (b.situacao = 'CANCELADA' AND b.i % 2 = 1) THEN b.agendada END,
       CASE WHEN b.situacao = 'CONCLUIDA' THEN b.concluida END,
       CASE WHEN b.i % 4 = 0 THEN 'Material separado e pronto para retirada' END,
       CASE b.situacao
            WHEN 'SOLICITADA' THEN 0
            WHEN 'AGENDADA' THEN 1
            WHEN 'EM_RECOLHIMENTO' THEN 2
            WHEN 'CONCLUIDA' THEN 3
            WHEN 'RECUSADA' THEN 1
            ELSE CASE WHEN b.i % 2 = 0 THEN 1 ELSE 2 END
       END
FROM perf_base b
JOIN perf_morador m ON m.rn = b.rn_mor
JOIN perf_ponto p ON p.rn = b.rn_poc
JOIN end_endereco e ON e.mor_nr_id = m.mor_nr_id
ORDER BY b.i;

-- 8. Itens: de 2 a 4 materiais aceitos pelo ponto; a efetiva (só nas CONCLUIDA) difere da estimada e o item
--    não recolhido tem efetiva 0; sempre há ao menos uma quantidade positiva
WITH aceitos AS (
    SELECT s.sol_nr_id, b.i, b.situacao, a.mat_nr_id, m.mat_tx_unidade AS unidade,
           row_number() OVER (PARTITION BY s.sol_nr_id ORDER BY a.mat_nr_id) AS r,
           count(*) OVER (PARTITION BY s.sol_nr_id) AS cnt
    FROM perf_base b
    JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
    JOIN mac_material_aceito a ON a.poc_nr_id = s.poc_nr_id
    JOIN mat_material m ON m.mat_nr_id = a.mat_nr_id
), escolhidos AS (
    SELECT aceitos.*, row_number() OVER (PARTITION BY sol_nr_id ORDER BY mat_nr_id) AS ord
    FROM aceitos
    WHERE ((r - 1 + i) % cnt) < (2 + (i % 3))
), quantidades AS (
    SELECT escolhidos.*,
           CASE WHEN unidade = 'UN' THEN (1 + ((i + r) % 10))::numeric
                ELSE ((1 + ((i + r) % 20)) * 2.5)::numeric END AS estimada
    FROM escolhidos
)
INSERT INTO iso_item_solicitacao (sol_nr_id, mat_nr_id, iso_tx_copia_unidade, iso_nr_quantidade_estimada,
                                  iso_nr_quantidade_efetiva)
SELECT sol_nr_id, mat_nr_id, unidade, estimada,
       CASE WHEN situacao = 'CONCLUIDA' THEN
            CASE WHEN unidade = 'UN'
                 THEN GREATEST(CASE WHEN ord = 1 THEN 1 ELSE 0 END, floor(estimada * (0.7 + ((i + r) % 4) * 0.1)))
                 ELSE CASE WHEN ord > 1 AND ((i + r) % 11) = 0 THEN 0
                           ELSE round(estimada * (0.7 + ((i + r) % 4) * 0.1), 3) END
            END
       END
FROM quantidades
ORDER BY sol_nr_id, mat_nr_id;

-- 9. Histórico. Cada bloco acrescenta a linha de uma etapa, na ordem cronológica, então a última linha de cada
--    solicitação é a sua situação atual. Exatamente um responsável por linha (ADR 0011).
-- 9.1 Criação pelo morador dono (as 1.000)
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, NULL, 'SOLICITADA', b.criada, NULL, s.mor_nr_id, NULL, NULL
FROM perf_base b JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
ORDER BY b.i;

-- 9.2 Agendamento pelo operador do ponto (AGENDADA, EM_RECOLHIMENTO, CONCLUIDA e as CANCELADA que já estavam agendadas)
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, 'SOLICITADA', 'AGENDADA', b.criada + INTERVAL '1 hour', o.ope_nr_id, NULL, NULL, b.agendada
FROM perf_base b
JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
JOIN ope_operador o ON o.poc_nr_id = s.poc_nr_id
WHERE b.situacao IN ('AGENDADA', 'EM_RECOLHIMENTO', 'CONCLUIDA') OR (b.situacao = 'CANCELADA' AND b.i % 2 = 1)
ORDER BY b.i;

-- 9.3 Início do recolhimento
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, 'AGENDADA', 'EM_RECOLHIMENTO', b.iniciada, o.ope_nr_id, NULL, NULL, NULL
FROM perf_base b
JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
JOIN ope_operador o ON o.poc_nr_id = s.poc_nr_id
WHERE b.situacao IN ('EM_RECOLHIMENTO', 'CONCLUIDA')
ORDER BY b.i;

-- 9.4 Conclusão
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, 'EM_RECOLHIMENTO', 'CONCLUIDA', b.concluida, o.ope_nr_id, NULL, NULL, NULL
FROM perf_base b
JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
JOIN ope_operador o ON o.poc_nr_id = s.poc_nr_id
WHERE b.situacao = 'CONCLUIDA'
ORDER BY b.i;

-- 9.5 Recusa pelo operador, com justificativa
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, 'SOLICITADA', 'RECUSADA', b.criada + INTERVAL '3 hours', o.ope_nr_id, NULL,
       'Fora da área de atendimento', NULL
FROM perf_base b
JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
JOIN ope_operador o ON o.poc_nr_id = s.poc_nr_id
WHERE b.situacao = 'RECUSADA'
ORDER BY b.i;

-- 9.6 Cancelamento em SOLICITADA (i par): ora pelo morador, ora pelo operador
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, 'SOLICITADA', 'CANCELADA', b.criada + INTERVAL '5 hours',
       CASE WHEN b.i % 4 = 2 THEN o.ope_nr_id END,
       CASE WHEN b.i % 4 = 0 THEN s.mor_nr_id END,
       'Cancelado antes do agendamento', NULL
FROM perf_base b
JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
JOIN ope_operador o ON o.poc_nr_id = s.poc_nr_id
WHERE b.situacao = 'CANCELADA' AND b.i % 2 = 0
ORDER BY b.i;

-- 9.7 Cancelamento em AGENDADA (i ímpar), pelo morador
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data, ope_nr_id, mor_nr_id,
                           his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, 'AGENDADA', 'CANCELADA', b.criada + INTERVAL '1 day', NULL, s.mor_nr_id,
       'Não estarei em casa na data marcada', NULL
FROM perf_base b
JOIN sol_solicitacao s ON s.sol_tx_protocolo = b.protocolo
WHERE b.situacao = 'CANCELADA' AND b.i % 2 = 1
ORDER BY b.i;

-- 10. Autoconferência: se algo saiu diferente do esperado, a transação inteira é desfeita
DO $$
DECLARE
    total integer;
BEGIN
    SELECT count(*) INTO total
    FROM sol_solicitacao s JOIN mor_morador m ON m.mor_nr_id = s.mor_nr_id
    WHERE m.mor_tx_email LIKE 'perf.morador.%';
    IF total <> 1000 THEN
        RAISE EXCEPTION 'Carga incompleta: esperadas 1000 solicitações do dataset, encontradas %', total;
    END IF;
END $$;

COMMIT;
