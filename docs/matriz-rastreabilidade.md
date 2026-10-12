# Matriz de rastreabilidade dos requisitos funcionais

Liga cada requisito funcional (RF01 a RF15) às entidades, às dependências, aos dados de seed, aos endpoints e aos testes. Serve para três coisas:

1. dizer, para qualquer RF, **o que precisa existir antes para testá-lo**;
2. definir a **ordem dos ciclos** de entrega;
3. responder na arguição **qual parte do código atende a qual requisito**.

## Como ler e manter

- **Entidades:** nome da entidade e tabela entre parênteses. *Grava* = insere ou altera; *lê* = só consulta ou valida contra ela.
- **Dependências:** RFs que precisam estar prontos (ou ter dados equivalentes no seed) para que o RF possa ser testado de ponta a ponta.
- **Seed:** dados fictícios (RNF06) que ajudam a demonstrar ou testar o RF. "Não vem de seed" significa que o próprio teste cria o dado.
- **Endpoints e testes:** começam como *pendente (ciclo N)*. Ao fechar o ciclo, preencher com `MÉTODO /caminho` e `Classe#método`, e conferir com a documentação OpenAPI (RNF07).

## Matriz

| RF | Entidades que usa | RF dos quais depende | Dados que podem vir de seed | Endpoints | Testes |
|----|-------------------|----------------------|-----------------------------|-----------|--------|
| **RF01** Cadastrar moradores | **Grava:** Morador (`mor_morador`) | — | Moradores fictícios com e-mails distintos, já em minúsculas | _pendente (ciclo 1)_ | _pendente (ciclo 1)_ |
| **RF02** Gerenciar endereços | **Grava:** Endereço (`end_endereco`)<br>**Lê:** Morador | RF01 | 1 a 2 endereços por morador; um com `S/N`, um sem complemento; ao menos um morador com 2 endereços (testa a posse do endereço no RF08) | _pendente (ciclo 1)_ | _pendente (ciclo 1)_ |
| **RF03** Gerenciar materiais recicláveis | **Grava:** Material (`mat_material`) | — | Papel, plástico, vidro e metal em KG; ao menos um em L e um em UN (testa RN02 e a não mistura de unidades no RF15); um material inativo | _pendente (ciclo 1)_ | _pendente (ciclo 1)_ |
| **RF04** Gerenciar pontos de coleta | **Grava:** Ponto de coleta (`poc_ponto_coleta`, endereço embutido) | — | Pontos em municípios e bairros diferentes; com e sem recolhimento domiciliar; um inativo; um sem contato e horários | _pendente (ciclo 1)_ | _pendente (ciclo 1)_ |
| **RF05** Vincular materiais aos pontos | **Grava:** Material aceito (`mac_material_aceito`)<br>**Lê:** Ponto de coleta, Material | RF03, RF04 | Vínculos com e sem limite e observação; um ponto que **não** aceita vidro (cenário "material incompatível") | _pendente (ciclo 2)_ | _pendente (ciclo 2)_ |
| **RF06** Consultar pontos e materiais | **Lê:** Ponto de coleta, Material, Material aceito | RF03, RF04, RF05 | Mesmo seed dos RF03 a RF05, com variedade de município, bairro e material para os filtros; ponto inativo que aceita vidro (não pode aparecer no filtro) | _pendente (ciclo 2)_ | _pendente (ciclo 2)_ |
| **RF07** Administrar responsáveis | **Grava:** Operador (`ope_operador`)<br>**Lê:** Ponto de coleta | RF04 | 2 operadores no mesmo ponto e 1 em outro ponto (testa acesso cruzado); um operador inativo | _pendente (ciclo 2)_ | _pendente (ciclo 2)_ |
| **RF08** Criar solicitação | **Grava:** Solicitação (`sol_solicitacao`), Item (`iso_item_solicitacao`), Histórico (`his_historico`, registro inicial com responsável morador)<br>**Lê:** Morador, Endereço, Ponto de coleta, Material, Material aceito | RF01, RF02, RF03, RF04, RF05 | **Não vem de seed:** o teste cria a solicitação. Os pré-requisitos vêm do seed dos RF01 a RF05 | _pendente (ciclo 3)_ | _pendente (ciclo 3)_ |
| **RF09** Consultar solicitações | **Lê:** Solicitação, Item, Histórico, Ponto de coleta, Morador (e Operador, quando ele é o responsável no histórico) | RF08 | Solicitações de demonstração em cada situação e em períodos diferentes; ao menos 2 pontos e 2 moradores (testa o filtro pelo contexto do ator) | _pendente (ciclo 3)_ | _pendente (ciclo 3)_ |
| **RF10** Agendar recolhimento | **Grava:** Solicitação (situação, agendamento, versão), Histórico<br>**Lê:** Operador, Ponto de coleta | RF07, RF08 | Uma solicitação SOLICITADA no ponto do operador e outra em ponto diferente | _pendente (ciclo 4)_ | _pendente (ciclo 4)_ |
| **RF11** Recusar solicitação | **Grava:** Solicitação, Histórico<br>**Lê:** Operador | RF07, RF08 (RF10 ou seed, para o caso de rejeição fora de SOLICITADA) | Uma solicitação SOLICITADA e uma AGENDADA | _pendente (ciclo 4)_ | _pendente (ciclo 4)_ |
| **RF12** Cancelar solicitação | **Grava:** Solicitação, Histórico (responsável morador ou operador)<br>**Lê:** Morador, Operador | RF07, RF08, RF10 | Solicitações em SOLICITADA, AGENDADA, EM_RECOLHIMENTO e CONCLUIDA (as duas últimas para os casos de rejeição) | _pendente (ciclo 4)_ | _pendente (ciclo 4)_ |
| **RF13** Registrar execução | **Grava:** Solicitação (situação, conclusão, versão), Item (quantidade efetiva), Histórico<br>**Lê:** Operador | RF07, RF08, RF10 | Uma solicitação AGENDADA com 2 ou mais itens; outra EM_RECOLHIMENTO | _pendente (ciclo 5)_ | _pendente (ciclo 5)_ |
| **RF14** Reagendar e registrar falha | **Grava:** Solicitação, Histórico (motivo, agendamento, informações da tentativa)<br>**Lê:** Operador | RF10, RF13 | Uma solicitação AGENDADA e outra EM_RECOLHIMENTO | _pendente (ciclo 5)_ | _pendente (ciclo 5)_ |
| **RF15** Exibir resumo gerencial | **Lê:** Solicitação, Item, Material | RF08, RF13 | Concluídas com 2 ou mais materiais, em unidades diferentes (KG e UN) e em períodos diferentes; estimada diferente da efetiva; solicitações em outras situações que **não** podem entrar na soma. O lote de desempenho do RNF04 não faz parte deste seed (ver "Dataset de desempenho") | _pendente (ciclo 6)_ | _pendente (ciclo 6)_ |

