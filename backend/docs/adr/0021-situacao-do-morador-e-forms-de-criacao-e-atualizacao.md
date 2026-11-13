# 21. Situação do morador: formulários de criação e atualização separados

**Data:** 2026-10-15

**Status:** Aceita; as decisões 1, 2 e 4 foram substituídas pelo ADR-0026

## Contexto

O RF01 permite cadastrar, consultar e atualizar moradores, e o ADR 0018 define que a inativação é feita pelo campo `situacao` do `PUT`. A especificação não diz quem pode inativar um morador: o administrador gerencia materiais, pontos e responsáveis (seção 3), mas moradores não aparecem nessa lista, e nada prevê que o morador desative a própria conta.

Além disso, o contrato usava o mesmo schema (`MoradorEntrada`) no `POST` e no `PUT`, o que permitia enviar `situacao` na criação sem utilidade, já que todo cadastro novo começa ATIVO.

Foram consideradas as seguintes alternativas: um formulário único com `situacao` opcional, com o serviço ignorando ou rejeitando o campo na criação, o que esconde a regra no código e deixa o contrato descrevendo um campo sem efeito; mappers diferentes por perfil (cliente e administrador), o que coloca permissão dentro do mapper, contrariando o ADR 0020, e mantém o `if` de permissão no serviço; e schemas separados para criação e atualização, com a permissão verificada no serviço. Escolheu-se a última, porque impede o campo por construção e deixa o contrato fiel ao comportamento.

## Decisão

1. A criação (`POST /moradores`) usa o schema `MoradorCriacao`, com `nome`, `email` e `fone`, sem `situacao`. Todo morador nasce ATIVO.
2. A atualização (`PUT /moradores/{id}`) usa o schema `MoradorEntrada`, com `situacao` opcional. Quando omitida, a situação atual é mantida, e o morador pode corrigir nome, e-mail e telefone sem reenviá-la.
3. Somente o administrador pode alterar a situação. Um morador que enviar `situacao` diferente da atual recebe 403 `ACESSO_NEGADO`. A verificação é feita no serviço, e não no formulário nem no mapper.
4. No código, `MoradorCriacaoForm` e `MoradorAtualizacaoForm` correspondem aos dois schemas. O `MoradorMapper` tem um método para criar e outro para atualizar, sem regra de permissão.
5. A normalização do e-mail (minúsculas, sem espaços nas pontas) e a verificação de duplicidade continuam no serviço (RF01). **Esclarecimento:** como o `@Email` valida o corpo antes de o serviço executar, os formulários de criação e atualização removem os espaços nas pontas do e-mail ao serem construídos, para que um e-mail como `" maria@exemplo.com "` não seja rejeitado com 400. O serviço continua responsável por colocar o e-mail em minúsculas, por normalizar os demais campos e por verificar a duplicidade.

## Consequências

- **Positivas:** o `POST` não aceita um campo sem efeito; o contrato reflete o comportamento; a regra de permissão fica explícita e testável; o morador edita seus dados sem se preocupar com a situação.
- **Negativas:** dois formulários e dois schemas no cadastro de morador; o morador não pode desativar a própria conta, e isso exige o administrador.
- **Cuidados:** a verificação de permissão depende da identificação do ator (ADR 0019), ainda por implementar. Até lá, o `PUT` aceita `situacao` sem checar o perfil, e isso deve constar como pendência na issue do RF01. Os testes devem cobrir morador tentando inativar (403), administrador inativando (200) e `PUT` sem `situacao` mantendo a situação atual. O mesmo padrão pode ser avaliado para material, ponto e operador, e, se for adotado, registrado em ADR próprio. Se um dia o morador puder desativar a própria conta, este ADR deve ser substituído por outro.

## Substituição parcial

Em 2026-11-10 as decisões 1, 2 e 4 (formulários e schemas separados) foram substituídas pelo [ADR 0026](0026-form-unico-de-morador-na-criacao-e-na-atualizacao.md), que unifica o cadastro em um único `MoradorForm`. Continuam valendo as decisões 3 e 5 e a regra de que todo morador nasce ATIVO. O texto acima foi preservado como registro histórico.
