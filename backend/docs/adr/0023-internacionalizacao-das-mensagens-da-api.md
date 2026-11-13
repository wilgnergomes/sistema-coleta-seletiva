# 23. Internacionalização das mensagens da API (português do Brasil e inglês)

**Data:** 2026-11-07

**Status:** Aceita

## Contexto

Hoje as mensagens que a API devolve ao cliente estão escritas em português, direto no código: nos formulários (`@NotBlank(message = "o nome é obrigatório")`), nas exceções lançadas pelo service e pela leitura do ator (`new AcessoNegadoException("...")`, `new ConflitoRegraException("...")`) e no tratador global (`"Dados inválidos."`). As quatro exceções de negócio recebem o texto já pronto no construtor. Isso impede atender clientes em outro idioma e espalha o texto pelo código, o que também dificulta padronizar as mensagens (Issue #30).

A Issue #35 pede o suporte a **português do Brasil (padrão) e inglês**, identificado pelo cabeçalho `Accept-Language`, sem alterar o formato de erro do ADR 0018 (`{ codigo, mensagem, erros }`). As mensagens devem entrar de forma progressiva, a cada funcionalidade nova, e não todas de uma vez.

Alternativas consideradas:

1. **`MessageSource` do Spring com chaves; as exceções carregam a chave e os argumentos; o tratador global resolve o texto no idioma da requisição.** Segue o padrão do Spring, mantém service e forms sem conhecer o idioma, integra com o Bean Validation e permite acrescentar um idioma só com um novo arquivo.
2. **Resolver a mensagem já no service, com o `MessageSource` injetado.** A mensagem nasce pronta, mas o service passa a depender do idioma da requisição e da infraestrutura de mensagens, o que mistura regra de negócio com apresentação.
3. **Manter só português e deixar a tradução com o cliente.** Não exige mudança agora, mas não atende ao pedido de pt-BR e inglês e faz a API de demonstração deixar de ser autoexplicativa em inglês.
4. **Devolver apenas o `codigo` e deixar a mensagem a cargo do cliente.** É simples, mas muda o contrato do ADR 0018 (o campo `mensagem` deixaria de existir).
5. **Escolher o idioma por parâmetro de URL (`?lang=en`) ou cookie.** Funciona, mas cria um parâmetro em todos os endpoints. O `Accept-Language` é o mecanismo padrão do HTTP e dispensa mudar as rotas.

Escolheu-se a primeira, porque mantém cada camada com a sua responsabilidade (o service diz *o que* aconteceu, o tratador diz *como* contar isso ao cliente) e usa o recurso nativo do Spring.

## Decisão

1. **Idiomas:** português do Brasil (`pt-BR`, padrão) e inglês (`en`). Um idioma não suportado cai no padrão.
2. **Identificação do idioma:** pelo cabeçalho `Accept-Language`, com um `AcceptHeaderLocaleResolver` configurado com `pt-BR` como idioma padrão e `pt-BR` e `en` como suportados. Sem o cabeçalho, a resposta sai em `pt-BR`.
3. **`MessageSource` e arquivos de mensagens:** as mensagens ficam em `src/main/resources/messages.properties` (`pt-BR`, também usado como padrão) e `messages_en.properties`, ambos em UTF-8. O mesmo `MessageSource` alimenta o Bean Validation, de modo que os formulários referenciam chaves, por exemplo `message = "{morador.nome.obrigatorio}"`. A configuração fica em `config/` (ADR 0020).
4. **Chaves padronizadas**, em minúsculas e com hífen, iguais nos dois arquivos:
   - validação de campo: `<recurso>.<campo>.<regra>`, por exemplo `morador.nome.obrigatorio` e `morador.email.invalido`;
   - regra de negócio, acesso e recurso ausente: `erro.<recurso>.<situacao>`, por exemplo `erro.morador.email-repetido` e `erro.morador.nao-encontrado`;
   - mensagens genéricas do tratador global: `erro.geral.<situacao>`, por exemplo `erro.geral.dados-invalidos` e `erro.geral.erro-interno`.
5. **Exceções de negócio:** `AcessoNegadoException`, `ConflitoRegraException`, `ConflitoVersaoException` e `NaoEncontradoException` passam a carregar uma **chave** e **argumentos opcionais**, em vez de um texto pronto. Somente o `TratadorGlobalExceptions` converte a chave em mensagem, usando o `MessageSource` e o idioma da requisição. Textos de log não são internacionalizados.
6. **O contrato de erro não muda:** os valores de `codigo`, os status HTTP e a estrutura `{ codigo, mensagem, erros: [{ campo, mensagem }] }` permanecem como no ADR 0018. Só os textos de `mensagem` mudam de idioma. Os nomes de campo e os valores de enumeração (`ATIVO`, `INATIVO`) não são traduzidos.
7. **Aplicação progressiva:** esta decisão é implementada na Issue #35 apenas com a infraestrutura e as mensagens do RF01. Cada funcionalidade seguinte acrescenta as suas chaves, nos dois idiomas, na própria branch, sem reabrir a Issue #35.

## Consequências

- **Positivas:** o idioma da resposta passa a depender só do cabeçalho; o texto sai do código e fica em dois arquivos fáceis de revisar (o que também resolve a padronização de mensagens da Issue #30); service e formulários não conhecem idioma; incluir um terceiro idioma exige apenas um arquivo novo.
- **Negativas:** as quatro exceções mudam de construtor, o que obriga a ajustar onde são lançadas (service e leitura do ator); toda funcionalidade nova passa a exigir chaves nos dois arquivos; uma chave esquecida aparece para o cliente como a própria chave ou em português, conforme o caso.
- **Cuidados:** o `Accept-Language` precisa ser documentado no `openapi.yaml` e na matriz de rastreabilidade no mesmo PR (ADR 0017). Recomenda-se um teste que confira que `messages.properties` e `messages_en.properties` têm o mesmo conjunto de chaves, para impedir chaves esquecidas. Mensagens com valores (por exemplo, o tamanho máximo ou os valores aceitos de uma enumeração) usam argumentos nomeados por posição (`{0}`), e os testes cobrem o idioma padrão, o inglês e o cabeçalho ausente. Se a API precisar de um terceiro idioma, basta um novo arquivo e a inclusão do idioma na lista de suportados; se o idioma passar a depender do usuário cadastrado, e não da requisição, este ADR deve ser revisto.
