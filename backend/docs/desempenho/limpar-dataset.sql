-- Limpeza do dataset de desempenho do RNF04 (Issue #58): remove SOMENTE o que a carga criou.
--
-- Cada linha apagada é identificada pelo marcador PERF (e-mails 'perf.morador.%' e 'perf.operador.%', nomes 'PERF %'),
-- na ordem inversa das chaves estrangeiras (ON DELETE RESTRICT): histórico, itens, solicitações, vínculos, operadores,
-- pontos, endereços, moradores e materiais. Dados de demonstração e quaisquer outros dados são preservados.
-- Pode ser executada com segurança sem ter carregado o dataset (não apaga nada) e mais de uma vez.
--
--   docker exec -i coleta-seletiva-db psql -U coleta -d coleta_desempenho -v ON_ERROR_STOP=1 < docs/desempenho/limpar-dataset.sql
--
-- Se alguma linha de fora do dataset apontar para um registro PERF (por exemplo, uma solicitação criada por alguém para um
-- ponto PERF), a chave estrangeira recusa a exclusão e a transação inteira é desfeita: nada é apagado pela metade.

BEGIN;

SET LOCAL search_path TO coleta;

DELETE FROM his_historico
WHERE sol_nr_id IN (SELECT s.sol_nr_id FROM sol_solicitacao s
                    JOIN mor_morador m ON m.mor_nr_id = s.mor_nr_id WHERE m.mor_tx_email LIKE 'perf.morador.%');

DELETE FROM iso_item_solicitacao
WHERE sol_nr_id IN (SELECT s.sol_nr_id FROM sol_solicitacao s
                    JOIN mor_morador m ON m.mor_nr_id = s.mor_nr_id WHERE m.mor_tx_email LIKE 'perf.morador.%');

DELETE FROM sol_solicitacao
WHERE mor_nr_id IN (SELECT mor_nr_id FROM mor_morador WHERE mor_tx_email LIKE 'perf.morador.%');

DELETE FROM mac_material_aceito
WHERE poc_nr_id IN (SELECT poc_nr_id FROM poc_ponto_coleta WHERE poc_tx_nome LIKE 'PERF %');

DELETE FROM ope_operador WHERE ope_tx_email LIKE 'perf.operador.%';

DELETE FROM poc_ponto_coleta WHERE poc_tx_nome LIKE 'PERF %';

DELETE FROM end_endereco
WHERE mor_nr_id IN (SELECT mor_nr_id FROM mor_morador WHERE mor_tx_email LIKE 'perf.morador.%');

DELETE FROM mor_morador WHERE mor_tx_email LIKE 'perf.morador.%';

DELETE FROM mat_material WHERE mat_tx_nome LIKE 'PERF %';

COMMIT;
