# Matriz de rastreabilidade dos requisitos funcionais

Liga cada requisito funcional (RF01 a RF15) às entidades, às dependências, aos dados de seed, aos endpoints e aos testes. Serve para três coisas:

1. dizer, para qualquer RF, **o que precisa existir antes para testá-lo**;
2. definir a **ordem dos ciclos** de entrega;
3. responder na arguição **qual parte do código atende a qual requisito**.

**Fontes:** especificação do trabalho (seções 6 a 8 e 13), migração `V1__criar_esquema_incial.sql`, contrato `docs/api/openapi.yaml` e ADRs 0005 a 0019.

## Como ler e manter

- **Entidades:** nome da entidade e tabela entre parênteses. *Grava* = insere ou altera; *lê* = só consulta ou valida contra ela.
- **Dependências:** RFs que precisam estar **implementados** para que o RF funcione. Estados necessários só para testar uma rejeição são pré-condição de teste e vêm de fixture (ver a regra abaixo da tabela).
- **Dados de seed:** cada item é classificado como **Seed** (dado de demonstração, serve para apresentar o sistema) ou **Fixture** (estado preparado só para teste, inclusive estados difíceis de atingir pelo fluxo normal). "Não vem de seed" significa que o próprio teste cria o dado.
- **Tipos de teste:** `[UNIT]` unitário; `[INT]` integração com banco; `[API]` contrato HTTP; `[CONC]` concorrência.
- **Endpoints:** começam como *pendente (ciclo N)*. Passam a *Previstos* quando constam do contrato `docs/api/openapi.yaml`, sempre com `MÉTODO /caminho`. Ao fechar o ciclo, cada endpoint recebe o registro de execução, por exemplo `✔ executado em AAAA-MM-DD`, depois de conferido com a documentação gerada pelo springdoc (RNF07). Endpoint documentado não prova que o requisito foi atendido: só o teste executado prova.
- **Testes:** começam como *pendente (ciclo N)*. Ao fechar o ciclo, preencher com `[TIPO] Classe#método` e o registro de execução. Esta coluna não é alterada pelo contrato.
- Linhas com **Previstos** listam testes já decididos; continuam pendentes até o ciclo entregá-los.
- Itens marcados *(a confirmar)* dependem das decisões listadas na seção "Decisões a confirmar".

## Matriz

