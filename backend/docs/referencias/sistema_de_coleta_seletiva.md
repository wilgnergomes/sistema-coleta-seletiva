[Page 1]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

# Sistema de Coleta Seletiva
**Proposta de projeto e especificação de requisitos**
Curso superior de Sistemas de Informação Disciplina PROGRAMAÇÃO WEB I

Este trabalho propõe desenvolver um sistema que conecte moradores, pontos de coleta e responsáveis pelo recolhimento de materiais recicláveis. A equipe deverá entregar uma API em Spring Boot integrada a um banco de dados relacional e para segunda unidade, uma interface web responsiva, utilizável em computadores e celulares.

## 1 Contexto e objetivos
A falta de informações sobre locais de descarte e sobre os materiais aceitos dificulta a participação da população na coleta seletiva. O sistema reunirá essas informações e permitirá solicitar, agendar e acompanhar recolhimentos em endereços cadastrados.

**Objetivo geral:** organizar o cadastro de pontos de coleta, materiais recicláveis e solicitações de recolhimento, garantindo que cada solicitação seja atendida por um ponto compatível com os materiais informados.

**Objetivos específicos**
Facilitar a localização de pontos ativos; informar os materiais aceitos e as orientações de separação; registrar solicitações com vários materiais; controlar agendamentos e situações de atendimento; consultar o histórico dos recolhimentos.

## 2 Escopo do trabalho
O escopo obrigatório compreende cadastros, consultas com filtros, compatibilidade entre ponto e material, solicitação de recolhimento, agendamento, atualização de situação e histórico. Os dados devem ser persistidos e as regras devem ser validadas no backend.

Mapas, geolocalização, notificações push, roteirização de veículos, pagamentos e aplicativo nativo são extensões opcionais. O atendimento poderá ser realizado por uma cooperativa ou organização fictícia, com dados de demonstração.

Sistema de Coleta Seletiva Especificação do trabalho 1

[Page 2]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 3 Atores e permissões

| Ator | Responsabilidades |
| :--- | :--- |
| **Morador** | Consultar pontos e materiais; cadastrar seus endereços; solicitar recolhimento; acompanhar e cancelar suas solicitações conforme as regras. |
| **Operador do ponto** | Consultar solicitações do seu ponto; aceitar ou recusar atendimento; agendar; iniciar e concluir recolhimentos. |
| **Administrador** | Gerenciar materiais, pontos e responsáveis; vincular materiais aceitos; consultar os registros e indicadores. |

O protótipo básico poderá usar atores fictícios e uma seleção de usuário para demonstração. Essa seleção não constitui autenticação nem deve ser utilizada em produção. Se login for implementado, o backend deverá validar identidade, permissões e vínculo com o ponto em cada operação restrita.

## 4 Fluxo principal
1. O administrador cadastra materiais, pontos de coleta e os materiais aceitos em cada ponto.
2. O morador consulta pontos ativos, verifica as orientações e escolhe um ponto que realize recolhimento domiciliar.
3. O morador informa endereço, materiais, quantidades estimadas e observações; o sistema gera um protocolo e registra a situação SOLICITADA.
4. O operador verifica a solicitação, agenda o atendimento ou recusa com justificativa.
5. Na data marcada, o operador inicia o recolhimento e, depois, registra as quantidades efetivamente recolhidas e conclui o atendimento.
6. O morador acompanha as alterações e consulta o histórico.

## 5 Prioridade dos requisitos
Todos os requisitos identificados como RF, RNF e RN são obrigatórios para o projeto básico. Os itens opcionais aparecem apenas na seção de extensões. A interface pode variar, desde que implemente os comportamentos e os critérios de aceite definidos neste documento.

Sistema de Coleta Seletiva Especificação do trabalho 2

[Page 3]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 6 Requisitos funcionais de cadastro e consulta

**RF01 Cadastrar moradores**
Cadastrar, consultar e atualizar nome, e-mail e telefone. Exigir nome e e-mail válido; impedir duplicação de e-mail após normalização. Aceite: cadastro válido é persistido e um e-mail repetido é rejeitado.

