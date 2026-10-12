# 8. Cópias de endereço e unidade guardadas na solicitação

**Data:** 2026-10-12

**Status:** Proposta

## Contexto

A RN04 exige guardar na solicitação uma cópia do endereço de atendimento e da unidade dos itens, de modo que mudanças posteriores no cadastro não alterem os dados históricos. Itens e endereço só podem ser corrigidos antes do agendamento, com nova validação de compatibilidade.

Foram consideradas as seguintes alternativas: apenas referenciar o cadastro por FK, o que faria o histórico mudar junto com o cadastro e violaria a RN04; copiar o endereço em colunas separadas, o que duplicaria a estrutura do endereço na solicitação sem benefício de consulta; manter versões do endereço em tabela própria, o que adiciona complexidade desproporcional ao projeto; e guardar uma cópia em texto único. Escolheu-se a última, por ser a mais simples e suficiente para exibição e auditoria.

## Decisão

1. O endereço de atendimento é copiado para `sol_tx_endereco_copia`, um texto único obrigatório, no momento da criação.
2. A unidade de cada item é copiada de `mat_tx_unidade` para `iso_tx_copia_unidade`, com o mesmo domínio de valores (KG, L, UN). Para itens em unidades de peça, as quantidades devem ser inteiras (RN02).
3. A solicitação não guarda FK para o endereço de origem. O endereço chega na requisição por identificador, o serviço confere que ele pertence ao morador (RF08) e então copia o texto.
4. Enquanto a solicitação não estiver agendada, a correção de itens ou endereço refaz as cópias com nova validação de compatibilidade (RN01 e RN04). Depois do agendamento as cópias não mudam.

## Consequências

- **Positivas:** o histórico da solicitação permanece correto mesmo que o morador mude ou o material troque de unidade; a exibição dos itens e dos totais (RF15) usa a unidade que valia no momento do pedido, sem misturar kg e unidades.
- **Negativas:** há duplicação intencional de dados; o banco não sabe de qual endereço cadastrado veio a cópia, nem se ele pertence ao morador.
- **Cuidados:** a verificação de que o endereço pertence ao morador é regra do serviço e precisa de teste. Se for necessária rastreabilidade de origem, uma migração futura pode adicionar uma FK opcional para `end_endereco`, sem alterar a cópia. Qualquer mudança no formato do texto copiado deve ser feita só para solicitações novas.
