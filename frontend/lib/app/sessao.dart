import 'package:material_ui/material_ui.dart';

/// Perfis da identificação simulada (ADR 0019).
enum Perfil {
  morador('MORADOR', 'Morador', '/morador/inicio'),
  operador('OPERADOR', 'Operador', '/operador/fila'),
  administrador('ADMINISTRADOR', 'Administrador', '/administracao/resumo');

  const Perfil(this.api, this.rotulo, this.rotaInicial);

  final String api;
  final String rotulo;
  final String rotaInicial;

  bool get exigeId => this != administrador;

  /// Prefixo das rotas exclusivas do perfil.
  String get prefixo => switch (this) {
    morador => '/morador',
    operador => '/operador',
    administrador => '/administracao',
  };

  static Perfil? daRota(String caminho) {
    for (final p in values) {
      if (caminho == p.prefixo || caminho.startsWith('${p.prefixo}/')) return p;
    }
    return null;
  }
}

/// Ator selecionado na demonstração. Fica só em memória: recarregar a
/// página exige nova seleção.
class Ator {
  const Ator(this.perfil, this.id, {this.nome});

  final Perfil perfil;
  final int? id;
  final String? nome;

  String get descricao {
    final base = perfil.exigeId ? '${perfil.rotulo} #$id' : perfil.rotulo;
    return nome == null ? base : '$base · $nome';
  }

  Map<String, String> get cabecalhos => {'X-Perfil': perfil.api, if (perfil.exigeId) 'X-Usuario-Id': '$id'};
}

/// Fonte de sessão: expõe o ator ao roteador, às telas e ao cliente HTTP.
///
/// Uma futura autenticação real substitui esta classe e a política de
/// cabeçalhos, mantendo rotas, repositórios e telas.
class Sessao extends ChangeNotifier {
  Ator? _ator;
  int _geracao = 0;
  int? _enderecoSelecionadoId;
  final _aoTrocar = <VoidCallback>[];

  Ator? get ator => _ator;
  Perfil? get perfil => _ator?.perfil;
  int? get usuarioId => _ator?.id;

  /// Muda a cada troca de ator. Respostas de outra geração são descartadas.
  int get geracao => _geracao;

  /// Endereço que o morador escolheu como referência na descoberta.
  int? get enderecoSelecionadoId => _enderecoSelecionadoId;

  set enderecoSelecionadoId(int? id) {
    if (_enderecoSelecionadoId == id) return;
    _enderecoSelecionadoId = id;
    notifyListeners();
  }

  /// Registra limpeza de caches e rascunhos restritos na troca de ator.
  void aoTrocarAtor(VoidCallback acao) => _aoTrocar.add(acao);

  Map<String, String> cabecalhos() => _ator?.cabecalhos ?? const {};

  Object? _respostaValidacao;

  /// Seleciona o ator. [respostaValidacao] é a resposta da primeira chamada
  /// do perfil, reaproveitada uma vez pela tela inicial.
  void entrar(Ator ator, {Object? respostaValidacao}) {
    _trocar(ator);
    _respostaValidacao = respostaValidacao;
  }

  /// Entrega a resposta da validação uma única vez.
  Object? consumirRespostaValidacao() {
    final resposta = _respostaValidacao;
    _respostaValidacao = null;
    return resposta;
  }

  /// Atualiza o nome exibido sem trocar o contexto.
  void atualizarNome(String nome) {
    final atual = _ator;
    if (atual == null) return;
    _ator = Ator(atual.perfil, atual.id, nome: nome);
    notifyListeners();
  }

  void sair() => _trocar(null);

  void _trocar(Ator? novo) {
    _ator = novo;
    _geracao++;
    _enderecoSelecionadoId = null;
    _respostaValidacao = null;
    for (final acao in _aoTrocar) {
      acao();
    }
    notifyListeners();
  }
}