**RF02 Gerenciar endereços**
Cadastrar endereços vinculados a um morador, com logradouro, número ou indicação de sem número, bairro, município, UF, CEP e complemento opcional. Aceite: endereço incompleto é rejeitado e cada endereço possui um único proprietário.

**RF03 Gerenciar materiais recicláveis**
Cadastrar, consultar, atualizar e inativar materiais, com nome, descrição, unidade de medida e orientação de preparo. Exemplos: papel, plástico, vidro e metal. Aceite: nome duplicado é rejeitado e material inativo não pode integrar uma nova solicitação.

**RF04 Gerenciar pontos de coleta**
Registrar nome, endereço, contato, horários de funcionamento, situação e disponibilidade de recolhimento domiciliar. Aceite: ponto sem nome ou endereço é rejeitado; ponto inativo não aparece como disponível para atendimento.

**RF05 Vincular materiais aos pontos**
Permitir associar vários materiais a cada ponto, registrando observações e eventual limite por solicitação na unidade do material. Aceite: não repetir o mesmo vínculo e rejeitar limite menor ou igual a zero.

**RF06 Consultar pontos e materiais**
Pesquisar pontos por nome, município, bairro e material aceito, com paginação; exibir endereço, horários, materiais aceitos e orientações. Aceite: filtrar por vidro retorna somente pontos ativos que aceitam vidro.

**RF07 Administrar responsáveis**
Cadastrar operadores e vinculá-los a pontos de coleta. Aceite: o operador vinculado a um ponto não pode atualizar solicitações de outro ponto quando o controle de acesso estiver implementado.

Sistema de Coleta Seletiva Especificação do trabalho 3

[Page 4]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 7 Requisitos funcionais de recolhimento

**RF08 Criar solicitação**
Registrar morador, ponto escolhido, endereço, observações e pelo menos um item com material e quantidade estimada positiva. Gerar protocolo único e data de criação. Aceite: solicitação com material não aceito pelo ponto é rejeitada sem gravação parcial.

**RF09 Consultar solicitações**
Exibir detalhes, itens, protocolo, situação, agendamento e histórico; filtrar por período, situação e ponto, com paginação. Aceite: a consulta retorna somente registros que correspondam aos filtros e ao contexto do ator.

**RF10 Agendar recolhimento**
Permitir ao operador aceitar a solicitação e informar data e hora futuras. Registrar a alteração no histórico. Aceite: agendamento passado é rejeitado; agendamento válido altera a situação para AGENDADA.

**RF11 Recusar solicitação**
Permitir recusar uma solicitação ainda em SOLICITADA, exigindo justificativa. Aceite: recusa sem motivo é rejeitada e a recusa válida aparece no histórico do morador.

**RF12 Cancelar solicitação**
Permitir cancelamento em SOLICITADA ou AGENDADA, com motivo obrigatório. Aceite: tentativa de cancelar um recolhimento iniciado ou concluído é rejeitada.

**RF13 Registrar execução**
Permitir iniciar uma solicitação AGENDADA, alterando-a para EM_RECOLHIMENTO. Para concluir, registrar data, responsável e quantidade efetiva de cada item, admitindo zero para item não recolhido. Aceite: concluir sem nenhuma quantidade positiva é rejeitado.

**RF14 Reagendar e registrar falha**
Permitir reagendar solicitações AGENDADAS ou retornar de EM_RECOLHIMENTO para AGENDADA após tentativa sem sucesso, com justificativa e nova data futura. Aceite: cada tentativa e alteração preserva o histórico.

**RF15 Exibir resumo gerencial**
Apresentar número de solicitações por situação e quantidades recolhidas por material e período, considerando apenas atendimentos CONCLUÍDOS. Aceite: não somar kg com unidades nem incluir quantidades estimadas como efetivas.

Sistema de Coleta Seletiva Especificação do trabalho 4

[Page 5]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 8 Regras de negócio

**RN01 Compatibilidade de atendimento**
Novas solicitações exigem ponto ativo, recolhimento domiciliar habilitado e todos os materiais ativos e aceitos pelo ponto. Limites cadastrados devem ser respeitados; o mesmo material não pode aparecer duas vezes na solicitação.

