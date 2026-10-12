# 3. Ferramenta usada para o diagrama ER

**Data:** 2026-10-11

**Status:** Aceita

## Contexto

É necessário escolher uma ferramenta para criar e manter o diagrama Entidade-Relacionamento (ER) do banco de dados, considerando a facilidade de edição, a visualização dos relacionamentos e a possibilidade de versionar o modelo junto ao código-fonte do projeto.

Foram consideradas as seguintes alternativas: Mermaid, que permite definir diagramas por texto, mas oferece menos controle sobre o posicionamento dos elementos; ERD Tool do pgAdmin, que permite criar diagramas visualmente, mas não utiliza um arquivo textual como fonte principal do modelo; e draw.io, que exige a edição visual manual dos elementos. O dbdiagram.io foi considerado mais adequado por permitir definir a estrutura do banco por código e gerar o diagrama automaticamente.

## Decisão

Será utilizado o **dbdiagram.io**, uma ferramenta online que permite definir a estrutura do banco de dados por meio da linguagem DBML (*Database Markup Language*) e gerar automaticamente o diagrama ER.

O arquivo-fonte `er.dbml` será armazenado no diretório `docs/diagramas/` do repositório GitHub e será a fonte oficial do modelo do banco de dados. As alterações serão realizadas no dbdiagram.io e, em seguida, atualizadas no arquivo do repositório. A imagem exportada `er.png` também será mantida no mesmo diretório para consulta e documentação.

As atualizações do modelo e da imagem deverão ser enviadas ao repositório por meio de Pull Requests, permitindo revisar e acompanhar as alterações realizadas.

## Consequências

- **Positivas:** geração automática do diagrama a partir do código; facilidade para editar tabelas e relacionamentos; versionamento do modelo pelo Git; possibilidade de revisar alterações por Pull Requests; exportação do diagrama para formatos como PNG e PDF.
- **Negativas:** dependência de uma ferramenta online para a edição visual; necessidade de aprender a sintaxe DBML; limite de 10 diagramas no plano gratuito, o que exige gerenciar os diagramas mantidos na plataforma.
- **Cuidados:** como a sincronização com o repositório não é automática, será necessário atualizar o arquivo `er.dbml` e a imagem `er.png` sempre que o modelo for alterado. O diagrama documenta a estrutura planejada, mas não substitui os scripts SQL ou as migrações responsáveis por implementar as alterações no banco de dados.