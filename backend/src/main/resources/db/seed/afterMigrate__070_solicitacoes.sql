-- Dados de demonstração do RF09 (consulta de solicitações). Carregado só no perfil "demo".
--
-- Depende dos seeds 010 a 060 (moradores, endereços, materiais, pontos, materiais aceitos e operadores):
-- tudo é ligado pelos nomes e e-mails de lá. Idempotente: a solicitação é identificada pelo protocolo, e os
-- itens e o histórico só entram quando ainda não existem (INSERT ... SELECT ... WHERE NOT EXISTS).
--
-- Há uma solicitação em cada situação e datas de criação entre setembro e novembro de 2026, para exercitar
-- os filtros de situação, ponto e período. O histórico é coerente com a situação: cada transição tem um
-- responsável (o morador, ou um operador do ponto, ADR 0010 e 0011). Os protocolos seguem o formato real
-- (SOL-AAAAMMDD-XXXXXX) e a cópia do endereço segue o texto gerado pela aplicação (ADR 0008).
--
-- Só o ponto "Cooperativa Verde Vida" tem operadores no seed, por isso as solicitações que passam por
-- operador são dele. As do "Ponto Recicla Itabaiana" estão em SOLICITADA.

-- 1. Solicitações
INSERT INTO sol_solicitacao (sol_tx_protocolo, mor_nr_id, poc_nr_id, sol_tx_endereco_copia, sol_tx_situacao,
                             sol_dt_criacao, sol_dt_agendamento, sol_dt_conclusao, sol_tx_observacoes)
SELECT v.protocolo, m.mor_nr_id, p.poc_nr_id,
       e.end_tx_logradouro || ', ' || e.end_tx_numero
           || CASE WHEN e.end_tx_complemento IS NULL THEN '' ELSE ' (' || e.end_tx_complemento || ')' END
           || ' - ' || e.end_tx_bairro || ' - ' || e.end_tx_municipio || '/' || e.end_tx_uf
           || ' - CEP ' || e.end_tx_cep,
       v.situacao, v.criada::timestamptz, v.agendada::timestamptz, v.concluida::timestamptz, v.observacoes
FROM (VALUES
    ('SOL-20260928-SEEDD5', 'joao.pereira@exemplo.com',  'Rua do Comércio',   'Cooperativa Verde Vida',    'CONCLUIDA',
     '2026-09-28 09:00:00-03', '2026-10-02 09:00:00-03', '2026-10-02 11:30:00-03', 'Caixas e garrafas no portão'),
    ('SOL-20261005-SEEDA2', 'maria.silva@exemplo.com',   'Rua das Flores',    'Cooperativa Verde Vida',    'SOLICITADA',
     '2026-10-05 10:00:00-03', NULL,                     NULL,                     'Caixas na calçada'),
    ('SOL-20261012-SEEDB3', 'maria.silva@exemplo.com',   'Rua das Flores',    'Cooperativa Verde Vida',    'AGENDADA',
     '2026-10-12 08:00:00-03', '2026-11-20 09:00:00-03', NULL,                     NULL),
    ('SOL-20261015-SEEDC4', 'joao.pereira@exemplo.com',  'Rua do Comércio',   'Cooperativa Verde Vida',    'EM_RECOLHIMENTO',
     '2026-10-15 16:00:00-03', '2026-10-20 09:00:00-03', NULL,                     'Chamar no portão lateral'),
    ('SOL-20261018-SEEDE6', 'ana.souza@exemplo.com',     'Travessa São José', 'Cooperativa Verde Vida',    'RECUSADA',
     '2026-10-18 11:00:00-03', NULL,                     NULL,                     NULL),
    ('SOL-20261022-SEEDF7', 'carlos.lima@exemplo.com',   'Rua da Paz',        'Cooperativa Verde Vida',    'CANCELADA',
     '2026-10-22 14:00:00-03', NULL,                     NULL,                     NULL),
    ('SOL-20261103-SEEDG8', 'carlos.lima@exemplo.com',   'Rua Nova',          'Ponto Recicla Itabaiana',   'SOLICITADA',
     '2026-11-03 09:30:00-03', NULL,                     NULL,                     'Material separado na garagem'),
    ('SOL-20261105-SEEDH9', 'maria.silva@exemplo.com',   'Avenida Principal', 'Ponto Recicla Itabaiana',   'SOLICITADA',
     '2026-11-05 18:45:00-03', NULL,                     NULL,                     NULL)
) AS v(protocolo, email, logradouro, ponto, situacao, criada, agendada, concluida, observacoes)
JOIN mor_morador m ON m.mor_tx_email = v.email
JOIN end_endereco e ON e.mor_nr_id = m.mor_nr_id AND e.end_tx_logradouro = v.logradouro
JOIN poc_ponto_coleta p ON LOWER(BTRIM(p.poc_tx_nome)) = LOWER(v.ponto)
WHERE NOT EXISTS (
    SELECT 1 FROM sol_solicitacao s WHERE s.sol_tx_protocolo = v.protocolo
);

