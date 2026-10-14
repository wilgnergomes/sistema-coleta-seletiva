# 20. Organização dos pacotes da aplicação

**Data:** 2026-10-13

**Status:** Proposta

## Contexto

É necessário definir uma convenção para organizar os pacotes e as classes da aplicação, visando facilitar a localização do código, manter a consistência estrutural e favorecer a manutenção do sistema.

Foram consideradas três abordagens principais:

- **Organização por camada técnica:** agrupar as classes conforme suas responsabilidades técnicas, como controllers, services, repositories e entidades.
- **Organização por funcionalidade:** agrupar as classes conforme os domínios ou funcionalidades do sistema, mantendo próximas as classes relacionadas a cada responsabilidade funcional.
- **Organização híbrida:** agrupar inicialmente por funcionalidade e, dentro de cada pacote funcional, separar as classes por camada técnica.

Como o projeto está em fase inicial de implementação e utiliza Spring Boot, considera-se importante adotar uma estrutura simples, previsível e alinhada às convenções técnicas conhecidas pela equipe. Também se busca evitar a criação prematura de pacotes funcionais cuja necessidade ainda não esteja justificada pela complexidade do sistema.

A nomenclatura dos pacotes, classes, atributos e métodos deverá respeitar a ADR 0002, que estabelece o português para os nomes relacionados ao domínio do projeto, preservando os termos técnicos e as convenções exigidas pelas tecnologias utilizadas.

## Decisão

Adotar a **organização por camada técnica**, agrupando as classes conforme suas responsabilidades, sem criar inicialmente pacotes específicos para cada funcionalidade ou tabela do banco de dados.

A estrutura inicial será organizada da seguinte forma:

```text
br.edu.ifs.coletaseletiva/
├── config/
├── controller/
├── dto/
├── entity/
├── exception/
├── form/
├── repository/
└── service/
```

Os pacotes terão as seguintes responsabilidades:

- **`controller`:** receber as requisições HTTP e encaminhar as operações para a camada de serviço.
- **`service`:** implementar os casos de uso e as regras de negócio da aplicação.
- **`repository`:** concentrar as interfaces de acesso e persistência dos dados.
- **`entity`:** conter as entidades persistentes correspondentes ao modelo de dados.
- **`form`:** conter os modelos utilizados para receber e validar os dados de entrada das requisições.
- **`dto`:** conter os modelos utilizados para estruturar os dados de saída das respostas da API.
- **`exception`:** conter as classes relacionadas ao tratamento e à representação padronizada de erros.
- **`config`:** conter as configurações da aplicação.

A distribuição das classes seguirá a responsabilidade de cada uma, sem exigir uma correspondência direta entre pacotes e tabelas. Por exemplo, as entidades `Morador`, `Endereco`, `Material`, `PontoColeta` e `Solicitacao` ficarão inicialmente em `entity/`, enquanto seus respectivos repositories ficarão em `repository/`.

A nomenclatura dos elementos específicos do domínio permanecerá em português, enquanto os nomes dos pacotes técnicos poderão seguir as convenções amplamente utilizadas no ecossistema Spring, em conformidade com a ADR 0002.

A estrutura poderá ser revista futuramente caso o crescimento da aplicação justifique uma organização por funcionalidade ou uma abordagem híbrida.

## Consequências

- **Positivas:** estrutura simples e previsível; facilidade para localizar classes por responsabilidade técnica; alinhamento com convenções comuns em projetos Spring Boot; menor complexidade inicial de organização.
- **Negativas:** classes relacionadas a uma mesma funcionalidade ficarão distribuídas por diferentes pacotes; a localização de todas as partes de um caso de uso poderá exigir navegação entre várias camadas.
- **Possibilidade futura:** a organização poderá evoluir para pacotes por funcionalidade ou módulos mais independentes se a complexidade e a necessidade de delimitação dos domínios justificarem a mudança.