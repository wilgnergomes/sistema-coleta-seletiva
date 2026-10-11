# Sistema de Coleta Seletiva

API REST para conectar **moradores**, **pontos de coleta** e **operadores** responsáveis pelo recolhimento de materiais recicláveis. O sistema reúne informações sobre locais de descarte e materiais aceitos e permite solicitar, agendar e acompanhar recolhimentos em endereços cadastrados.

> Trabalho da disciplina **Programação Web I** — Curso Superior de Sistemas de Informação, Instituto Federal de Sergipe (IFS).

> 🚧 **Status:** em desenvolvimento. Este README é atualizado conforme cada etapa é concluída.

## Sumário

- [Objetivo](#objetivo)
- [Funcionalidades](#funcionalidades)
- [Atores](#atores)
- [Fluxo principal](#fluxo-principal)
- [Situações da solicitação](#situações-da-solicitação)
- [Tecnologias](#tecnologias)
- [Requisitos para executar](#requisitos-para-executar)
- [Como executar](#como-executar)
- [Configuração](#configuração)
- [Documentação da API](#documentação-da-api)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Roadmap](#roadmap)
- [Contribuindo](#contribuindo)
- [Equipe](#equipe)

## Objetivo

Organizar o cadastro de pontos de coleta, materiais recicláveis e solicitações de recolhimento, garantindo que cada solicitação seja atendida por um ponto **compatível com os materiais informados**.

Objetivos específicos:

- Facilitar a localização de pontos de coleta ativos.
- Informar os materiais aceitos e as orientações de separação.
- Registrar solicitações com vários materiais.
- Controlar agendamentos e situações de atendimento.
- Consultar o histórico dos recolhimentos.

## Funcionalidades

**Cadastros e consultas**

- Moradores e seus endereços.
- Materiais recicláveis (com inativação, sem exclusão física).
- Pontos de coleta e vínculo dos materiais aceitos (com limite opcional por solicitação).
- Operadores vinculados a pontos de coleta.
- Pesquisa de pontos por nome, município, bairro e material aceito, com paginação.

**Recolhimento**

- Criação de solicitação com vários itens e geração de protocolo único.
- Consulta de solicitações com filtros (período, situação e ponto) e paginação.
- Agendamento, recusa e cancelamento, sempre com histórico.
- Início e conclusão do recolhimento, com registro das quantidades efetivas.
- Reagendamento e registro de tentativa sem sucesso.
- Resumo gerencial: solicitações por situação e quantidades recolhidas por material e período.

## Atores

| Ator | Responsabilidades |
|---|---|
| **Morador** | Consultar pontos e materiais, cadastrar seus endereços, solicitar recolhimento, acompanhar e cancelar suas solicitações. |
| **Operador do ponto** | Consultar solicitações do seu ponto, aceitar ou recusar, agendar, iniciar e concluir recolhimentos. |
| **Administrador** | Gerenciar materiais, pontos e responsáveis, vincular materiais aceitos e consultar registros e indicadores. |

## Fluxo principal

1. O administrador cadastra materiais, pontos de coleta e os materiais aceitos em cada ponto.
2. O morador consulta pontos ativos, verifica as orientações e escolhe um ponto com recolhimento domiciliar.
3. O morador informa endereço, materiais, quantidades estimadas e observações. O sistema gera um protocolo e registra a situação `SOLICITADA`.
4. O operador verifica a solicitação, agenda o atendimento ou recusa com justificativa.
5. Na data marcada, o operador inicia o recolhimento, registra as quantidades efetivamente recolhidas e conclui o atendimento.
6. O morador acompanha as alterações e consulta o histórico.

## Situações da solicitação

| Situação atual | Próximas situações permitidas |
|---|---|
| `SOLICITADA` | `AGENDADA`, `RECUSADA` ou `CANCELADA` |
| `AGENDADA` | `EM_RECOLHIMENTO` ou `CANCELADA` (o reagendamento mantém `AGENDADA`) |
| `EM_RECOLHIMENTO` | `CONCLUIDA` ou `AGENDADA` (após tentativa sem sucesso) |
| `CONCLUIDA`, `RECUSADA`, `CANCELADA` | Situações finais, sem novas transições |

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 4.1.1 | Framework da aplicação |
| Spring Web | API REST |
| Spring Data JPA | Persistência |
| Bean Validation | Validação de entrada |
| PostgreSQL | Banco de dados relacional |
| Flyway | Migrações do esquema do banco |
| Spring Boot Actuator | Monitoramento (endpoint de saúde) |
| Testcontainers | Testes de integração com PostgreSQL real |
| Docker | Execução dos containers (testes de integração e, futuramente, banco de desenvolvimento) |
| Spring Boot DevTools | Reinício automático durante o desenvolvimento |
| Lombok | Redução de código repetitivo (getters, construtores etc.) |
| Maven (wrapper) | Build e gerenciamento de dependências |

## Requisitos para executar

- **JDK 21** (variável `JAVA_HOME` apontando para ele).
- **Docker** em execução (necessário para os testes de integração com Testcontainers).
- **Git**.

Não é necessário instalar o Maven: o projeto inclui o *wrapper* (`mvnw` / `mvnw.cmd`).

Para conferir o ambiente:

```bash
java -version
docker version
```

## Como executar

Clone o repositório:

```bash
git clone https://github.com/<usuario>/sistema-coleta-seletiva.git
cd sistema-coleta-seletiva
```

Compilar e executar os testes:

```bash
# Windows (PowerShell/CMD)
.\mvnw.cmd clean verify

# Linux / macOS / Git Bash
./mvnw clean verify
```

Executar a aplicação:

```bash
# Windows (PowerShell/CMD)
.\mvnw.cmd spring-boot:run

# Linux / macOS / Git Bash
./mvnw spring-boot:run
```

> ⚠️ A execução da aplicação depende da configuração do banco de dados (em definição, ver [Configuração](#configuração)).

Verificar se a aplicação está no ar:

```bash
curl http://localhost:8080/actuator/health
```

## Configuração

As credenciais e parâmetros de conexão com o banco são configurados por **variáveis de ambiente**. Nenhuma senha, token ou credencial é versionado no repositório.

> 🚧 A lista de variáveis e o arquivo `.env.example` serão adicionados junto com a configuração do banco (Docker Compose).

## Documentação da API

> 🚧 Em construção. A documentação dos endpoints, exemplos e respostas será disponibilizada em formato OpenAPI (Swagger) ou coleção Postman.

Padrões previstos para a API:

- JSON como formato de troca.
- Códigos HTTP: `201` para criação, `400` para entrada inválida, `404` para registro ausente e `409` para conflitos de regra ou de versão.
- Listagens paginadas, com limite máximo de 100 registros por página.
- Tratamento global de erros, com mensagem, código e erros por campo.

## Estrutura do repositório

```text
.
├── docs/            # Documentação do projeto (decisões, diagramas, API)
├── src/
│   ├── main/        # Código-fonte e recursos da aplicação
│   └── test/        # Testes automatizados
├── mvnw / mvnw.cmd  # Maven Wrapper
├── pom.xml          # Dependências e build
└── README.md
```

> A pasta `docs/` será criada conforme os documentos forem produzidos.

## Roadmap

- [x] Levantamento de requisitos
- [x] Criação do repositório e do projeto Spring Boot
- [ ] Modelagem do sistema (casos de uso, diagrama de estados, sequência)
- [ ] Modelagem do banco de dados (diagrama ER e migrações)
- [ ] Configuração do banco com Docker Compose
- [ ] Contrato da API (OpenAPI)
- [ ] Cadastros: materiais, pontos, vínculos, moradores, endereços e operadores
- [ ] Consultas com filtros e paginação
- [ ] Solicitação de recolhimento (compatibilidade, itens e protocolo)
- [ ] Transições de situação e histórico
- [ ] Resumo gerencial
- [ ] Testes de integração e de concorrência
- [ ] Interface web responsiva (segunda unidade)

## Contribuindo

Fluxo de trabalho da equipe (branches, commits e Pull Requests): veja o [CONTRIBUTING.md](CONTRIBUTING.md).

## Equipe

| Nome | GitHub |
|---|---|
| _Wilgner_ | [@wilgnergomes](https://github.com/wilgnergomes) |
| _Nome do integrante_ | [@usuario](https://github.com/usuario) |
| _Nome do integrante_ | [@usuario](https://github.com/usuario) |

---

Projeto acadêmico. Todos os dados utilizados na demonstração são fictícios.