**RN02 Quantidades e unidades**
A unidade pertence ao material e deve ser exibida nos itens. Usar decimal para peso ou volume e valor inteiro para unidade de peça. Quantidade estimada deve ser positiva; quantidade efetiva não pode ser negativa.

**RN03 Integridade e histórico**
Não excluir fisicamente materiais, pontos ou moradores vinculados a solicitações. Inativar os cadastros aplicáveis e preservar os registros. Gravar situação anterior, nova situação, data, responsável e motivo de cada alteração.

**RN04 Preservação dos dados da solicitação**
Guardar uma cópia do endereço de atendimento e da unidade dos itens na solicitação. Mudanças posteriores no cadastro não podem alterar os dados históricos. Itens e endereço podem ser corrigidos apenas antes do agendamento, com nova validação de compatibilidade.

**RN05 Alterações no ponto de coleta**
A inativação de um ponto ou remoção de um vínculo de material deve ser bloqueada enquanto houver solicitações abertas afetadas. O operador deve resolver ou cancelar esses atendimentos com justificativa antes da alteração.

**RN06 Atomicidade e concorrência**
Criação com itens e mudanças de situação com histórico devem ocorrer em uma única transação. Se duas atualizações concorrentes usarem a mesma versão, rejeitar a segunda com conflito, evitando perda silenciosa de dados.

**Transições permitidas**

| Situação atual | Próxima situação |
| :--- | :--- |
| **SOLICITADA** | AGENDADA, RECUSADA ou CANCELADA |
| **AGENDADA** | EM_RECOLHIMENTO ou CANCELADA; reagendamento mantém AGENDADA |
| **EM_RECOLHIMENTO** | CONCLUÍDA ou AGENDADA após tentativa sem sucesso |
| **CONCLUÍDA, RECUSADA ou CANCELADA** | Situações finais sem novas transições |

Sistema de Coleta Seletiva Especificação do trabalho 5

[Page 6]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 9 Requisitos não funcionais

**RNF01 Arquitetura e tecnologias**
Utilizar Java 21, Spring Boot, API REST, PostgreSQL e Spring Data JPA. Organizar Entity, DTO, Repository, Service e Controller; manter as regras de negócio no serviço e usar Bean Validation na entrada.

**RNF02 Usabilidade e responsividade**
Disponibilizar interface web funcional em larguras de 360 px e 1366 px, sem rolagem horizontal nas telas principais. Identificar campos obrigatórios, confirmar ações de cancelamento e mostrar erros próximos aos campos.

**RNF03 Contrato da API e erros**
Usar JSON e métodos HTTP adequados. Retornar 201 para criação, 400 para entrada inválida, 404 para registro ausente e 409 para conflitos de regra ou versão. Usar tratamento global com mensagem, código e erros de campos, sem expor stack trace.

**RNF04 Desempenho verificável**
Paginar listagens com limite máximo de 100 registros por página e índices coerentes com os filtros. Meta acadêmica: 95% das consultas em até 2 segundos em teste local com 1.000 solicitações, registrando máquina, ferramenta e resultados.

**RNF05 Persistência e consistência**
Usar chaves primárias, estrangeiras, restrições de unicidade e transações. Datas de eventos devem incluir fuso ou offset. Após reiniciar o backend, todos os dados confirmados devem permanecer disponíveis.

**RNF06 Segurança básica e dados de demonstração**
Utilizar dados fictícios na entrega. Não versionar senhas, tokens ou credenciais de banco; configurar por variáveis de ambiente. Não devolver informações pessoais em consultas públicas. Se houver login, armazenar somente hash seguro da senha e testar permissões no backend.

**RNF07 Documentação e reprodução**
Documentar endpoints, exemplos e respostas por OpenAPI ou coleção Postman; fornecer README com requisitos, configuração e comandos de execução, além de scripts ou migrações e dados de demonstração.

**RNF08 Testes essenciais**
Automatizar testes para compatibilidade de materiais, quantidades, transições, limites e concorrência. Executar ao menos um teste de integração para criação com itens e histórico, incluindo rollback em caso de falha.

Sistema de Coleta Seletiva Especificação do trabalho 6

[Page 7]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 10 Modelo de dados sugerido

