-- Dados de demonstração do RF15 (resumo gerencial). Carregado só no perfil "demo".
--
-- Depende dos seeds 010 a 070. Os seeds anteriores ficam intactos: este acrescenta o que o resumo precisa para ser
-- demonstrado com unidades diferentes. Idempotente: o vínculo, as solicitações (pelo protocolo), os itens e o histórico
-- só entram quando ainda não existem (INSERT ... SELECT ... WHERE NOT EXISTS).
--
-- 1. Nenhum ponto com recolhimento domiciliar aceitava um material em unidade de peça (UN): "Pilhas e baterias" só era
--    aceito pelo "Ecoponto Centro", que não recolhe em domicílio. O vínculo abaixo permite o cenário "kg com unidades"
--    do enunciado sem quebrar a RN01.
-- 2. Três solicitações CONCLUIDA, concluídas em setembro, outubro e novembro de 2026, com a quantidade efetiva diferente
--    da estimada e, em uma, um item não recolhido (efetiva 0). Histórico coerente: quem opera pertence ao ponto (ADR 0010).
--
-- Com o seed 070, o resumo gerencial do perfil "demo" soma (apenas a quantidade efetiva das CONCLUIDA):
--   Papel 76,000 KG (17,5 + 18,5 + 40) · Vidro 10,000 KG (10 + 0) · Plástico 7,250 KG · Metal 5,000 KG ·
--   Pilhas e baterias 16,000 UN (10 + 6).

-- 1. Vínculo que permite um material em UN em um ponto com recolhimento domiciliar
INSERT INTO mac_material_aceito (poc_nr_id, mat_nr_id, mac_tx_observacoes, mac_nr_limite)
SELECT p.poc_nr_id, m.mat_nr_id, 'Até 20 unidades por solicitação', 20.000
FROM poc_ponto_coleta p
JOIN mat_material m ON LOWER(BTRIM(m.mat_tx_nome)) = LOWER('Pilhas e baterias')
WHERE LOWER(BTRIM(p.poc_tx_nome)) = LOWER('Cooperativa Verde Vida')
  AND NOT EXISTS (
    SELECT 1 FROM mac_material_aceito a WHERE a.poc_nr_id = p.poc_nr_id AND a.mat_nr_id = m.mat_nr_id
);

-- 2. Solicitações concluídas
INSERT INTO sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao,
                             sol_dt_criacao, sol_dt_agendamento, sol_dt_conclusao, sol_tx_observacoes)
SELECT v.protocolo, m.mor_nr_id, p.poc_nr_id,
       e.end_tx_logradouro || ', ' || e.end_tx_numero
           || CASE WHEN e.end_tx_complemento IS NULL THEN '' ELSE ' (' || e.end_tx_complemento || ')' END
           || ' - ' || e.end_tx_bairro || ' - ' || e.end_tx_municipio || '/' || e.end_tx_uf
           || ' - CEP ' || e.end_tx_cep,
       'CONCLUIDA', v.criada::timestamptz, v.agendada::timestamptz, v.concluida::timestamptz, v.observacoes
FROM (VALUES
    ('SOL-20260915-SEEDJ2', 'ana.souza@exemplo.com',   'Travessa São José', 'Cooperativa Verde Vida',
     '2026-09-15 10:00:00-03', '2026-09-17 09:00:00-03', '2026-09-17 11:00:00-03', 'Papel e pilhas na portaria'),
    ('SOL-20261008-SEEDK3', 'carlos.lima@exemplo.com', 'Rua da Paz',        'Cooperativa Verde Vida',
     '2026-10-08 08:30:00-03', '2026-10-10 09:00:00-03', '2026-10-10 12:15:00-03', NULL),
    ('SOL-20261030-SEEDM5', 'maria.silva@exemplo.com', 'Rua das Flores',    'Cooperativa Verde Vida',
     '2026-10-30 17:20:00-03', '2026-11-03 09:00:00-03', '2026-11-03 10:40:00-03', 'Plástico e metal separados')
) AS v(protocolo, email, logradouro, ponto, criada, agendada, concluida, observacoes)
JOIN mor_morador m ON m.mor_tx_email = v.email
JOIN end_endereco e ON e.mor_nr_id = m.mor_nr_id AND e.end_tx_logradouro = v.logradouro
JOIN poc_ponto_coleta p ON LOWER(BTRIM(p.poc_tx_nome)) = LOWER(v.ponto)
WHERE NOT EXISTS (
    SELECT 1 FROM sol_solicitacao s WHERE s.sol_tx_protocolo = v.protocolo
);

-- 3. Itens: a quantidade efetiva difere da estimada; a unidade é copiada do material (ADR 0008)
INSERT INTO iso_item_solicitacao (sol_nr_id, mat_nr_id, iso_tx_copia_unidade,
                                  iso_nr_quantidade_estimada, iso_nr_quantidade_efetiva)