## Ordem dos ciclos (sugestão)

A ordem segue a coluna de dependências. Ajustar conforme as issues e a divisão da equipe.

| Ciclo | RFs | O que entrega | Cenário de aceite habilitado (seção 13) |
|-------|-----|---------------|-----------------------------------------|
| 1 | RF01, RF02, RF03, RF04 | Cadastros base, sem dependências entre si além de RF02 sobre RF01. Podem andar em paralelo | — |
| 2 | RF05, RF06, RF07 | Catálogo completo, pesquisa e operadores | Cadastro e pesquisa |
| 3 | RF08, RF09 | Criação transacional com itens e histórico inicial, protocolo, consulta com filtros | Solicitação válida; Material incompatível |
| 4 | RF10, RF11, RF12 | Mecanismo de transição de situação, versão (409) e histórico, reaproveitado nos ciclos seguintes | Agendamento inválido; Cancelamento e situação final |
| 5 | RF13, RF14 | Execução, conclusão com quantidades, falha e reagendamento | Execução completa; Tentativa sem sucesso |
| 6 | RF15 e fechamento da RN05 | Resumo gerencial, bloqueios retroativos nos RF04 e RF05 e medição de desempenho do RNF04 (ver "Dataset de desempenho") | Persistência e indicadores |

## Aceites que só podem ser testados depois de outro RF