| Entidade | Dados principais e relacionamentos |
| :--- | :--- |
| **Morador** | ID, nome, e-mail único e telefone; possui vários endereços e solicitações. |
| **Endereço** | ID, morador, logradouro, número, bairro, município, UF, CEP e complemento. |
| **Material** | ID, nome único, descrição, unidade, orientação e situação. |
| **Ponto de coleta** | ID, nome, endereço, contato, horários, situação e recolhimento domiciliar. |
| **Material aceito** | Ponto + material com vínculo único; observações e limite opcional. |
| **Operador** | ID, nome, e-mail e vínculos com pontos. |
| **Solicitação** | ID, protocolo único, morador, ponto, cópia do endereço, situação, datas, observações e versão. |
| **Item da solicitação** | Solicitação, material, cópia da unidade, quantidade estimada e efetiva. Vínculo único por solicitação e material. |
| **Histórico** | Solicitação, situação anterior e nova, data, responsável, motivo e informações da tentativa. |

O modelo deverá ser apresentado em um diagrama entidade-relacionamento. A equipe poderá ajustar nomes e campos, preservando as relações e regras. Endereço do ponto pode ser embutido no cadastro; histórico e itens devem permanecer relacionados à solicitação.

## 11 Interfaces mínimas
Consulta pública de pontos e materiais; cadastro de moradores e endereços; administração de materiais e pontos; vínculo de materiais aceitos; formulário de solicitação com vários itens; lista e detalhe das solicitações com histórico; painel do operador para agendar e executar; resumo gerencial.

## 12 Extensões opcionais
Autenticação com Spring Security e JWT; mapa dos pontos; aviso por e-mail ou push; anexos de fotos; aplicativo Flutter; exportação de relatórios. Extensões só devem ser iniciadas após o funcionamento do escopo obrigatório.

Sistema de Coleta Seletiva Especificação do trabalho 7

[Page 8]
SISTEMAS DE INFORMAÇÃO | DESENVOLVIMENTO WEB I INSTITUTO FEDERAL DE SERGIPE

## 13 Cenários de aceite para demonstração

| Cenário | Resultado esperado |
| :--- | :--- |
| **Cadastro e pesquisa** | Cadastrar um ponto que aceite papel e plástico; encontrá-lo pelos filtros de município e material. |
| **Solicitação válida** | Criar solicitação com dois materiais aceitos; obter protocolo único, itens persistidos e histórico inicial. |
| **Material incompatível** | Tentar solicitar vidro no ponto anterior; receber erro e confirmar ausência de gravação parcial. |
| **Agendamento inválido** | Informar data passada; manter a solicitação em SOLICITADA e apresentar mensagem clara. |
| **Execução completa** | Agendar, iniciar e concluir; visualizar quantidades efetivas e todas as mudanças no histórico. |
| **Cancelamento e situação final** | Cancelar uma solicitação agendada; tentar reabrir ou concluir e receber conflito. |
| **Tentativa sem sucesso** | Iniciar atendimento, registrar falha e reagendar; preservar a tentativa anterior no histórico. |
| **Persistência e indicadores** | Reiniciar a aplicação; confirmar os dados e conferir totais por material sem misturar unidades. |

## 14 Organização e entregas
Formação sugerida: equipes de até 3 integrantes. Etapa 1: requisitos, protótipos e modelo de dados. Etapa 2: banco, API e regras. Etapa 3(Unidade 2): interface e integração. Etapa 4: testes. As datas serão definidas pelo professor.

Entregar repositório Git com histórico de participação; código do backend e frontend; scripts ou migrações; dados fictícios; README; documentação da API; relatório com modelo de dados, decisões e evidências dos testes. Cada integrante será interrogado e perguntas serão feitas direcionadas ou não para um desenvolvedor para explicar uma parte do código e sua relação com os requisitos a pedido.

| Critério de avaliação sugerido | Peso |
| :--- | :--- |
| **Requisitos e modelagem** | 15% |
| **API REST e regras de negócio** | 25% |
| **Persistência e integridade** | 15% |
| **Interface responsiva e integração** | 20% |
| **Organização e qualidade do código** | 10% |
| **Testes** | 5% |
| **Apresentação e demonstração** | 10% |

Sistema de Coleta Seletiva Especificação do trabalho 8