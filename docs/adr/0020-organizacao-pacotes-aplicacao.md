# 20. Organização dos pacotes da aplicação

**Data:** 2026-10-13

**Status:** Aceita

## Contexto

É necessário definir uma convenção para organizar os pacotes e as classes da aplicação, visando facilitar a localização do código, manter a consistência estrutural e favorecer a manutenção do sistema.

Foram consideradas três abordagens principais:

- **Organização por camada técnica:** agrupar as classes conforme suas responsabilidades técnicas, como controllers, services, repositories e entidades.
- **Organização por funcionalidade:** agrupar as classes conforme os domínios ou funcionalidades do sistema, mantendo próximas as classes relacionadas a cada responsabilidade funcional.
- **Organização híbrida:** agrupar inicialmente por funcionalidade e, dentro de cada pacote funcional, separar as classes por camada técnica.

Como o projeto está em fase inicial de implementação e utiliza Spring Boot, considera-se importante adotar uma estrutura simples, previsível e alinhada às convenções técnicas conhecidas pela equipe. Também se busca evitar a criação prematura de pacotes funcionais cuja necessidade ainda não esteja justificada pela complexidade do sistema.

O RNF01 pede a separação entre Entity, DTO, Repository, Service e Controller. O contrato da API (ADR 0017) distingue os dados de entrada dos de saída, o que justifica pacotes separados para `form` (entrada) e `dto` (saída). As conversões entre esses modelos e a entidade são frequentes e não pertencem à regra de negócio, o que justifica o pacote `mapper`. Os domínios fechados de valores (ADR 0012) são usados por entidades, forms e DTOs ao mesmo tempo, o que justifica o pacote `enumeration`.

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
├── enumeration/
├── exception/
├── form/
├── mapper/
├── repository/
└── service/
```

Os pacotes terão as seguintes responsabilidades:

- **`controller`:** receber as requisições HTTP e encaminhar as operações para a camada de serviço.
- **`service`:** implementar os casos de uso e as regras de negócio da aplicação, incluindo as verificações de permissão do ator.
- **`repository`:** concentrar as interfaces de acesso e persistência dos dados.
- **`entity`:** conter as entidades persistentes correspondentes ao modelo de dados.
- **`form`:** conter os modelos utilizados para receber e validar os dados de entrada das requisições.
- **`dto`:** conter os modelos utilizados para estruturar os dados de saída das respostas da API, incluindo o formato padronizado de erro (`RespostaErro` e `ErroCampo`).
- **`mapper`:** converter os formulários em entidades e as entidades em DTOs. Não contém regras de negócio, validações, verificação de permissão nem acesso ao banco, apenas a cópia de campos entre os modelos.
- **`enumeration`:** conter os enums de domínio (por exemplo, `SituacaoCadastro`, `SituacaoSolicitacao` e `Unidade`), usados pelas entidades, forms e DTOs. O nome do pacote não pode ser `enum`, por ser palavra reservada do Java. As regras de mapeamento seguem a ADR 0012.
- **`exception`:** conter as classes relacionadas ao tratamento de erros: as exceções próprias, o enum `CodigoErro` e o tratador global (`TratadorGlobalExcecoes`). O `CodigoErro` permanece neste pacote, e não em `enumeration`, por ser parte do tratamento de erros, e não do domínio.
- **`config`:** conter as configurações da aplicação.

A distribuição das classes seguirá a responsabilidade de cada uma, sem exigir uma correspondência direta entre pacotes e tabelas. Por exemplo, as entidades `Morador`, `Endereco`, `Material`, `PontoColeta` e `Solicitacao` ficarão inicialmente em `entity/`, enquanto seus respectivos repositories ficarão em `repository/`.

### Nomes e tipos das classes

O nome do pacote e o sufixo da classe coincidem, exceto na entidade e no enum, que usam apenas o nome do conceito do domínio, sempre no singular:

| Pacote | Nome da classe | Exemplo | Tipo |
|--------|----------------|---------|------|
| `entity` | conceito, sem sufixo | `Morador` | classe |
| `enumeration` | conceito, sem sufixo | `SituacaoCadastro` | enum |
| `repository` | `...Repository` | `MoradorRepository` | interface |
| `service` | `...Service` | `MoradorService` | classe |
| `controller` | `...Controller` | `MoradorController` | classe |
| `mapper` | `...Mapper` | `MoradorMapper` | classe |
| `form` | `...Form`, com a ação quando há formulários distintos | `MoradorCriacaoForm`, `MoradorAtualizacaoForm` | `record` |
| `dto` | `...DTO` | `MoradorDTO` | `record` |
| `exception` | `...Exception` | `NaoEncontradoException` | classe |

Quando a criação e a atualização têm campos ou regras diferentes no contrato, cada uma tem o seu formulário, com a ação no nome (por exemplo, o ADR 0021 para o morador). Quando são iguais, um único `...Form` basta.

A entidade é uma classe comum, porque o JPA exige construtor vazio e atributos alteráveis. Forms e DTOs são `record`, por carregarem apenas dados, e não usam Lombok. O formato de erro (`RespostaErro`, `ErroCampo`) não segue o sufixo `DTO` por ser um caso à parte.

Os ids usam `Integer`, coerente com as colunas `INTEGER` da migração V1 e com o contrato OpenAPI (`int32`). Contagens e totais usam `long`, coerente com `int64` no contrato.

A biblioteca Lombok é utilizada somente nas entidades, para reduzir código repetitivo. Nelas, evitar `@Data`, pois seus métodos `equals`, `hashCode` e `toString` são inadequados para entidades JPA; usar `@Getter` e `@Setter`. Ao combinar `@Builder` com valor inicial de atributo, usar `@Builder.Default`.

A nomenclatura dos elementos específicos do domínio permanecerá em português, enquanto os nomes dos pacotes técnicos poderão seguir as convenções amplamente utilizadas no ecossistema Spring, em conformidade com a ADR 0002.

A estrutura poderá ser revista futuramente caso o crescimento da aplicação justifique uma organização por funcionalidade ou uma abordagem híbrida.

## Consequências

- **Positivas:** estrutura simples e previsível; facilidade para localizar classes por responsabilidade técnica; alinhamento com convenções comuns em projetos Spring Boot; menor complexidade inicial de organização; conversões entre modelos concentradas em um único lugar; entrada e saída da API separadas, como no contrato.
- **Negativas:** classes relacionadas a uma mesma funcionalidade ficarão distribuídas por diferentes pacotes; a localização de todas as partes de um caso de uso poderá exigir navegação entre várias camadas; os mappers manuais exigem atualização sempre que um campo de form, entidade ou DTO mudar; o uso do Lombok adiciona uma dependência de compilação.
- **Cuidados:** o mapper não deve conter regra de negócio (por exemplo, normalização de e-mail, verificação de duplicidade ou permissão do ator), que pertence ao service. Ao criar um enum novo, seguir a ADR 0012. Ao criar um formulário novo, conferir os campos e as validações com o schema correspondente no `openapi.yaml` (ADR 0017). Ao acrescentar ou renomear um pacote, atualizar este ADR no mesmo Pull Request.
- **Possibilidade futura:** a organização poderá evoluir para pacotes por funcionalidade ou módulos mais independentes se a complexidade e a necessidade de delimitação dos domínios justificarem a mudança. Se os mappers manuais se tornarem repetitivos, a biblioteca MapStruct pode substituí-los sem alterar os demais pacotes.