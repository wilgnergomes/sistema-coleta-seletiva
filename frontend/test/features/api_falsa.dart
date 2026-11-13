import 'dart:convert';

import 'package:coleta_seletiva_web/core/api/cliente_api.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

/// Rota simulada: recebe a requisição e devolve (status, corpo JSON).
typedef Resposta = (int, Object?) Function(http.Request requisicao);

/// API simulada por método e caminho, que registra as requisições recebidas.
class ApiFalsa {
  ApiFalsa([Map<String, Resposta>? rotas]) : rotas = rotas ?? {};

  final Map<String, Resposta> rotas;
  final requisicoes = <http.Request>[];

  void quando(String metodoECaminho, Resposta resposta) => rotas[metodoECaminho] = resposta;

  ClienteApi cliente({Map<String, String> Function()? identidade}) => ClienteApi(
    base: Uri.parse('http://teste/api'),
    identidade: identidade,
    http: MockClient((r) async {
      requisicoes.add(r);
      final caminho = r.url.path.replaceFirst('/api', '');
      final resposta = rotas['${r.method} $caminho'];
      if (resposta == null) {
        return http.Response.bytes(
          utf8.encode(jsonEncode({'codigo': 'NAO_ENCONTRADO', 'mensagem': 'Recurso não encontrado.', 'erros': []})),
          404,
          headers: {'content-type': 'application/json'},
        );
      }
      final (status, corpo) = resposta(r);
      return http.Response.bytes(
        corpo == null ? [] : utf8.encode(jsonEncode(corpo)),
        status,
        headers: {'content-type': 'application/json'},
      );
    }),
  );

  Iterable<http.Request> chamadas(String metodoECaminho) =>
      requisicoes.where((r) => '${r.method} ${r.url.path.replaceFirst('/api', '')}' == metodoECaminho);
}

Map<String, Object?> pagina(List<Object?> conteudo, {int pagina = 0, int tamanho = 20, int? total}) {
  final totalElementos = total ?? conteudo.length;
  return {
    'conteudo': conteudo,
    'pagina': pagina,
    'tamanho': tamanho,
    'totalElementos': totalElementos,
    'totalPaginas': (totalElementos / tamanho).ceil(),
  };
}

Map<String, Object?> materialJson(int id, String nome, {String unidade = 'KG', String situacao = 'ATIVO'}) => {
  'id': id,
  'nome': nome,
  'descricao': 'Descrição de $nome',
  'unidade': unidade,
  'orientacao': 'Orientação de $nome',
  'situacao': situacao,
};

Map<String, Object?> pontoJson(
  int id,
  String nome, {
  bool domiciliar = true,
  String municipio = 'Lagarto',
  String bairro = 'Centro',
  String situacao = 'ATIVO',
  String? contato,
  List<Map<String, Object?>> materiais = const [],
}) => {
  'id': id,
  'nome': nome,
  'contato': contato,
  'horarios': null,
  'recolhimentoDomiciliar': domiciliar,
  'situacao': situacao,
  'endereco': {
    'logradouro': 'Rua das Flores',
    'numero': '120',
    'bairro': bairro,
    'municipio': municipio,
    'uf': 'SE',
    'cep': '49400000',
    'complemento': null,
  },
  'materiaisAceitos': materiais,
};

Map<String, Object?> vinculoJson(int materialId, String nome, {String unidade = 'KG', num? limite}) => {
  'materialId': materialId,
  'nomeMaterial': nome,
  'unidade': unidade,
  'orientacao': 'Orientação de $nome',
  'observacoes': null,
  'limite': limite,
};