SELECT s.sol_nr_id, m.mat_nr_id, m.mat_tx_unidade, v.estimada, v.efetiva
FROM (VALUES
    ('SOL-20260915-SEEDJ2', 'Papel',             20.000, 18.500),
    ('SOL-20260915-SEEDJ2', 'Pilhas e baterias', 12.000, 10.000),
    ('SOL-20261008-SEEDK3', 'Papel',             40.000, 40.000),
    ('SOL-20261008-SEEDK3', 'Vidro',             10.000,  0.000),
    ('SOL-20261008-SEEDK3', 'Pilhas e baterias',  6.000,  6.000),
    ('SOL-20261030-SEEDM5', 'Plástico',           8.000,  7.250),
    ('SOL-20261030-SEEDM5', 'Metal',              5.000,  5.000)
) AS v(protocolo, material, estimada, efetiva)
JOIN sol_solicitacao s ON s.sol_tx_protocolo = v.protocolo
JOIN mat_material m ON LOWER(BTRIM(m.mat_tx_nome)) = LOWER(v.material)
WHERE NOT EXISTS (
    SELECT 1 FROM iso_item_solicitacao i WHERE i.sol_nr_id = s.sol_nr_id AND i.mat_nr_id = m.mat_nr_id
);

-- 4. Histórico: exatamente um responsável por linha (ck_his_responsavel); só entra para a solicitação sem histórico
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data,
                           ope_nr_id, mor_nr_id, his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, v.anterior, v.nova, v.data::timestamptz, o.ope_nr_id, m.mor_nr_id, NULL,
       v.agendamento::timestamptz
FROM (VALUES
    ('SOL-20260915-SEEDJ2', NULL,              'SOLICITADA',      '2026-09-15 10:00:00-03', NULL,                      'ana.souza@exemplo.com', NULL),
    ('SOL-20260915-SEEDJ2', 'SOLICITADA',      'AGENDADA',        '2026-09-16 14:00:00-03', 'carla.nunes@exemplo.com',  NULL, '2026-09-17 09:00:00-03'),
    ('SOL-20260915-SEEDJ2', 'AGENDADA',        'EM_RECOLHIMENTO', '2026-09-17 09:10:00-03', 'paulo.santos@exemplo.com', NULL, NULL),
    ('SOL-20260915-SEEDJ2', 'EM_RECOLHIMENTO', 'CONCLUIDA',       '2026-09-17 11:00:00-03', 'paulo.santos@exemplo.com', NULL, NULL),
    ('SOL-20261008-SEEDK3', NULL,              'SOLICITADA',      '2026-10-08 08:30:00-03', NULL,                      'carlos.lima@exemplo.com', NULL),
    ('SOL-20261008-SEEDK3', 'SOLICITADA',      'AGENDADA',        '2026-10-09 10:00:00-03', 'carla.nunes@exemplo.com',  NULL, '2026-10-10 09:00:00-03'),
    ('SOL-20261008-SEEDK3', 'AGENDADA',        'EM_RECOLHIMENTO', '2026-10-10 09:05:00-03', 'carla.nunes@exemplo.com',  NULL, NULL),
    ('SOL-20261008-SEEDK3', 'EM_RECOLHIMENTO', 'CONCLUIDA',       '2026-10-10 12:15:00-03', 'carla.nunes@exemplo.com',  NULL, NULL),
    ('SOL-20261030-SEEDM5', NULL,              'SOLICITADA',      '2026-10-30 17:20:00-03', NULL,                      'maria.silva@exemplo.com', NULL),
    ('SOL-20261030-SEEDM5', 'SOLICITADA',      'AGENDADA',        '2026-10-31 09:00:00-03', 'paulo.santos@exemplo.com', NULL, '2026-11-03 09:00:00-03'),
    ('SOL-20261030-SEEDM5', 'AGENDADA',        'EM_RECOLHIMENTO', '2026-11-03 09:05:00-03', 'paulo.santos@exemplo.com', NULL, NULL),
    ('SOL-20261030-SEEDM5', 'EM_RECOLHIMENTO', 'CONCLUIDA',       '2026-11-03 10:40:00-03', 'paulo.santos@exemplo.com', NULL, NULL)
) AS v(protocolo, anterior, nova, data, email_operador, email_morador, agendamento)
JOIN sol_solicitacao s ON s.sol_tx_protocolo = v.protocolo
LEFT JOIN ope_operador o ON o.ope_tx_email = v.email_operador
LEFT JOIN mor_morador m ON m.mor_tx_email = v.email_morador
WHERE NOT EXISTS (
    SELECT 1 FROM his_historico h WHERE h.sol_nr_id = s.sol_nr_id
);
