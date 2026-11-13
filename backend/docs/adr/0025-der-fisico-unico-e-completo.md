# 25. DER físico único e completo

**Data:** 2026-11-08

**Status:** Aceita (decisão aprovada pelo estudante em 2026-11-08)

## Contexto

O ADR 0004 (2026-10-11) dividiu o diagrama Entidade-Relacionamento em duas visões, a estrutural (cadastros) e a transacional (fluxo de recolhimento), para reduzir a carga visual de um modelo que se previa grande. Com as migrações V1 e V2 aplicadas, o modelo ficou com **9 tabelas e 11 chaves estrangeiras**, e a divisão passou a custar mais do que rendia:

- a visão transacional repetia quatro tabelas como *stubs* (`mor_morador`, `mat_material`, `poc_ponto_coleta`, `ope_operador`) com colunas parciais, que precisavam ser ajustadas à mão sempre que a definição mudava, como o próprio ADR 0004 já listava entre os cuidados;
- o modelo completo exigia abrir dois arquivos `.dbml` e duas imagens;
- na prática, os nomes dos arquivos já tinham divergido do que os ADRs 0003 e 0004 citavam (`er.dbml`, `er.png`, `er-cadastros.dbml`);
- as alterações recentes (cópia do endereço com 500 caracteres, nota do ADR 0013) precisaram ser repetidas em mais de um arquivo.

Alternativas consideradas: **manter as duas visões**, o que preserva a leitura por domínio e mantém a duplicação e a sincronização manual; **um único DER com `TableGroup`**, que reduz a duplicação, mas deixa o posicionamento a cargo do dbdiagram.io e tende a cruzar linhas; e **um único DER com disposição manual das tabelas**, que elimina a duplicação e permite organizar o desenho para evitar cruzamentos, ao custo de uma imagem maior.

## Decisão

1. O modelo físico passa a ser documentado em **um único DER, completo**, com as 9 tabelas, seus campos, tipos, chaves e obrigatoriedade, e as 11 chaves estrangeiras.
2. Os arquivos têm **nomes estáveis**, sem número de versão, e ficam em `docs/diagramas/entidade-relacionamento-fisico/`:
   - `der-unificado.dbml`, a fonte do diagrama;
   - `der-unificado.svg`, a imagem exportada do dbdiagram.io (ADR 0003) com a disposição manual das tabelas.
3. As versões anteriores ficam no **histórico do Git**; não se criam arquivos `der-v1`, `der-v2` ou semelhantes. Os quatro arquivos das visões antigas (`der-estrutural.*` e `der-transacional.*`) foram removidos.
4. A **migração Flyway continua sendo a fonte da verdade do banco**. O DBML documenta as tabelas, os tipos, as chaves, as unicidades e o índice `idx_ope_poc`; os CHECKs e os índices de desempenho ficam nas migrações. Quando uma migração mudar o esquema, o DBML e o SVG são atualizados no mesmo Pull Request.
5. A **ordem das colunas** no desenho é escolhida pela legibilidade (por exemplo, `mor_nr_id` antes de `ope_nr_id` em `his_historico`, para evitar cruzamento de linhas) e pode diferir da ordem física da migração. A conferência entre o DBML e as migrações compara as colunas **por nome e atributos**, não pela posição.
6. O ADR 0003 continua válido (dbdiagram.io e DBML como ferramenta e linguagem). O ADR 0004 passa a **Substituída por ADR-0025**, com o texto original preservado.

## Consequências

- **Positivas:** uma só fonte e uma só imagem para todo o banco; fim dos *stubs* e da sincronização manual entre arquivos; o desenho pode ser lido de uma vez e é mais fácil de apresentar; a conferência com as migrações é feita em um único arquivo.
- **Negativas:** a imagem é maior e mostra todas as tabelas ao mesmo tempo; o posicionamento manual precisa ser refeito no dbdiagram.io quando entrarem tabelas novas; se o modelo crescer muito, uma divisão em visões pode voltar a fazer sentido (nova decisão, com novo ADR).
- **Cuidados:** a exportação do SVG continua manual e depende do dbdiagram.io (ADR 0003), então a imagem deve ser reexportada sempre que o DBML mudar. Referências antigas aos nomes `der-estrutural` e `der-transacional` permanecem apenas nos relatórios de auditoria e nos ADRs, como registro histórico. A decisão de documentar no DER os índices da V3, se houver, será tomada junto da migração (Issue #33).

Relacionado: ADR 0003, ADR 0004 (substituído), ADR 0013, ADR 0016, Issue #33.
