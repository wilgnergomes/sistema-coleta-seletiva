# 14. Datas de referência do período e fuso de negócio

**Data:** 2026-10-12

**Status:** Proposta

## Contexto

O RF15 pede quantidades recolhidas "por material e período", e o RF09 pede filtro de solicitações por período. A especificação não define qual data delimita o período. A tabela `sol_solicitacao` guarda `sol_dt_criacao`, `sol_dt_agendamento` e `sol_dt_conclusao`, todas `timestamptz` (RNF05), e não há coluna de última alteração: a data da última mudança só pode ser obtida em `his_historico`. O número de solicitações por situação não menciona período na especificação.

Foram consideradas as seguintes alternativas para a data: criação, que nunca muda; agendamento, que muda a cada reagendamento; conclusão, preenchida somente em CONCLUIDA (`ck_sol_conclusao`); e última alteração, que moveria a mesma solicitação de período a cada transição. Para o fuso: UTC, o fuso do servidor, ou um fuso de negócio configurável. Como o PostgreSQL guarda instantes, e não fusos, o fuso usado para interpretar e apresentar os limites precisa ser definido pela aplicação.

Esta decisão interpreta a especificação, e não decorre dela. Por isso permanece como proposta até a confirmação.

## Decisão

1. **Quantidades recolhidas (RF15):** o período usa `sol_dt_conclusao`, por tratar-se da data em que o recolhimento foi concluído e por ser preenchida apenas em atendimentos CONCLUIDA.
2. **Solicitações por situação (RF15), quando houver filtro de período, e filtro de período do RF09:** usam `sol_dt_criacao`, por ser a única data que não muda com as transições.
3. **Fuso de negócio:** `America/Sao_Paulo`, definido em propriedade de configuração, e não fixo no código. As datas continuam armazenadas com fuso ou offset (RNF05) e as respostas da API usam formato ISO-8601 com offset.
4. **Limites do período:** intervalo semiaberto. O início é o começo do dia da data inicial, no fuso de negócio (inclusivo), e o fim é o começo do dia seguinte à data final (exclusivo).
5. Enquanto o status for Proposta, a implementação deve isolar a escolha da data e do fuso em um único ponto, para que a mudança, se houver, não altere o esquema do banco.

## Consequências

- **Positivas:** o significado de cada indicador fica explícito e previsível; o filtro do RF09 não muda de resultado quando a solicitação transita; o intervalo semiaberto evita dupla contagem nas bordas do período.
- **Negativas:** os dois indicadores do RF15 usam datas diferentes, então uma solicitação criada em um mês e concluída em outro entra em meses distintos em cada indicador; filtrar o RF09 por criação não encontra solicitações pela data de conclusão.
- **Cuidados:** os testes devem cobrir as bordas: uma conclusão às 23h59 do último dia entra no período, uma às 00h00 do dia seguinte não entra, e um instante que em UTC cai no dia seguinte, mas no fuso de negócio cai no dia anterior, é contado no dia do fuso de negócio. Os campos `sol_dt_criacao` e `sol_dt_conclusao` podem precisar de índices para o RNF04, o que deve ser avaliado com o dataset de desempenho. Se a confirmação trouxer outra data ou outro fuso, atualizar este ADR e a seção "Decisões a confirmar" da matriz de rastreabilidade, passando o status para Aceita, ou criar um novo ADR que substitua este.
