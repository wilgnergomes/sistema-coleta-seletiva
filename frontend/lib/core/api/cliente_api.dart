import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'falhas.dart';
import 'json.dart';

/// Como a chamada se identifica para a API (ADR 0019).
enum Acesso {
  /// Sem cabeçalhos de identidade. Catálogo público e cadastro de morador.
  publico,

  /// Com os cabeçalhos do ator selecionado na sessão.
  ator,
}

/// Fornece os cabeçalhos de identidade no momento da chamada.
///
/// A fonte demo devolve `X-Perfil` e `X-Usuario-Id`. Uma futura
/// autenticação real troca apenas esta função.
typedef FonteIdentidade = Map<String, String> Function();

/// Cliente HTTP comum: monta a URL, aplica cabeçalhos, prazo, status e
/// decodificação. Repositórios usam este cliente e devolvem modelos tipados.
class ClienteApi {
  ClienteApi({
    required this.base,
    required this._http,
    FonteIdentidade? identidade,
    this.prazoLeitura = const Duration(seconds: 10),
    this.prazoGravacao = const Duration(seconds: 15),
  }) : _identidade = identidade ?? _semIdentidade;

  static Map<String, String> _semIdentidade() => const {};

  /// Base já resolvida, por exemplo `http://127.0.0.1:8000/api`.
  final Uri base;
  final http.Client _http;
  final FonteIdentidade _identidade;
  final Duration prazoLeitura;
  final Duration prazoGravacao;

  /// Monta a URL omitindo parâmetros nulos ou em branco.
  Uri url(String caminho, [Map<String, Object?> query = const {}]) {
    final parametros = <String, String>{
      for (final MapEntry(:key, :value) in query.entries)
        if (value != null && value.toString().trim().isNotEmpty) key: value.toString().trim(),
    };
    return base.replace(path: '${base.path}$caminho', queryParameters: parametros.isEmpty ? null : parametros);
  }

  Map<String, String> _cabecalhos(Acesso acesso, {required bool comCorpo, Map<String, String>? identidade}) => {
    'Accept': 'application/json',
    'Accept-Language': 'pt-BR',
    if (comCorpo) 'Content-Type': 'application/json',
    if (acesso == Acesso.ator) ...(identidade ?? _identidade()),
  };

  /// [identidade] substitui a identidade da sessão nesta chamada. É usada
  /// para validar um ator antes de selecioná-lo.
  Future<Object?> get(
    String caminho, {
    Map<String, Object?> query = const {},
    Acesso acesso = Acesso.publico,
    Map<String, String>? identidade,
  }) => _executar(
    () => _http.get(url(caminho, query), headers: _cabecalhos(acesso, comCorpo: false, identidade: identidade)),
    gravacao: false,
  );

  Future<Object?> post(String caminho, {Object? corpo, Acesso acesso = Acesso.ator}) => _executar(
    () => _http.post(url(caminho), headers: _cabecalhos(acesso, comCorpo: true), body: jsonEncode(corpo)),
    gravacao: true,
  );

  Future<Object?> put(String caminho, {Object? corpo, Acesso acesso = Acesso.ator}) => _executar(
    () => _http.put(url(caminho), headers: _cabecalhos(acesso, comCorpo: true), body: jsonEncode(corpo)),
    gravacao: true,
  );

  Future<void> delete(String caminho, {Acesso acesso = Acesso.ator}) =>
      _executar(() => _http.delete(url(caminho), headers: _cabecalhos(acesso, comCorpo: false)), gravacao: true);

  Future<Object?> _executar(Future<http.Response> Function() enviar, {required bool gravacao}) async {
    final http.Response resposta;
    try {
      resposta = await enviar().timeout(gravacao ? prazoGravacao : prazoLeitura);
    } on TimeoutException {
      throw FalhaPrazo(gravacao: gravacao);
    } on http.ClientException {
      throw FalhaRede(gravacao: gravacao);
    }
    return interpretar(resposta);
  }

  /// Converte a resposta em JSON ou em [FalhaApi].
  ///
  /// O corpo é lido como UTF-8 a partir dos bytes: o Spring envia
  /// `application/json` sem `charset`, e o padrão do pacote `http` nesse caso
  /// seria Latin-1, o que corromperia os acentos.
  static Object? interpretar(http.Response resposta) {
    final status = resposta.statusCode;
    final texto = resposta.bodyBytes.isEmpty ? '' : utf8.decode(resposta.bodyBytes, allowMalformed: true);
    final ehJson = (resposta.headers['content-type'] ?? '').contains('json');

    if (status >= 200 && status < 300) {
      if (status == 204 || texto.trim().isEmpty) return null;
      if (!ehJson) throw const RespostaInvalida('sucesso sem JSON');
      try {
        return jsonDecode(texto);
      } on FormatException {
        throw const RespostaInvalida('JSON malformado');
      }
    }
    throw _erro(status, ehJson ? texto : '');
  }

  static ErroHttp _erro(int status, String texto) {
    if (texto.isNotEmpty) {
      try {
        final corpo = comoMapa(jsonDecode(texto));
        final mensagem = corpo['mensagem'];
        final codigo = corpo['codigo'];
        final erros = corpo['erros'];
        if (mensagem is String && mensagem.trim().isNotEmpty) {
          return ErroHttp(
            status: status,
            codigo: codigo is String ? codigo : null,
            mensagem: mensagem,
            erros: [
              if (erros is List)
                for (final erro in erros.whereType<Map<Object?, Object?>>())
                  ErroCampo(
                    campo: erro['campo'] is String ? erro['campo'] as String : null,
                    mensagem: erro['mensagem'] is String ? erro['mensagem'] as String : 'Valor inválido.',
                  ),
            ],
          );
        }
      } on Object {
        // Corpo fora do padrão: cai na mensagem genérica abaixo.
      }
    }
    return ErroHttp(status: status, codigo: null, mensagem: mensagemPadrao(status));
  }

  static String mensagemPadrao(int status) => switch (status) {
    400 => 'Os dados enviados não foram aceitos. Revise os campos.',
    403 => 'Esta operação não está disponível para a identidade selecionada.',
    404 => 'O registro não foi encontrado ou não está disponível.',
    405 || 415 => 'O aplicativo e o servidor não estão compatíveis para esta operação.',
    409 => 'A operação entrou em conflito com o estado atual do registro.',
    >= 500 => 'O servidor não conseguiu concluir a operação. Tente novamente em instantes.',
    _ => 'O servidor respondeu de forma inesperada (código $status).',
  };

  void fechar() => _http.close();
}
