# 13. Bloqueio da alteração da unidade do material com dependências

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

O RF03 permite atualizar materiais, e a RN02 define que a unidade de medida pertence ao material. A unidade é copiada para cada item no momento da solicitação (ADR 0008), o que protege os itens já gravados. Mesmo assim, a troca da unidade de um material já utilizado tem efeitos que a cópia não resolve:

- o limite por solicitação de `mac_material_aceito` é expresso "na unidade do material" (RF05); trocar a unidade muda o significado do limite sem alterá-lo;
- solicitações abertas e agendadas mantêm a cópia antiga, enquanto o limite já seria interpretado na unidade nova;
- o mesmo material passaria a ter totais em duas unidades no resumo gerencial (RF15), que não são comparáveis.

Foram consideradas as seguintes alternativas: permitir a troca livremente, tratando os efeitos acima por regras adicionais e revisão manual dos limites; tornar a unidade imutável após o cadastro, o que impede corrigir um erro de cadastro em material que nunca foi usado; bloquear a troca somente quando houver item em solicitação, o que deixa os limites dos vínculos interpretados na unidade errada; e bloquear a troca quando houver qualquer item ou vínculo. Escolheu-se a última, por eliminar os três efeitos em vez de gerenciá-los, mantendo a correção livre enquanto o material não tiver uso.

## Decisão

1. A unidade (`mat_tx_unidade`) de um material só pode ser alterada se não existir nenhum item em `iso_item_solicitacao` nem nenhum vínculo em `mac_material_aceito` para esse material.
2. O bloqueio independe da situação do material. O vínculo de material aceito não tem situação: ele existe ou foi removido, e a existência de qualquer vínculo bloqueia, mesmo sem limite cadastrado.
3. A tentativa bloqueada resulta em resposta 409 com mensagem clara (RNF03), sem alterar nenhum dado.
4. Nome, descrição, orientação e situação continuam editáveis, sujeitos às demais regras (por exemplo, a unicidade do nome).
5. Para passar a usar outra unidade em um material já utilizado, o material atual é inativado e se cadastra outro, com nome diferente por causa de `uq_mat_nome`.
6. A regra é verificada no serviço, por consulta às duas tabelas. Um `CHECK` não alcança outras tabelas, e o projeto mantém regras entre múltiplas tabelas no backend.

## Consequências

- **Positivas:** limites, solicitações abertas e relatórios nunca ficam em unidades diferentes das registradas; a cópia de unidade do ADR 0008 deixa de ser a única proteção; a regra é simples de explicar e de testar.
- **Negativas:** para trocar a unidade de um material vinculado, é preciso remover antes os vínculos, e a RN05 só permite isso quando não houver solicitações abertas afetadas; corrigir erro de unidade em material já usado exige cadastrar outro material com nome diferente; o bloqueio por qualquer vínculo é mais conservador que o necessário, já que um vínculo sem limite não depende da unidade.
- **Limitação de concorrência:** a verificação e a alteração da unidade podem se cruzar com a criação concomitante de uma solicitação com esse material, e `mat_material` não possui coluna de versão. O volume é baixo e o cruzamento é raro, então o risco é aceito. Se for necessário eliminá-lo, bloquear a linha do material nos dois fluxos.
- **Relação com outros ADRs:** complementa e restringe o ADR 0008, sem reescrevê-lo. A afirmação daquele ADR de que o histórico permanece correto "mesmo que o material troque de unidade" passa a valer, para materiais com uso, apenas como proteção adicional, já que a troca é bloqueada.
- **Cuidados:** testes de integração devem cobrir a rejeição com 409 e a preservação do estado do banco, tanto com item quanto com vínculo, além da edição livre de nome e descrição; o teste do RF08 que confere que a unidade copiada é igual à do material não substitui os testes do RF03. O RF15 continua agregando por `iso_tx_copia_unidade`. A verificação consulta os itens e os vínculos por material, então os índices `iso_item_solicitacao (mat_nr_id)` e `mac_material_aceito (mat_nr_id)` são candidatos para a migração V2, sujeitos à confirmação do plano de execução. A nota de `mat_tx_unidade` no `der-estrutural.dbml` deve ser atualizada.
