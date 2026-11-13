/// Enums estáveis da API (ADR 0012), com valor desconhecido para tolerância.
library;

enum SituacaoSolicitacao {
  solicitada('SOLICITADA', 'Solicitada'),
  agendada('AGENDADA', 'Agendada'),
  emRecolhimento('EM_RECOLHIMENTO', 'Em recolhimento'),
  concluida('CONCLUIDA', 'Concluída'),
  recusada('RECUSADA', 'Recusada'),
  cancelada('CANCELADA', 'Cancelada'),

  /// Valor que esta versão do app não conhece. Não habilita ações.
  desconhecida('', 'Situação indisponível');

  const SituacaoSolicitacao(this.api, this.rotulo);

  final String api;
  final String rotulo;

  static const conhecidas = [solicitada, agendada, emRecolhimento, concluida, recusada, cancelada];

  static SituacaoSolicitacao deApi(String? valor) =>
      conhecidas.firstWhere((s) => s.api == valor, orElse: () => desconhecida);

  bool get finalizada => this == concluida || this == recusada || this == cancelada;
}

enum SituacaoCadastro {
  ativo('ATIVO', 'Ativo'),
  inativo('INATIVO', 'Inativo'),
  desconhecida('', 'Situação indisponível');

  const SituacaoCadastro(this.api, this.rotulo);

  final String api;
  final String rotulo;

  static SituacaoCadastro deApi(String? valor) => switch (valor) {
    'ATIVO' => ativo,
    'INATIVO' => inativo,
    _ => desconhecida,
  };
}

enum Unidade {
  kg('KG', 'kg', 'quilogramas'),
  litro('L', 'L', 'litros'),
  unidade('UN', 'un.', 'unidades'),
  desconhecida('', '', 'unidade indisponível');

  const Unidade(this.api, this.simbolo, this.extenso);

  final String api;
  final String simbolo;
  final String extenso;

  static const conhecidas = [kg, litro, unidade];

  static Unidade deApi(String? valor) => conhecidas.firstWhere((u) => u.api == valor, orElse: () => desconhecida);

  /// Peças só aceitam quantidades inteiras (RN02).
  bool get inteira => this == unidade;
}

enum TipoResponsavel {
  morador('MORADOR', 'Morador'),
  operador('OPERADOR', 'Operador'),
  desconhecido('', 'Responsável');

  const TipoResponsavel(this.api, this.rotulo);

  final String api;
  final String rotulo;

  static TipoResponsavel deApi(String? valor) => switch (valor) {
    'MORADOR' => morador,
    'OPERADOR' => operador,
    _ => desconhecido,
  };
}