| RF | Entidades que usa | RF dos quais depende | Dados que podem vir de seed | Endpoints | Testes |
|----|-------------------|----------------------|-----------------------------|-----------|--------|
| **RF01** Cadastrar moradores | **Grava:** Morador (`mor_morador`) | — | **Seed:** moradores fictícios com e-mails distintos, já em minúsculas | **Previstos (ciclo 1):**<br>`POST /moradores`<br>`GET /moradores`<br>`GET /moradores/{id}`<br>`PUT /moradores/{id}` | _pendente (ciclo 1)_ |
| **RF02** Gerenciar endereços | **Grava:** Endereço (`end_endereco`)<br>**Lê:** Morador | RF01 | **Seed:** 1 a 2 endereços por morador; um com `S/N` (valor explícito, ADR 0005), um sem complemento<br>**Fixture:** ao menos um morador com 2 endereços (testa a posse do endereço no RF08) | **Previstos (ciclo 1):**<br>`POST /moradores/{moradorId}/enderecos`<br>`GET /moradores/{moradorId}/enderecos`<br>`PUT /moradores/{moradorId}/enderecos/{id}` | _pendente (ciclo 1)_ |
| **RF03** Gerenciar materiais recicláveis | **Grava:** Material (`mat_material`)<br>**Lê:** Item (`iso_item_solicitacao`) e Material aceito (`mac_material_aceito`), para o bloqueio de alteração de unidade (ADR 0013) | — (a regra de bloqueio só é testável depois do RF05 e do RF08) | **Seed:** papel, plástico, vidro e metal em KG; ao menos um em L e um em UN; um material inativo<br>**Fixture:** material já usado em item de solicitação; material vinculado a ponto sem itens | **Previstos (ciclo 1):**<br>`POST /materiais`<br>`GET /materiais` (compartilhado com RF06)<br>`GET /materiais/{id}`<br>`PUT /materiais/{id}` (inclui inativar e a regra do ADR 0013) | _pendente (ciclo 1; testes de bloqueio de unidade no ciclo 3)_<br>**Previstos:** `[INT]` alterar a unidade de material com item → 409 e estado preservado; `[INT]` alterar a unidade de material com vínculo em `mac_material_aceito` → 409 e estado preservado; `[INT]` nome e descrição continuam editáveis |
| **RF04** Gerenciar pontos de coleta | **Grava:** Ponto de coleta (`poc_ponto_coleta`, endereço embutido) | — | **Seed:** pontos em municípios e bairros diferentes; com e sem recolhimento domiciliar; um inativo; um sem contato e horários. A listagem administrativa pode filtrar por situação | **Previstos (ciclo 1):**<br>`POST /pontos-coleta`<br>`PUT /pontos-coleta/{id}` (inclui inativar)<br>`GET /pontos-coleta` com filtro `situacao` para o administrador (compartilhado com RF06) | _pendente (ciclo 1)_ |
| **RF05** Vincular materiais aos pontos | **Grava:** Material aceito (`mac_material_aceito`)<br>**Lê:** Ponto de coleta, Material | RF03, RF04 | **Seed:** vínculos com e sem limite e observação; um ponto que **não** aceita vidro (cenário "material incompatível") | **Previstos (ciclo 2):**<br>`PUT /pontos-coleta/{pontoId}/materiais/{materialId}` (cria ou atualiza o vínculo)<br>`DELETE /pontos-coleta/{pontoId}/materiais/{materialId}` | _pendente (ciclo 2)_ |
| **RF06** Consultar pontos e materiais | **Lê:** Ponto de coleta, Material, Material aceito | RF03, RF04, RF05 | **Seed:** mesmo catálogo dos RF03 a RF05, com variedade de município, bairro e material para os filtros<br>**Fixture:** ponto inativo que aceita vidro. A consulta pública devolve **só pontos ativos**; o ponto inativo só aparece na listagem administrativa do RF04, com filtro de situação | **Previstos (ciclo 2):**<br>`GET /pontos-coleta` (público: só ativos)<br>`GET /pontos-coleta/{id}`<br>`GET /materiais` (compartilhado com RF03) | _pendente (ciclo 2)_ |
| **RF07** Administrar responsáveis | **Grava:** Operador (`ope_operador`)<br>**Lê:** Ponto de coleta | RF04 | **Seed:** 2 operadores no mesmo ponto e 1 em outro ponto (testa acesso cruzado)<br>**Fixture:** um operador inativo | **Previstos (ciclo 2):**<br>`POST /operadores`<br>`GET /operadores`<br>`GET /operadores/{id}`<br>`PUT /operadores/{id}` (muda ponto e situação) | _pendente (ciclo 2)_ |
| **RF08** Criar solicitação | **Grava:** Solicitação (`sol_solicitacao`), Item (`iso_item_solicitacao`), Histórico (`his_historico`, registro inicial com responsável morador)<br>**Lê:** Morador, Endereço, Ponto de coleta, Material, Material aceito | RF01, RF02, RF03, RF04, RF05 | **Não vem de seed:** o teste cria a solicitação. Os pré-requisitos vêm do seed dos RF01 a RF05 | **Previstos (ciclo 3):**<br>`POST /solicitacoes`<br>`PUT /solicitacoes/{id}` (correção antes do agendamento, RN04) | _pendente (ciclo 3)_<br>**Previstos:** `[UNIT]` RN01 (compatibilidade e limites) e RN02 (quantidades); `[INT]` criação com itens e histórico inicial, com exatamente um responsável (`ck_his_responsavel`); `[INT]` rollback: após falha induzida na gravação de item ou histórico, não existe solicitação, item nem histórico; `[INT]` unidade copiada para o item igual à do material no momento da criação |
| **RF09** Consultar solicitações | **Lê:** Solicitação, Item, Histórico, Ponto de coleta, Morador (e Operador, quando ele é o responsável no histórico) | RF08 | **Seed:** solicitações de demonstração em cada situação e em períodos diferentes, criadas com histórico coerente<br>**Fixture:** ao menos 2 pontos e 2 moradores (testa o filtro pelo contexto do ator). Filtro de período pela data de criação (`sol_dt_criacao`) *(a confirmar)* | **Previstos (ciclo 3):**<br>`GET /solicitacoes`<br>`GET /solicitacoes/{id}` (traz itens e histórico) | _pendente (ciclo 3)_ |
| **RF10** Agendar recolhimento | **Grava:** Solicitação (situação, agendamento, versão), Histórico<br>**Lê:** Operador, Ponto de coleta | RF07, RF08 | **Fixture:** uma solicitação SOLICITADA no ponto do operador e outra em ponto diferente | **Previstos (ciclo 4):**<br>`POST /solicitacoes/{id}/agendamento` | _pendente (ciclo 4)_<br>**Previstos:** `[UNIT]` transições permitidas e proibidas (RN06); `[INT]` agendamento válido grava situação e histórico; `[CONC]` mesma versão em dois pedidos → o segundo recebe 409 |
| **RF11** Recusar solicitação | **Grava:** Solicitação, Histórico<br>**Lê:** Operador | RF07, RF08 (RF10 só se o estado AGENDADA não vier de fixture) | **Fixture:** uma solicitação SOLICITADA e uma AGENDADA | **Previstos (ciclo 4):**<br>`POST /solicitacoes/{id}/recusa` | _pendente (ciclo 4)_<br>**Previstos:** `[UNIT]` recusa sem motivo rejeitada; `[API]` recusa fora de SOLICITADA → 409; `[INT]` recusa válida aparece no histórico do morador |
| **RF12** Cancelar solicitação | **Grava:** Solicitação, Histórico (responsável morador ou operador)<br>**Lê:** Morador, Operador | RF07, RF08, RF10 (os cenários em EM_RECOLHIMENTO e CONCLUIDA usam fixture ou o RF13) | **Fixture:** solicitações em SOLICITADA, AGENDADA, EM_RECOLHIMENTO e CONCLUIDA (as duas últimas para os casos de rejeição) | **Previstos (ciclo 4):**<br>`POST /solicitacoes/{id}/cancelamento` | _pendente (ciclo 4)_<br>**Previstos:** `[UNIT]` motivo obrigatório; `[API]` cancelar recolhimento iniciado ou concluído → 409; `[INT]` histórico gravado com o responsável correto (morador ou operador) |
| **RF13** Registrar execução | **Grava:** Solicitação (situação, conclusão, versão), Item (quantidade efetiva), Histórico<br>**Lê:** Operador | RF07, RF08, RF10 | **Fixture:** uma solicitação AGENDADA com 2 ou mais itens; outra EM_RECOLHIMENTO | **Previstos (ciclo 5):**<br>`POST /solicitacoes/{id}/inicio`<br>`POST /solicitacoes/{id}/conclusao` | _pendente (ciclo 5)_<br>**Previstos:** `[UNIT]` quantidade efetiva não negativa e inteira para unidade de peça (RN02); `[UNIT]` concluir sem nenhuma quantidade positiva é rejeitado; `[CONC]` mesma versão → 409 |
| **RF14** Reagendar e registrar falha | **Grava:** Solicitação, Histórico (motivo, agendamento, informações da tentativa)<br>**Lê:** Operador | RF10, RF13 | **Fixture:** uma solicitação AGENDADA e outra EM_RECOLHIMENTO. Pela RN06, reagendar vale em AGENDADA e mantém AGENDADA; a falha vale em EM_RECOLHIMENTO e volta a AGENDADA | **Previstos (ciclo 5):**<br>`POST /solicitacoes/{id}/reagendamento`<br>`POST /solicitacoes/{id}/falha` | _pendente (ciclo 5)_<br>**Previstos:** `[UNIT]` transições permitidas e proibidas; `[INT]` cada tentativa e alteração preserva o histórico; `[CONC]` mesma versão → 409 |
| **RF15** Exibir resumo gerencial | **Lê:** Solicitação, Item, Material | RF08, RF13 | **Seed:** solicitações concluídas com 2 ou mais materiais, em unidades diferentes (KG e UN) e em períodos diferentes<br>**Fixture:** estimada diferente da efetiva; solicitações em outras situações que **não** podem entrar na soma. O lote de desempenho do RNF04 não faz parte deste seed (ver "Dataset de desempenho"). Definição do resumo na seção "Definição do resumo gerencial (RF15)" | **Previstos (ciclo 6):**<br>`GET /resumo-gerencial` | _pendente (ciclo 6)_ |

