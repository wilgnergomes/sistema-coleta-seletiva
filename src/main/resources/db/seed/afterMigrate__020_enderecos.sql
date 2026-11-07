-- Dados de demonstração do RF02 (endereços dos moradores do seed 010). Carregado só no perfil "demo".
--
-- Idempotente: só insere o endereço que ainda não existe para aquele morador (mesmo logradouro,
-- número e CEP). Usa INSERT ... SELECT ... WHERE NOT EXISTS para não consumir a identidade a cada
-- reexecução. Roda depois do 010, que cria os moradores. Dados fictícios, no formato do banco:
-- UF com duas maiúsculas, CEP com oito dígitos, número informado ou "S/N" explícito.
INSERT INTO end_endereco (mor_nr_id, end_tx_logradouro, end_tx_numero, end_tx_bairro,
                          end_tx_municipio, end_tx_uf, end_tx_cep, end_tx_complemento)
SELECT m.mor_nr_id, v.logradouro, v.numero, v.bairro, v.municipio, v.uf, v.cep, v.complemento
FROM (VALUES
    ('maria.silva@exemplo.com',   'Rua das Flores',          '120', 'Centro',        'Lagarto',   'SE', '49400000', 'Casa dos fundos'),
    ('maria.silva@exemplo.com',   'Avenida Principal',       'S/N', 'Zona Rural',    'Lagarto',   'SE', '49400001', NULL),
    ('joao.pereira@exemplo.com',  'Rua do Comércio',         '45',  'Centro',        'Itabaiana', 'SE', '49500000', NULL),
    ('ana.souza@exemplo.com',     'Travessa São José',       '8',   'São José',      'Aracaju',   'SE', '49015000', 'Apto 201'),
    ('carlos.lima@exemplo.com',   'Rua da Paz',              '300', 'Siqueira Campos','Aracaju',  'SE', '49075000', NULL),
    ('carlos.lima@exemplo.com',   'Rua Nova',                'S/N', 'Santa Maria',   'Aracaju',   'SE', '49044000', 'Próximo à escola'),
    ('beatriz.rocha@exemplo.com', 'Rua Boa Esperança',       '77',  'Bairro Industrial','Aracaju','SE', '49040000', NULL)
) AS v(email, logradouro, numero, bairro, municipio, uf, cep, complemento)
JOIN mor_morador m ON m.mor_tx_email = v.email
WHERE NOT EXISTS (
    SELECT 1 FROM end_endereco e
    WHERE e.mor_nr_id = m.mor_nr_id
      AND e.end_tx_logradouro = v.logradouro
      AND e.end_tx_numero = v.numero
      AND e.end_tx_cep = v.cep
);
