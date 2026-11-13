-- Dados de demonstração do RF05 (materiais aceitos pelos pontos). Carregado só no perfil "demo".
--
-- Depende dos seeds 030 (materiais) e 040 (pontos): os vínculos são feitos pelos nomes. Idempotente:
-- só insere o vínculo que ainda não existe (a chave primária é ponto + material). Há vínculos com e
-- sem limite e com e sem observação. O "Ecoponto Centro" e o "Ponto Comunitário Sem Contato" NÃO
-- aceitam vidro (cenário de material incompatível do RF08).
INSERT INTO mac_material_aceito (poc_nr_id, mat_nr_id, mac_tx_observacoes, mac_nr_limite)
SELECT p.poc_nr_id, m.mat_nr_id, v.observacoes, v.limite
FROM (VALUES
    ('Cooperativa Verde Vida',         'Papel',             'Aceita caixas desmontadas',          100.000),
    ('Cooperativa Verde Vida',         'Plástico',          NULL,                                  NULL),
    ('Cooperativa Verde Vida',         'Vidro',             'Apenas embalagens limpas',            50.500),
    ('Cooperativa Verde Vida',         'Metal',             NULL,                                  NULL),
    ('Cooperativa Verde Vida',         'Óleo de cozinha',   'Em garrafa PET bem fechada',          20.000),
    ('Ecoponto Centro',                'Papel',             NULL,                                  NULL),
    ('Ecoponto Centro',                'Plástico',          'Sem sacolas',                         30.000),
    ('Ecoponto Centro',                'Metal',             NULL,                                  NULL),
    ('Ecoponto Centro',                'Pilhas e baterias', 'Até 10 unidades por entrega',         10.000),
    ('Ponto Recicla Itabaiana',        'Papel',             NULL,                                  NULL),
    ('Ponto Recicla Itabaiana',        'Vidro',             NULL,                                  80.000),
    ('Ponto Comunitário Sem Contato',  'Plástico',          NULL,                                  NULL),
    ('Ecoponto Desativado',            'Papel',             NULL,                                  NULL)
) AS v(ponto, material, observacoes, limite)
JOIN poc_ponto_coleta p ON LOWER(BTRIM(p.poc_tx_nome)) = LOWER(v.ponto)
JOIN mat_material m ON LOWER(BTRIM(m.mat_tx_nome)) = LOWER(v.material)
WHERE NOT EXISTS (
    SELECT 1 FROM mac_material_aceito a WHERE a.poc_nr_id = p.poc_nr_id AND a.mat_nr_id = m.mat_nr_id
);