**Regra: dependência de implementação não é pré-condição de teste.** A coluna "RF dos quais depende" lista o que precisa estar implementado. Um estado necessário só para testar uma rejeição (por exemplo, AGENDADA para a recusa do RF11) pode ser preparado por fixture válida e não obriga a implementar o RF que o produziria. A única dependência de implementação compartilhada entre RF10 e RF14 é o mecanismo de transição e versão criado no RF10.

## Ordem dos ciclos (sugestão)

A ordem segue a coluna de dependências. Ajustar conforme as issues e a divisão da equipe.

| Ciclo | RFs | O que entrega | Cenário de aceite habilitado (seção 13) |
|-------|-----|---------------|-----------------------------------------|
| 1 | RF01, RF02, RF03, RF04 | Cadastros base, sem dependências entre si além de RF02 sobre RF01. Podem andar em paralelo | — |
| 2 | RF05, RF06, RF07 | Catálogo completo, pesquisa e operadores | Cadastro e pesquisa |
| 3 | RF08, RF09 | Criação transacional com itens e histórico inicial, protocolo, consulta com filtros. Inclui os testes de bloqueio de unidade do RF03 | Solicitação válida; Material incompatível |
| 4 | RF10, RF11, RF12 | Mecanismo de transição de situação, versão (409) e histórico, reaproveitado nos ciclos seguintes | Agendamento inválido; Cancelamento e situação final |
| 5 | RF13, RF14 | Execução, conclusão com quantidades, falha e reagendamento | Execução completa; Tentativa sem sucesso |
| 6 | RF15 e fechamento da RN05 | Resumo gerencial, bloqueios retroativos nos RF04 e RF05 e medição de desempenho do RNF04 (ver "Dataset de desempenho") | Persistência e indicadores |

