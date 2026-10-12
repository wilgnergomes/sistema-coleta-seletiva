# 4. Separação do Diagrama Entidade-Relacionamento em Múltiplas Visões

**Data:** 2026-10-11

**Status:** Aceita

## Contexto

Com a consolidação das regras de negócio do sistema de Coleta Seletiva, o modelo físico de dados cresceu, englobando múltiplas entidades, chaves estrangeiras complexas e regras de auditoria/histórico. É necessário definir a melhor estratégia para representar esse modelo visualmente no dbdiagram.io.

Foram consideradas as seguintes alternativas: manter o diagrama como um monolito, reunindo todas as tabelas e relacionamentos do sistema em uma única imagem, o que geraria uma representação completa, porém com sobrecarga visual e cruzamentos inevitáveis de linhas; e a divisão por visões, segmentando o diagrama em contextos específicos (Domínios de Negócio). Acabou-se escolhendo a divisão por visão por conta da drástica redução da carga cognitiva, melhor legibilidade estrutural e facilidade na apresentação do projeto para avaliação.

## Decisão

O diagrama Entidade-Relacionamento será dividido em duas visões separadas, focadas em domínios específicos do sistema:

1. **Visão Estrutural (Cadastros e Infraestrutura):** Focada nas entidades base do sistema (Moradores, Operadores, Materiais, Pontos de Coleta e suas intersecções).
2. **Visão Transacional (Fluxo de Recolhimento):** Focada no evento principal e na rastreabilidade (Solicitações, Itens de Solicitação e Histórico), utilizando as tabelas base apenas como referências simplificadas (stubs).

Ambas as visões terão seus próprios arquivos-fonte na linguagem DBML (ex.: `er-cadastros.dbml` e `er-transacional.dbml`), além de suas respectivas imagens exportadas, armazenadas no diretório `docs/diagramas/` do repositório.

## Consequências

- **Positivas:** redução da sobrecarga visual para entender o sistema; eliminação do cruzamento excessivo de linhas (grafo mais limpo); facilidade para apresentar, documentar e discutir o projeto em partes menores; maior flexibilidade para escalar a documentação se novos módulos (como faturamento ou rotas) forem adicionados no futuro.
- **Negativas:** exigência de manter duas fontes de documentação (dois arquivos `.dbml` e duas imagens separadas) em vez de apenas uma; necessidade de navegar entre duas imagens para ter a visão global de todo o banco de dados.
- **Cuidados:** será necessário ter atenção ao atualizar tabelas que aparecem como "referência" (stubs) na Visão Transacional. Se uma tabela estrutural (como `mor_morador`) sofrer mudança em sua Chave Primária, essa alteração deverá ser refletida manualmente em ambos os arquivos `.dbml` para manter a consistência da documentação.