-- Dados de demonstração do RF01 (moradores). Carregado somente no perfil "demo".
--
-- Idempotente: roda a cada início da aplicação e só insere o morador cujo e-mail ainda
-- não existe. Usa INSERT ... SELECT ... WHERE NOT EXISTS (e não ON CONFLICT DO NOTHING)
-- para não consumir valores da identidade a cada reexecução.
--
-- Todos os dados são fictícios e os e-mails já estão normalizados (minúsculas, sem espaços),
-- como exige o CHECK ck_mor_email_normalizado.
INSERT INTO mor_morador (mor_tx_nome, mor_tx_email, mor_tx_fone, mor_tx_situacao)
SELECT v.nome, v.email, v.fone, v.situacao
FROM (VALUES
    ('Maria Silva',    'maria.silva@exemplo.com',    '79999990001', 'ATIVO'),
    ('João Pereira',   'joao.pereira@exemplo.com',   '79999990002', 'ATIVO'),
    ('Ana Souza',      'ana.souza@exemplo.com',      NULL,          'ATIVO'),
    ('Carlos Lima',    'carlos.lima@exemplo.com',    '79999990004', 'ATIVO'),
    ('Beatriz Rocha',  'beatriz.rocha@exemplo.com',  '79999990005', 'INATIVO')
) AS v(nome, email, fone, situacao)
WHERE NOT EXISTS (
    SELECT 1 FROM mor_morador m WHERE m.mor_tx_email = v.email
);