-- 2. Itens (a unidade é copiada do material, ADR 0008; só a solicitação concluída tem quantidade efetiva)
INSERT INTO iso_item_solicitacao (sol_nr_id, mat_nr_id, iso_tx_copia_unidade,
                                  iso_nr_quantidade_estimada, iso_nr_quantidade_efetiva)
SELECT s.sol_nr_id, m.mat_nr_id, m.mat_tx_unidade, v.estimada, v.efetiva
FROM (VALUES
    ('SOL-20260928-SEEDD5', 'Papel',          18.000, 17.500),
    ('SOL-20260928-SEEDD5', 'Vidro',          10.000, 10.000),
    ('SOL-20261005-SEEDA2', 'Papel',          12.500, NULL),
    ('SOL-20261005-SEEDA2', 'Plástico',        4.000, NULL),
    ('SOL-20261012-SEEDB3', 'Vidro',          20.000, NULL),
    ('SOL-20261012-SEEDB3', 'Metal',           3.200, NULL),
    ('SOL-20261015-SEEDC4', 'Papel',          30.000, NULL),
    ('SOL-20261015-SEEDC4', 'Vidro',          15.000, NULL),
    ('SOL-20261018-SEEDE6', 'Óleo de cozinha', 5.000, NULL),
    ('SOL-20261022-SEEDF7', 'Papel',           8.000, NULL),
    ('SOL-20261103-SEEDG8', 'Vidro',           6.000, NULL),
    ('SOL-20261103-SEEDG8', 'Papel',           9.000, NULL),
    ('SOL-20261105-SEEDH9', 'Papel',          14.000, NULL)
) AS v(protocolo, material, estimada, efetiva)
JOIN sol_solicitacao s ON s.sol_tx_protocolo = v.protocolo
JOIN mat_material m ON LOWER(BTRIM(m.mat_tx_nome)) = LOWER(v.material)
WHERE NOT EXISTS (
    SELECT 1 FROM iso_item_solicitacao i WHERE i.sol_nr_id = s.sol_nr_id AND i.mat_nr_id = m.mat_nr_id
);

-- 3. Histórico: cada linha tem exatamente um responsável (operador OU morador, ck_his_responsavel).
--    Só entra para a solicitação que ainda não tem nenhum registro.
INSERT INTO his_historico (sol_nr_id, his_tx_situacao_anterior, his_tx_situacao_nova, his_dt_data,
                           ope_nr_id, mor_nr_id, his_tx_motivo, his_dt_agendamento)
SELECT s.sol_nr_id, v.anterior, v.nova, v.data::timestamptz, o.ope_nr_id, m.mor_nr_id, v.motivo,
       v.agendamento::timestamptz
