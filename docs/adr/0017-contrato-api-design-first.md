# 17. Contrato da API escrito antes do código

**Data:** 2026-10-13

**Status:** Aceita

## Contexto

O RNF07 exige documentar endpoints, exemplos e respostas por OpenAPI ou coleção Postman, e o planejamento prevê definir o contrato da API antes de elaborar o front. Era preciso decidir de onde vem o contrato: se é escrito antes e guia o código, ou se é gerado depois a partir dele.

Foram consideradas as seguintes alternativas: coleção Postman, que é rápida de montar, mas mistura documentação com testes manuais e não descreve os formatos de dados de forma reutilizável; contrato gerado pelo springdoc a partir dos controllers (code-first), que dá menos trabalho, mas só existe depois do código e muda toda vez que o código muda; e contrato escrito à mão em OpenAPI antes do código (design-first). Escolheu-se a última porque o front e a matriz de rastreabilidade precisam dos endpoints antes dos controllers, e porque revisar o contrato em um Pull Request é mais barato do que revisar o código.

## Decisão

1. O contrato é o arquivo `docs/api/openapi.yaml`, em OpenAPI 3.0.3, e é a fonte da verdade da API.
2. Ele descreve os endpoints dos RF01 a RF15, os formatos de entrada e saída, as respostas de erro e exemplos ilustrativos. Os exemplos não são dados de seed nem testes.
3. A partir do Ciclo 1, o springdoc gera a documentação a partir do código apenas para conferência. Ao fechar cada ciclo, o gerado é comparado com o `openapi.yaml`. O JSON gerado não é versionado.
4. Alterar um endpoint exige alterar o `openapi.yaml` no mesmo Pull Request, junto com a coluna "Endpoints" da matriz de rastreabilidade.
5. Na matriz, o endpoint começa como *pendente*, passa a *previsto* quando consta do contrato e a *executado* quando o teste correspondente passa.

## Consequências

- **Positivas:** front e back trabalham contra o mesmo documento; os endpoints podem ser revisados antes de existir código; a matriz de rastreabilidade ganha a lista de endpoints desde o início; o RNF07 é atendido por um arquivo versionado.
- **Negativas:** o contrato e o código podem divergir se ninguém conferir; o arquivo é longo e difícil de revisar linha a linha; a escolha da versão do springdoc fica para o Ciclo 1.
- **Cuidados:** o contrato não pode conter nada que não decorra de um RF, RN, RNF ou ADR; em caso de divergência entre contrato e código, vale o contrato até que um ADR o altere; a leitura deve ser feita pela pré-visualização (Swagger UI ou editor), e não pelo texto; a versão do springdoc deve ser compatível com o Spring Boot do projeto, e isso é verificado ao adicioná-lo no Ciclo 1.