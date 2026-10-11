# 1. Ferramenta de build do projeto

**Data:** 2026-10-11

**Status:** Aceita

## Contexto

É necessário escolher uma ferramenta de build (*build tool*) para automatizar tarefas relacionadas à compilação, ao gerenciamento de dependências e à execução de testes do projeto Java.

Foram consideradas duas alternativas: Maven e Gradle. Ambas permitem gerenciar dependências, configurar o processo de build e integrar testes automatizados. O Maven utiliza principalmente um arquivo de configuração XML (`pom.xml`), enquanto o Gradle permite configurações por meio de arquivos em Groovy ou Kotlin.

Como o Maven já foi utilizado durante as atividades acadêmicas e recomendado pelo professor, enquanto o conhecimento sobre Gradle ainda é limitado, prioriza-se neste momento a familiaridade com a ferramenta e a redução da complexidade inicial de configuração.

## Decisão

Será utilizado o **Apache Maven** como ferramenta de build do projeto. A configuração será mantida no arquivo `pom.xml`, responsável por declarar as dependências e configurar as tarefas necessárias ao desenvolvimento.

O projeto já utiliza o Maven Wrapper, representado pelos arquivos `mvnw`, `mvnw.cmd` e pela pasta `.mvn/wrapper/`, incluídos no repositório. Todos os comandos de build deverão utilizar o wrapper, garantindo que os integrantes da equipe e o ambiente de integração contínua utilizem a versão do Maven definida no projeto.

## Consequências

- **Positivas:** familiaridade com a ferramenta; alinhamento com a orientação recebida durante as atividades acadêmicas; configuração centralizada das dependências; versão do Maven definida no repositório; maior consistência na execução do build entre diferentes ambientes.
- **Negativas:** menor contato prático com o Gradle, que também é utilizado em projetos Java; necessidade de aprender a estrutura e as convenções do Maven para configurações mais avançadas; necessidade de ter um JDK compatível instalado e configurado em cada ambiente; necessidade de conexão com a internet na primeira execução do wrapper para baixar a distribuição do Maven, caso ela ainda não esteja disponível em cache.
- **Limitação:** o Maven Wrapper padroniza a versão do Maven, mas não instala nem fixa automaticamente a versão do JDK utilizada pelo projeto.
- **Possibilidade futura:** a escolha poderá ser reavaliada caso os requisitos do projeto mudem ou surja uma necessidade concreta que justifique a adoção de outra ferramenta.