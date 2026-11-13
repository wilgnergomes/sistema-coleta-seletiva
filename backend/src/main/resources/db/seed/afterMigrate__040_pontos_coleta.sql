-- Dados de demonstração do RF04 (pontos de coleta). Carregado só no perfil "demo".
--
-- Idempotente: só insere o ponto cujo nome (sem diferenciar maiúsculas) ainda não existe. Usa
-- INSERT ... SELECT ... WHERE NOT EXISTS para não consumir a identidade a cada reexecução.
-- Dados fictícios: municípios e bairros diferentes, com e sem recolhimento domiciliar, um ponto
-- inativo e um ponto sem contato e horários (NULL, nunca texto vazio: ck_poc_contato/ck_poc_horarios).
INSERT INTO poc_ponto_coleta (poc_tx_nome, poc_tx_contato, poc_tx_horarios, poc_bl_rec_domiciliar, poc_tx_situacao,
                              poc_tx_logradouro, poc_tx_numero, poc_tx_bairro, poc_tx_municipio, poc_tx_uf,
                              poc_tx_cep, poc_tx_complemento)
SELECT v.nome, v.contato, v.horarios, v.domiciliar, v.situacao,
       v.logradouro, v.numero, v.bairro, v.municipio, v.uf, v.cep, v.complemento
FROM (VALUES
    ('Cooperativa Verde Vida', '(79) 3333-0000', 'Segunda a sexta, das 8h às 17h', TRUE,  'ATIVO',
     'Avenida Principal', 'S/N', 'Distrito Industrial', 'Lagarto', 'SE', '49400000', NULL),
    ('Ecoponto Centro', '(79) 3333-1111', 'Segunda a sábado, das 7h às 13h', FALSE, 'ATIVO',
     'Rua do Comércio', '100', 'Centro', 'Aracaju', 'SE', '49010000', 'Ao lado da praça'),
    ('Ponto Recicla Itabaiana', '(79) 3333-2222', 'Terça e quinta, das 8h às 12h', TRUE,  'ATIVO',
     'Rua da Feira', '25', 'Marcela', 'Itabaiana', 'SE', '49500000', NULL),
    ('Ponto Comunitário Sem Contato', NULL, NULL, FALSE, 'ATIVO',
     'Travessa das Palmeiras', 'S/N', 'Bairro Novo', 'Estância', 'SE', '49200000', NULL),
    ('Ecoponto Desativado', '(79) 3333-3333', 'Funcionamento suspenso', FALSE, 'INATIVO',
     'Rua Antiga', '9', 'Centro', 'Lagarto', 'SE', '49400001', NULL)
) AS v(nome, contato, horarios, domiciliar, situacao,
       logradouro, numero, bairro, municipio, uf, cep, complemento)
WHERE NOT EXISTS (
    SELECT 1 FROM poc_ponto_coleta p WHERE LOWER(BTRIM(p.poc_tx_nome)) = LOWER(v.nome)
);
