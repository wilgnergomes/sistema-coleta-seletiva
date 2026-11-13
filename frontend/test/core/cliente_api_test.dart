import 'dart:async';
import 'dart:convert';

import 'package:coleta_seletiva_web/core/api/cliente_api.dart';
import 'package:coleta_seletiva_web/core/api/falhas.dart';
import 'package:coleta_seletiva_web/core/api/json.dart';
import 'package:coleta_seletiva_web/core/api/pagina.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

http.Response json(Object? corpo, int status) =>
    http.Response.bytes(utf8.encode(jsonEncode(corpo)), status, headers: {'content-type': 'application/json'});

void main() {
  final base = Uri.parse('http://127.0.0.1:8000/api');

  group('url', () {
    final api = ClienteApi(base: base, http: MockClient((_) async => http.Response('', 204)));

    test('acrescenta o caminho à base e omite parâmetros nulos ou em branco', () {
      final url = api.url('/pontos-coleta', {'nome': '  ', 'municipio': 'Lagarto', 'materialId': null, 'page': 0});
      expect(url.toString(), 'http://127.0.0.1:8000/api/pontos-coleta?municipio=Lagarto&page=0');
    });

    test('codifica acentos, espaços e caracteres literais da busca', () {
      final url = api.url('/pontos-coleta', {'bairro': 'São José', 'nome': '50%_a'});
      expect(url.queryParameters, {'bairro': 'São José', 'nome': '50%_a'});
      expect(url.toString(), contains('S%C3%A3o+Jos%C3%A9'));
    });
  });

  group('cabeçalhos', () {
    test('chamada pública não envia identidade, mesmo com ator selecionado', () async {
      late http.BaseRequest enviada;
      final api = ClienteApi(
        base: base,
        identidade: () => {'X-Perfil': 'MORADOR', 'X-Usuario-Id': '1'},
        http: MockClient((r) async {
          enviada = r;
          return json({'ok': true}, 200);
        }),
      );
      await api.get('/materiais');
      expect(enviada.headers['Accept'], 'application/json');
      expect(enviada.headers['Accept-Language'], 'pt-BR');
      expect(enviada.headers.containsKey('X-Perfil'), isFalse);
      expect(enviada.headers.containsKey('Content-Type'), isFalse);
    });

    test('chamada de ator envia identidade e JSON no corpo', () async {
      late http.Request enviada;
      final api = ClienteApi(
        base: base,
        identidade: () => {'X-Perfil': 'MORADOR', 'X-Usuario-Id': '7'},
        http: MockClient((r) async {
          enviada = r;
          return json({'id': 1}, 201);
        }),
      );
      await api.post('/solicitacoes', corpo: {'pontoId': 2});
      expect(enviada.method, 'POST');
      expect(enviada.headers['X-Perfil'], 'MORADOR');
      expect(enviada.headers['X-Usuario-Id'], '7');
      expect(enviada.headers['Content-Type'], startsWith('application/json'));
      expect(jsonDecode(enviada.body), {'pontoId': 2});
    });
  });

  group('interpretação das respostas', () {
    test('lê UTF-8 mesmo sem charset no content-type', () async {
      final api = ClienteApi(base: base, http: MockClient((_) async => json({'nome': 'Plástico'}, 200)));
      final corpo = comoMapa(await api.get('/materiais/2'));
      expect(corpo['nome'], 'Plástico');
    });

    test('204 não tem corpo', () async {
      final api = ClienteApi(base: base, http: MockClient((_) async => http.Response('', 204)));
      await expectLater(api.delete('/pontos-coleta/1/materiais/2'), completes);
    });

    test('erro padrão traz status, código e erros de campo', () async {
      final api = ClienteApi(
        base: base,
        http: MockClient(
          (_) async => json({
            'codigo': 'VALIDACAO',
            'mensagem': 'Há campos inválidos.',
            'erros': [
              {'campo': 'endereco.cep', 'mensagem': 'CEP deve ter 8 dígitos.'},
              {'campo': null, 'mensagem': 'Valor inválido para unidade.'},
            ],
          }, 400),
        ),
      );
      final erro = await api
          .post('/pontos-coleta', corpo: {})
          .then<ErroHttp?>((_) => null, onError: (e) => e as ErroHttp);
      expect(erro!.status, 400);
      expect(erro.codigo, 'VALIDACAO');
      expect(erro.validacao, isTrue);
      expect(erro.errosPorCampo, {'endereco.cep': 'CEP deve ter 8 dígitos.'});
      expect(erro.erros, hasLength(2));
    });

    test('distingue conflito de versão de conflito de regra pelo código', () async {
      Future<ErroHttp> conflito(String codigo) async {
        final api = ClienteApi(
          base: base,
          http: MockClient((_) async => json({'codigo': codigo, 'mensagem': 'x', 'erros': []}, 409)),
        );
        try {
          await api.post('/solicitacoes/1/inicio', corpo: {'versao': 0});
        } on ErroHttp catch (e) {
          return e;
        }
        fail('deveria falhar');
      }

      expect((await conflito('CONFLITO_VERSAO')).conflitoVersao, isTrue);
      expect((await conflito('CONFLITO_REGRA')).conflitoRegra, isTrue);
      expect((await conflito('CONFLITO_REGRA')).conflitoVersao, isFalse);
    });

    test('erro sem JSON vira mensagem genérica, sem expor o corpo', () async {
      final api = ClienteApi(
        base: base,
        http: MockClient(
          (_) async => http.Response('<html>stack trace</html>', 502, headers: {'content-type': 'text/html'}),
        ),
      );
      try {
        await api.get('/materiais');
        fail('deveria falhar');
      } on ErroHttp catch (e) {
        expect(e.status, 502);
        expect(e.codigo, isNull);
        expect(e.mensagem, isNot(contains('html')));
      }
    });

    test('sucesso sem JSON é resposta inválida', () async {
      final api = ClienteApi(
        base: base,
        http: MockClient((_) async => http.Response('<html></html>', 200, headers: {'content-type': 'text/html'})),
      );
      expect(api.get('/materiais'), throwsA(isA<RespostaInvalida>()));
    });

    test('falha de rede em gravação avisa que o resultado é incerto', () async {
      final api = ClienteApi(
        base: base,
        http: MockClient((_) async => throw http.ClientException('XMLHttpRequest error')),
      );
      try {
        await api.post('/moradores', corpo: {}, acesso: Acesso.publico);
        fail('deveria falhar');
      } on FalhaRede catch (e) {
        expect(e.gravacao, isTrue);
        expect(e.mensagem, contains('Consulte o registro'));
      }
    });

    test('prazo de leitura vencido vira FalhaPrazo', () async {
      final api = ClienteApi(
        base: base,
        prazoLeitura: const Duration(milliseconds: 10),
        http: MockClient((_) => Completer<http.Response>().future),
      );
      expect(api.get('/materiais'), throwsA(isA<FalhaPrazo>()));
    });
  });

  group('Pagina', () {
    test('lê a página e calcula a navegação', () {
      final pagina = Pagina<String>.deJson({
        'conteudo': [
          {'nome': 'Papel'},
        ],
        'pagina': 0,
        'tamanho': 1,
        'totalElementos': 3,
        'totalPaginas': 3,
      }, (item) => item.texto('nome'));
      expect(pagina.conteudo, ['Papel']);
      expect(pagina.temAnterior, isFalse);
      expect(pagina.temProxima, isTrue);
    });

    test('reconhece consulta além da última página', () {
      final pagina = Pagina<String>.deJson({
        'conteudo': [],
        'pagina': 5,
        'tamanho': 20,
        'totalElementos': 3,
        'totalPaginas': 1,
      }, (item) => '');
      expect(pagina.alemDaUltima, isTrue);
    });

    test('campo obrigatório ausente é resposta inválida', () {
      expect(() => Pagina<String>.deJson({'conteudo': []}, (i) => ''), throwsA(isA<RespostaInvalida>()));
    });
  });
}