FROM (VALUES
    ('SOL-20260928-SEEDD5', NULL,         'SOLICITADA',      '2026-09-28 09:00:00-03', NULL,                      'joao.pereira@exemplo.com', NULL, NULL),
    ('SOL-20260928-SEEDD5', 'SOLICITADA', 'AGENDADA',        '2026-09-29 14:00:00-03', 'carla.nunes@exemplo.com',  NULL, NULL, '2026-10-02 09:00:00-03'),
    ('SOL-20260928-SEEDD5', 'AGENDADA',   'EM_RECOLHIMENTO', '2026-10-02 09:10:00-03', 'paulo.santos@exemplo.com', NULL, NULL, NULL),
    ('SOL-20260928-SEEDD5', 'EM_RECOLHIMENTO', 'CONCLUIDA',  '2026-10-02 11:30:00-03', 'paulo.santos@exemplo.com', NULL, NULL, NULL),
    ('SOL-20261005-SEEDA2', NULL,         'SOLICITADA',      '2026-10-05 10:00:00-03', NULL,                      'maria.silva@exemplo.com', NULL, NULL),
    ('SOL-20261012-SEEDB3', NULL,         'SOLICITADA',      '2026-10-12 08:00:00-03', NULL,                      'maria.silva@exemplo.com', NULL, NULL),
    ('SOL-20261012-SEEDB3', 'SOLICITADA', 'AGENDADA',        '2026-10-13 14:00:00-03', 'carla.nunes@exemplo.com',  NULL, NULL, '2026-11-20 09:00:00-03'),
    ('SOL-20261015-SEEDC4', NULL,         'SOLICITADA',      '2026-10-15 16:00:00-03', NULL,                      'joao.pereira@exemplo.com', NULL, NULL),
    ('SOL-20261015-SEEDC4', 'SOLICITADA', 'AGENDADA',        '2026-10-16 10:00:00-03', 'carla.nunes@exemplo.com',  NULL, NULL, '2026-10-20 09:00:00-03'),
    ('SOL-20261015-SEEDC4', 'AGENDADA',   'EM_RECOLHIMENTO', '2026-10-20 09:05:00-03', 'carla.nunes@exemplo.com',  NULL, NULL, NULL),
    ('SOL-20261018-SEEDE6', NULL,         'SOLICITADA',      '2026-10-18 11:00:00-03', NULL,                      'ana.souza@exemplo.com', NULL, NULL),
    ('SOL-20261018-SEEDE6', 'SOLICITADA', 'RECUSADA',        '2026-10-19 09:00:00-03', 'carla.nunes@exemplo.com',  NULL,
     'Óleo fora da capacidade de recolhimento neste período', NULL),
    ('SOL-20261022-SEEDF7', NULL,         'SOLICITADA',      '2026-10-22 14:00:00-03', NULL,                      'carlos.lima@exemplo.com', NULL, NULL),
    ('SOL-20261022-SEEDF7', 'SOLICITADA', 'CANCELADA',       '2026-10-23 08:15:00-03', NULL,                      'carlos.lima@exemplo.com',
     'Mudança de planos', NULL),
    ('SOL-20261103-SEEDG8', NULL,         'SOLICITADA',      '2026-11-03 09:30:00-03', NULL,                      'carlos.lima@exemplo.com', NULL, NULL),
    ('SOL-20261105-SEEDH9', NULL,         'SOLICITADA',      '2026-11-05 18:45:00-03', NULL,                      'maria.silva@exemplo.com', NULL, NULL)
) AS v(protocolo, anterior, nova, data, email_operador, email_morador, motivo, agendamento)
JOIN sol_solicitacao s ON s.sol_tx_protocolo = v.protocolo
LEFT JOIN ope_operador o ON o.ope_tx_email = v.email_operador
LEFT JOIN mor_morador m ON m.mor_tx_email = v.email_morador
WHERE NOT EXISTS (
    SELECT 1 FROM his_historico h WHERE h.sol_nr_id = s.sol_nr_id
);