Estes critérios pertencem a um RF, mas dependem de outro estar pronto. Ficam registrados aqui para não serem esquecidos ao fechar o ciclo.

| Aceite | Pertence a | Só é testável depois de |
|--------|-----------|-------------------------|
| Material inativo não pode integrar uma nova solicitação | RF03 | RF08 |
| Ponto inativo não aparece como disponível para atendimento | RF04 | RF06 (consulta) e RF08 (RN01) |
| Operador não atualiza solicitações de outro ponto | RF07 | RF10 a RF14, quando o controle de acesso estiver implementado |
| Bloquear inativação de ponto ou remoção de vínculo com solicitações abertas (RN05) | RF04, RF05 | RF08 e RF12 (para resolver ou cancelar as abertas) |
| Não excluir fisicamente cadastros vinculados a solicitações (RN03) | RF01, RF03, RF04 | RF08 (o banco já garante com `ON DELETE RESTRICT`, mas o teste precisa de uma solicitação) |
| Recusa fora de SOLICITADA é rejeitada | RF11 | RF10 ou seed de solicitação AGENDADA |
| Tentar cancelar recolhimento iniciado ou concluído | RF12 | RF13 ou seed |

## Testes obrigatórios do RNF08 e onde caem

| Teste exigido | RF principal |
|---------------|--------------|
| Compatibilidade de materiais | RF08 (RN01) |
| Quantidades (estimada positiva, efetiva não negativa, inteiro para unidade de peça) | RF08, RF13 (RN02) |
| Limites cadastrados | RF05, RF08 |
| Transições de situação | RF10 a RF14 (RN06) |
| Concorrência (mesma versão, segundo pedido rejeitado com 409) | RF10 a RF14 (ADR 0009) |
| Integração: criação com itens e histórico, com rollback em caso de falha | RF08 (RN06) |

## Dataset de desempenho (RNF04)

O RNF04 não pertence a um único RF: a meta de 95% das consultas em até 2 segundos, com 1.000 solicitações, vale para as listagens e filtros em geral. O lote é, por isso, um dado de teste próprio, separado do seed de demonstração e do seed funcional de cada RF.

| Item | Definição |
|------|-----------|
| Consultas medidas | RF06 (pesquisa de pontos), RF09 (consulta de solicitações com filtros e paginação) e RF15 (resumo gerencial) |
| Conteúdo | 1.000 solicitações com itens e histórico coerentes, distribuídas entre situações, pontos, materiais e períodos |
| Como é carregado | Script ou perfil isolado, executado sob demanda; nunca junto da carga de demonstração |
| Quando medir | Depois que RF09 e RF15 existirem (ciclo 6). O script pode ser escrito antes |
| Evidência | Registrar máquina, ferramenta e resultados, como exige o RNF04 |

## Observações para o seed

- Solicitações inseridas direto no banco devem respeitar as restrições: cada linha de histórico precisa de **exatamente um** responsável (`ope_nr_id` ou `mor_nr_id`, ADR 0011), e cada solicitação precisa ter histórico coerente com a situação atual.
- A cópia de endereço (`sol_tx_endereco_copia`) e a de unidade (`iso_tx_copia_unidade`) devem ser preenchidas também no seed (ADR 0008).
- O lote de 1.000 solicitações do RNF04 é um dataset à parte (ver "Dataset de desempenho"), para não poluir a apresentação nem os dados dos testes funcionais.
