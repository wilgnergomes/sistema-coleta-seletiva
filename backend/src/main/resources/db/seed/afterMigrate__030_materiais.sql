-- Dados de demonstração do RF03 (materiais). Carregado só no perfil "demo".
--
-- Idempotente: só insere o material cujo nome (sem diferenciar maiúsculas) ainda não existe, o mesmo
-- critério do índice único uq_mat_nome. Usa INSERT ... SELECT ... WHERE NOT EXISTS para não consumir
-- a identidade a cada reexecução. Dados fictícios: quatro materiais em KG, um em L, um em UN e um inativo.
INSERT INTO mat_material (mat_tx_nome, mat_tx_descricao, mat_tx_unidade, mat_tx_orientacao, mat_tx_situacao)
SELECT v.nome, v.descricao, v.unidade, v.orientacao, v.situacao
FROM (VALUES
    ('Papel',             'Papel e papelão limpos e secos',              'KG', 'Separar do lixo úmido e desmontar as caixas',        'ATIVO'),
    ('Plástico',          'Garrafas, potes e embalagens plásticas',      'KG', 'Enxaguar e retirar os rótulos, se possível',         'ATIVO'),
    ('Vidro',             'Garrafas e potes de vidro',                   'KG', 'Lavar e remover as tampas',                          'ATIVO'),
    ('Metal',             'Latas de alumínio e de aço',                  'KG', 'Enxaguar e amassar para ocupar menos espaço',        'ATIVO'),
    ('Óleo de cozinha',   'Óleo vegetal usado',                          'L',  'Armazenar em garrafa PET bem fechada',               'ATIVO'),
    ('Pilhas e baterias', 'Pilhas e baterias portáteis',                 'UN', 'Entregar sem abrir ou perfurar',                     'ATIVO'),
    ('Isopor',            'Embalagens de poliestireno expandido',        'KG', 'Material temporariamente sem coleta',                'INATIVO')
) AS v(nome, descricao, unidade, orientacao, situacao)
WHERE NOT EXISTS (
    SELECT 1 FROM mat_material m WHERE LOWER(BTRIM(m.mat_tx_nome)) = LOWER(v.nome)
);