## Decisões a confirmar

Estas escolhas não decorrem diretamente da especificação original. Ficam registradas aqui até a confirmação formal.

| Decisão | Valor adotado | Status | Registro |
|---------|---------------|--------|----------|
| Data do período das quantidades recolhidas (RF15) | `sol_dt_conclusao` | A confirmar | ADR 0014 (Proposta) |
| Data do período das solicitações por situação (RF15) | `sol_dt_criacao` | A confirmar | ADR 0014 (Proposta) |
| Data do filtro de período (RF09) | `sol_dt_criacao` | A confirmar | ADR 0014 (Proposta) |
| Fuso de negócio dos limites de período | `America/Sao_Paulo`, configurável | A confirmar | ADR 0014 (Proposta) |
| Limites do período | Intervalo semiaberto: início do dia inicial (inclusive) até o início do dia seguinte ao final (exclusivo) | A confirmar | ADR 0014 (Proposta) |
| Alteração da unidade do material | Bloqueada, com 409, se existir qualquer item ou vínculo | Decidida | ADR 0013 (Aceita) |

## Definição do resumo gerencial (RF15)

- Quantidades recolhidas: somar `iso_nr_quantidade_efetiva` apenas de solicitações em CONCLUIDA, nunca a estimada.
- Agrupar por material, período (`sol_dt_conclusao`, *a confirmar*) e `iso_tx_copia_unidade`.
- Não somar unidades diferentes: cada unidade tem totais independentes, sem total geral entre elas.
- Item com quantidade efetiva 0 conta como 0; item sem quantidade efetiva não entra.
- Número de solicitações por situação: contar todas as situações; quando houver filtro de período, usar `sol_dt_criacao` (*a confirmar*).
- Limites do período no fuso de negócio, como intervalo semiaberto.

## Aceites que só podem ser testados depois de outro RF

Estes critérios pertencem a um RF, mas dependem de outro estar pronto ou de uma fixture válida. Ficam registrados aqui para não serem esquecidos ao fechar o ciclo.

| Aceite | Pertence a | Só é testável depois de |
|--------|-----------|-------------------------|
| Material inativo não pode integrar uma nova solicitação | RF03 | RF08 |
| Alteração de unidade bloqueada com item ou vínculo (ADR 0013) | RF03 | RF05 (vínculo) e RF08 (item), ou fixture equivalente |
| Ponto inativo não aparece como disponível para atendimento | RF04 | RF06 (consulta) e RF08 (RN01) |
| Operador não atualiza solicitações de outro ponto | RF07 | RF10 a RF14, quando o controle de acesso estiver implementado |
| Bloquear inativação de ponto ou remoção de vínculo com solicitações abertas (RN05) | RF04, RF05 | RF08 e RF12 (para resolver ou cancelar as abertas) |
| Não excluir fisicamente cadastros vinculados a solicitações (RN03) | RF01, RF03, RF04 | RF08 (o banco já garante com `ON DELETE RESTRICT`, mas o teste precisa de uma solicitação) |
| Recusa fora de SOLICITADA é rejeitada | RF11 | RF10 ou fixture de solicitação AGENDADA |
| Tentar cancelar recolhimento iniciado ou concluído | RF12 | RF13 ou fixture |

