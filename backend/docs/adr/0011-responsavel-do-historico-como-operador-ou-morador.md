# 11. Responsável do histórico como operador ou morador

**Data:** 2026-10-12

**Status:** Aceita

## Contexto

A RN03 e a especificação do modelo de dados (seção 10) exigem registrar obrigatoriamente, em cada mudança de situação, o responsável pela ação. O morador cria a solicitação e pode cancelar a sua (RF08 e RF12), enquanto o operador aceita, recusa, agenda, inicia, conclui e registra falhas (RF10, RF11, RF13 e RF14). Portanto, o responsável de um registro do histórico pode ser um morador ou um operador, que estão em tabelas diferentes. O primeiro rascunho tinha apenas FK para operador, o que impedia registrar a criação e o cancelamento feitos pelo morador.

Foram consideradas as seguintes alternativas: uma única coluna com tipo e identificador, sem FK, que perde a integridade referencial; uma tabela supertipo de atores, especializada por morador e operador, que serve bem quando há muitos tipos de ator, mas aumenta a estrutura para apenas dois; e duas colunas com FK reais, das quais apenas uma é preenchida em cada linha (arco exclusivo). Escolheu-se o arco exclusivo, por manter as FKs verdadeiras com o menor custo.

## Decisão

A tabela `his_historico` tem `ope_nr_id` e `mor_nr_id`, ambos opcionais e com FK. Para garantir a obrigatoriedade exigida na especificação, uma restrição `CHECK` (`num_nonnulls(ope_nr_id, mor_nr_id) = 1`) no banco de dados exige que exatamente um dos campos seja preenchido. Isso impede tanto que o histórico fique sem responsável quanto que tenha dois responsáveis simultâneos.

O responsável nunca é informado pelo cliente: o serviço determina-o pela ação executada e pelo ator do contexto, e um único método de registro de histórico preenche a coluna correspondente.

## Consequências

- **Positivas:** integridade referencial preservada nos dois casos; consultas simples por responsável; o banco rejeita linhas sem responsável (garantindo o cumprimento da RN03) ou com dois responsáveis simultâneos.
- **Negativas:** uma das colunas é sempre nula; um terceiro tipo de ator que mude situações exigiria nova coluna ou a migração para um supertipo.
- **Cuidados:** a restrição garante a presença de um responsável no banco, mas não quem realmente agiu. A identidade do ator vem do contexto (seleção de usuário no protótipo, que não é autenticação, ou da sessão se houver login), e o serviço valida se o morador é dono da solicitação e se o operador está vinculado ao ponto (RF07). Testes devem cobrir morador tentando agendar, operador de outro ponto tentando concluir e histórico gravado com a coluna correta.