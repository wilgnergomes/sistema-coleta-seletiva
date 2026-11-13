import 'package:http/http.dart' as http;

import '../core/api/cliente_api.dart';
import 'configuracao.dart';

/// Composição das dependências por construtores.
class Dependencias {
  Dependencias({required this.configuracao, required this.api});

  factory Dependencias.doAmbiente() {
    final configuracao = ConfiguracaoApp.doAmbiente();
    final api = ClienteApi(base: configuracao.resolverBase(Uri.base), http: http.Client());
    return Dependencias(configuracao: configuracao, api: api);
  }

  final ConfiguracaoApp configuracao;
  final ClienteApi api;
}
