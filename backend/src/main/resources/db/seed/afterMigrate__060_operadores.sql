-- Dados de demonstração do RF07 (operadores). Carregado só no perfil "demo".
--
-- Depende do seed 040 (pontos): o vínculo é feito pelo nome do ponto. Idempotente: só insere o
-- operador cujo e-mail ainda não existe (INSERT ... SELECT ... WHERE NOT EXISTS). Dois operadores no
-- mesmo ponto e um em outro (cenário de acesso cruzado). E-mails já normalizados (ck_ope_email_normalizado).
-- Servem de atores conhecidos nas demonstrações: X-Perfil: OPERADOR e X-Usuario-Id com o id do operador.
INSERT INTO ope_operador (ope_tx_nome, ope_tx_email, ope_tx_situacao, poc_nr_id)
SELECT v.nome, v.email, v.situacao, p.poc_nr_id
FROM (VALUES
    ('Carla Nunes',  'carla.nunes@exemplo.com',  'ATIVO', 'Cooperativa Verde Vida'),
    ('Paulo Santos', 'paulo.santos@exemplo.com', 'ATIVO', 'Cooperativa Verde Vida'),
    ('Rita Alves',   'rita.alves@exemplo.com',   'ATIVO', 'Ecoponto Centro')
) AS v(nome, email, situacao, ponto)
JOIN poc_ponto_coleta p ON LOWER(BTRIM(p.poc_tx_nome)) = LOWER(v.ponto)
WHERE NOT EXISTS (
    SELECT 1 FROM ope_operador o WHERE o.ope_tx_email = v.email
);
