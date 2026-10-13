# 19. Identificação do ator no protótipo

**Data:** 2026-10-13

**Status:** Aceita

## Contexto

A especificação (seção 3) permite que o protótipo use atores fictícios e uma seleção de usuário, e diz que essa seleção não é autenticação nem deve ser usada em produção. Várias regras dependem de saber quem age: o responsável do histórico vem do ator (ADR 0011), o operador só atua em solicitações do seu ponto (RF07 e ADR 0010), a consulta de solicitações respeita o contexto do ator (RF09) e as consultas públicas não podem devolver dados pessoais (RNF06). O contrato precisa dizer como o ator chega à API.

Foram consideradas as seguintes alternativas: login com Spring Security e JWT, que é uma extensão opcional (seção 12) e só deve ser iniciada depois que o escopo obrigatório funcionar; o identificador do ator em parâmetro de consulta, que aparece em logs e se confunde com os filtros; o identificador no corpo da requisição, que contraria o ADR 0011, pois o responsável nunca é informado pelo cliente; e cabeçalhos HTTP próprios. Escolheram-se os cabeçalhos, por não misturarem o ator com os dados da operação e por serem simples de usar no Swagger e no front.

## Decisão

1. O ator é informado por dois cabeçalhos: `X-Perfil`, com `MORADOR`, `OPERADOR` ou `ADMINISTRADOR`, e `X-Usuario-Id`, com o id do morador ou do operador. O administrador dispensa o id.
2. Isso é uma seleção de usuário para demonstração. Não valida identidade e não deve ser usado em produção.
3. O serviço confere no banco que o usuário existe e está ATIVO. O morador cria e consulta apenas as próprias solicitações e endereços. O operador atua apenas em solicitações do ponto ao qual está vinculado, e esse ponto é lido do banco, nunca do corpo da requisição (ADR 0010). O administrador gerencia os cadastros e consulta todos os registros.
4. O responsável de cada registro de histórico é determinado a partir do ator (ADR 0011).
5. Cabeçalho ausente, usuário inexistente ou inativo, ou perfil sem permissão para a operação resultam em 403 `ACESSO_NEGADO`.
6. Funcionam sem cabeçalhos: o cadastro de morador (`POST /moradores`) e as consultas públicas de pontos e materiais. Sem ator, ou como morador ou operador, essas consultas devolvem apenas registros ATIVOS e nenhum dado pessoal. Como administrador, aceitam o filtro `situacao` e mostram também os inativos.
7. Se um login for implementado, este ADR será substituído por outro, e o backend passará a validar identidade, permissões e vínculo com o ponto em cada operação restrita.

## Consequências

- **Positivas:** o ator fica explícito em cada requisição e nos testes; o mesmo endpoint serve ao público e ao administrador, sem caminhos duplicados; o contrato e o Swagger já documentam os cabeçalhos.
- **Negativas:** qualquer cliente pode se passar por qualquer ator, então não há segurança real; o front precisa enviar os dois cabeçalhos em cada chamada.
- **Cuidados:** as permissões são verificadas no serviço, e nunca por um valor de morador ou de ponto vindo do corpo; os testes de permissão exigidos pelo RNF06 e RNF08 devem existir (morador tentando agendar, operador de outro ponto tentando concluir); o seed e as fixtures precisam de moradores e operadores conhecidos para usar como atores; se o Spring Security entrar, será preciso liberar as rotas públicas e a documentação.