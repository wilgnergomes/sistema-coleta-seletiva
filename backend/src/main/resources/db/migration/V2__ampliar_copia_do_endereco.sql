-- =====================================================================
-- V2: amplia a cópia do endereço da solicitação de 400 para 500 caracteres.
--
-- O texto copiado (ADR 0008) tem o formato
--   "logradouro, numero (complemento) - bairro - municipio/UF - CEP cep"
-- e, com todos os campos no limite do cadastro (150, 10, 100, 100, 100, 2 e 8),
-- chega a 489 caracteres. Com 400, esse endereço válido do RF02 era recusado.
-- Só o tamanho da coluna muda; a V1 já aplicada não é editada.
-- =====================================================================

ALTER TABLE coleta.sol_solicitacao
    ALTER COLUMN sol_tx_endereco_copia TYPE VARCHAR(500);
