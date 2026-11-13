/// Falhas de comunicação com a API, já traduzidas para a interface.
///
/// A interface nunca recebe HTML, corpo bruto ou rastreamento de pilha
/// (RNF03). A decisão por tipo de erro usa `status` e `codigo`, nunca o texto.
library;

class ErroCampo {
  const ErroCampo({required this.campo, required this.mensagem});

  /// Caminho do campo enviado pelo servidor, como `endereco.cep` ou
  /// `itens[0].quantidadeEstimada`. Pode ser nulo em enum inválido.
  final String? campo;
  final String mensagem;
}

sealed class FalhaApi implements Exception {
  const FalhaApi(this.mensagem);

  /// Mensagem adequada para exibir ao usuário.
  final String mensagem;

  @override
  String toString() => '$runtimeType: $mensagem';
}

/// Resposta HTTP com status de erro.
class ErroHttp extends FalhaApi {
  const ErroHttp({required this.status, required this.codigo, required String mensagem, this.erros = const []})
    : super(mensagem);

  final int status;

  /// Código estável do backend: `VALIDACAO`, `VALOR_INVALIDO`,
  /// `ACESSO_NEGADO`, `NAO_ENCONTRADO`, `CONFLITO_REGRA`, `CONFLITO_VERSAO`
  /// ou `ERRO_INTERNO`. Nulo quando a resposta não trouxe o corpo padrão.
  final String? codigo;
  final List<ErroCampo> erros;

  bool get validacao => status == 400;
  bool get acessoNegado => status == 403;
  bool get naoEncontrado => status == 404;
  bool get conflito => status == 409;
  bool get conflitoVersao => status == 409 && codigo == 'CONFLITO_VERSAO';
  bool get conflitoRegra => status == 409 && codigo != 'CONFLITO_VERSAO';

  /// `405` e `415` indicam incompatibilidade de método ou conteúdo, e não
  /// um campo que o usuário possa corrigir.
  bool get incompatibilidade => status == 405 || status == 415;

  /// Erros associados a um campo, indexados pelo caminho.
  Map<String, String> get errosPorCampo => {
    for (final erro in erros)
      if (erro.campo != null && erro.campo!.isNotEmpty) erro.campo!: erro.mensagem,
  };
}

/// Sem resposta do servidor: rede indisponível, CORS ou conexão recusada.
class FalhaRede extends FalhaApi {
  const FalhaRede({required this.gravacao})
    : super(
        gravacao
            ? 'Não foi possível confirmar a operação por falta de conexão. '
                  'Consulte o registro antes de tentar de novo.'
            : 'Não foi possível conectar ao servidor. Verifique a conexão e tente novamente.',
      );

  /// Em gravação, o resultado é incerto: o servidor pode ter gravado.
  final bool gravacao;
}

/// O prazo do cliente terminou antes da resposta.
class FalhaPrazo extends FalhaApi {
  const FalhaPrazo({required this.gravacao})
    : super(
        gravacao
            ? 'O servidor demorou a responder e o resultado é incerto. '
                  'Consulte o registro antes de tentar de novo.'
            : 'O servidor demorou a responder. Tente novamente.',
      );

  final bool gravacao;
}

/// Resposta de sucesso com formato inesperado.
class RespostaInvalida extends FalhaApi {
  const RespostaInvalida([String? detalhe])
    : detalhe = detalhe ?? '',
      super('O servidor enviou uma resposta que o aplicativo não reconhece.');

  /// Detalhe técnico para testes e diagnóstico. Não é exibido.
  final String detalhe;
}