## Testes obrigatórios do RNF08 e onde caem

| Teste exigido | Tipo | RF principal |
|---------------|------|--------------|
| Compatibilidade de materiais | `[UNIT]` | RF08 (RN01) |
| Quantidades (estimada positiva, efetiva não negativa, inteiro para unidade de peça) | `[UNIT]` | RF08, RF13 (RN02) |
| Limites cadastrados | `[UNIT]` | RF05, RF08 |
| Transições de situação, permitidas e proibidas | `[UNIT]` | RF10 a RF14 (RN06) |
| Concorrência (mesma versão, segundo pedido rejeitado com 409) | `[CONC]` | RF10 a RF14 (ADR 0009) |
| Criação com itens e histórico, com rollback em caso de falha | `[INT]` | RF08 (RN06) |

## Evidências exigidas pelos requisitos não funcionais

| RNF | Evidência a registrar | Quando |
|-----|----------------------|--------|
| RNF04 | Relatório de desempenho com máquina, ferramenta e resultados, medido sobre o dataset de desempenho | Ciclo 6 |
| RNF06 | Dados de demonstração fictícios; nenhuma senha, token ou credencial de banco versionados (variáveis de ambiente); consultas públicas sem informações pessoais | Ao longo dos ciclos; conferir na entrega |
| RNF07 | Documentação OpenAPI (ou coleção Postman) com exemplos e respostas; README com requisitos, configuração e comandos; migrações e dados de demonstração reproduzíveis | Atualizar a cada ciclo |
| RNF08 | Resultado da execução dos testes automatizados, incluindo o teste de integração com rollback | Ao fechar cada ciclo |

## Dataset de desempenho (RNF04)

O RNF04 não pertence a um único RF: a meta de 95% das consultas em até 2 segundos, com 1.000 solicitações, vale para as listagens e filtros em geral. O lote é, por isso, um dado de teste próprio, separado do seed de demonstração e das fixtures de cada RF.

| Item | Definição |
|------|-----------|
| Consultas medidas | RF06 (pesquisa de pontos), RF09 (consulta de solicitações com filtros e paginação) e RF15 (resumo gerencial) |
| Conteúdo | 1.000 solicitações com itens e histórico coerentes, distribuídas entre situações, pontos, materiais e períodos |
| Como é carregado | Script ou perfil isolado, executado sob demanda; nunca junto da carga de demonstração |
| Quando medir | Depois que RF09 e RF15 existirem (ciclo 6). O script pode ser escrito antes |
| Evidência | Registrar máquina, ferramenta e resultados, como exige o RNF04 |

## Observações para o seed e as fixtures

- Solicitações inseridas direto no banco devem respeitar as restrições: cada linha de histórico precisa de **exatamente um** responsável (`ope_nr_id` ou `mor_nr_id`, ADR 0011), e cada solicitação precisa ter histórico coerente com a situação atual.
- A cópia de endereço (`sol_tx_endereco_copia`) e a de unidade (`iso_tx_copia_unidade`) devem ser preenchidas também no seed e nas fixtures (ADR 0008), e a unidade copiada deve ser igual à do material.
- O seed de demonstração serve para apresentar o sistema. As fixtures criam estados específicos para testar rejeições e falhas e não devem ser carregadas na demonstração.
- O lote de 1.000 solicitações do RNF04 é um dataset à parte (ver "Dataset de desempenho"), para não poluir a apresentação nem os dados dos testes funcionais.

## Pendências fora da matriz

- **DER:** atualizar a nota de `mat_tx_unidade` em `der-estrutural.dbml` com a restrição do ADR 0013 e regenerar a imagem, conforme o ADR 0003.
- **Migração V2 com índices candidatos:** `iso_item_solicitacao (mat_nr_id)`, `his_historico (sol_nr_id, his_dt_data)` (ambos recomendados no ADR 0007 e ausentes na V1) e `mac_material_aceito (mat_nr_id)`. A chave primária de `mac_material_aceito` começa por `poc_nr_id` e atende mal as consultas só por material. Antes de incluir cada índice, confirmar o padrão das consultas e o plano de execução, e documentar a motivação.

