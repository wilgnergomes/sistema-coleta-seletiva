# 18. Convenções da API

**Data:** 2026-10-13

**Status:** Aceita

## Contexto

A especificação define regras gerais para a API: JSON e métodos HTTP adequados, códigos 201, 400, 404 e 409, tratamento global de erros com mensagem, código e erros de campos, paginação com limite de 100 e datas com fuso ou offset (RNF03, RNF04 e RNF05). Ela não define os nomes dos caminhos, o formato exato do erro, como a versão da solicitação trafega nem como as mudanças de situação são expostas. Sem isso, cada endpoint seria decidido isoladamente.

Foram consideradas as seguintes alternativas: para a mudança de situação, um `PATCH` do campo de situação, que concentra tudo em um endpoint, mas esconde regras, campos e histórico diferentes de cada transição, e ações `POST` sobre a solicitação, uma para cada transição; para a versão, um cabeçalho `If-Match` com `ETag`, que é mais próximo do padrão HTTP, e a versão no corpo da requisição, mais simples para o front e para a documentação; para a paginação, devolver o `Page` do Spring, cujo formato JSON não é um contrato estável, e um DTO próprio; para inativar cadastros, um `PATCH` de situação e um campo no `PUT`. Escolheram-se as ações `POST`, a versão no corpo, o DTO próprio e o campo no `PUT`, por serem as opções mais simples de documentar e de testar.

## Decisão

1. Caminhos e campos usam português (ADR 0002), com recursos no plural (`/moradores`, `/pontos-coleta`) e JSON como formato de troca.
2. Criação devolve 201 com o cabeçalho `Location`; consulta e atualização devolvem 200; remoção de vínculo devolve 204.
3. Cadastros usam `POST` para criar, `GET` para consultar e `PUT` para atualizar, com a representação completa. A inativação é feita pelo campo `situacao` do `PUT` (RF03 e RF04, e o mesmo para moradores e operadores). Não há exclusão física de cadastros (RN03).
4. As mudanças de situação da solicitação são ações `POST` sobre `/solicitacoes/{id}`: `agendamento`, `recusa`, `cancelamento`, `inicio`, `conclusao`, `reagendamento` e `falha`. Cada uma aplica a regra da sua transição (RN06) e grava um registro de histórico.
5. A `versao` lida pelo cliente vai no corpo de toda ação e da correção de itens e endereço. A resposta devolve a solicitação com a nova versão. Versão diferente da atual ou bloqueio otimista resulta em 409 `CONFLITO_VERSAO` (ADR 0009).
6. O erro tem sempre o formato `{ codigo, mensagem, erros: [{ campo, mensagem }] }`, sem stack trace, com este catálogo:

   | codigo | HTTP | Quando |
      |--------|------|--------|
   | `VALIDACAO` | 400 | Bean Validation falhou ou JSON malformado |
   | `VALOR_INVALIDO` | 400 | Valor de enum desconhecido, com os valores aceitos (ADR 0012) |
   | `ACESSO_NEGADO` | 403 | O ator não pode executar a operação (ADR 0019) |
   | `NAO_ENCONTRADO` | 404 | Registro ausente |
   | `CONFLITO_REGRA` | 409 | Regra de negócio violada (transição, compatibilidade, ADR 0013) |
   | `CONFLITO_VERSAO` | 409 | Versão desatualizada ou bloqueio otimista |
   | `ERRO_INTERNO` | 500 | Erro inesperado, com mensagem genérica |

7. O código 403 é um acréscimo à lista do RNF03. Ele é necessário para o RF07, que exige impedir que o operador atue em solicitações de outro ponto.
8. As listagens recebem `page` (a partir de 0) e `size` (padrão 20, máximo 100) e devolvem `{ conteudo, pagina, tamanho, totalElementos, totalPaginas }`, em um DTO próprio.
9. Datas de evento usam ISO-8601 com offset (RNF05). Os filtros de período `de` e `ate` são datas no formato `AAAA-MM-DD`, interpretadas conforme o ADR 0014.
10. Os valores de enum não têm acento e usam maiúsculas (ADR 0012), por exemplo `CONCLUIDA`.
11. Na conclusão (RF13), a data é registrada pelo servidor e o responsável vem do ator. O cliente envia apenas as quantidades efetivas.
12. A correção de itens e endereço antes do agendamento (RN04 e ADR 0008) é o `PUT /solicitacoes/{id}`, permitido somente em SOLICITADA.
13. O resumo gerencial (RF15) é um único `GET /resumo-gerencial`, com um bloco de solicitações por situação e outro de quantidades recolhidas.

## Consequências

- **Positivas:** o front sabe o formato de qualquer resposta e de qualquer erro; cada transição de situação tem endpoint, corpo e teste próprios; a paginação não depende do formato interno do Spring; o contrato fica coerente entre os 15 requisitos.
- **Negativas:** há vários endpoints `POST` de ação, em vez de um só de alteração de situação; o cliente precisa guardar e reenviar a `versao`; o 403 não consta da lista do RNF03.
- **Cuidados:** todo endpoint novo ou alterado entra no `openapi.yaml` e na matriz no mesmo Pull Request; o tratador global de exceções deve produzir exatamente o formato e o catálogo acima; os testes `[API]` devem conferir o formato do erro; acrescentar um valor de enum exige uma migração nova e o ajuste do contrato (ADR 0012); o ADR 0014 continua Proposta, e os filtros de período seguem o que ele definir.