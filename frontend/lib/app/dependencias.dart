import 'package:http/http.dart' as http;

import '../core/api/cliente_api.dart';
import '../core/dominio/datas.dart';
import '../features/catalogo/data/repositorio_catalogo.dart';
import '../features/demonstracao/pagina_demonstracao.dart';
import '../features/moradores/data/moradores.dart';
import '../features/solicitacoes/data/solicitacoes.dart';
import 'configuracao.dart';
import 'navegacao.dart';
import 'sessao.dart';

/// Composição das dependências por construtores.
class Dependencias {
  Dependencias({required this.configuracao, required this.api, required this.sessao})
    : repositorioCatalogo = RepositorioCatalogo(api),
      repositorioMoradores = RepositorioMoradores(api),
      repositorioSolicitacoes = RepositorioSolicitacoes(api),
      fuso = FusoNegocio(configuracao.fusoNegocio) {
    validadorAtor = ValidadorAtor(api, repositorioMoradores);
    // Conteúdo lido com a identidade anterior não pode reaparecer.
    sessao.aoTrocarAtor(repositorioCatalogo.invalidar);
  }

  factory Dependencias.doAmbiente() {
    final configuracao = ConfiguracaoApp.doAmbiente();
    final sessao = Sessao();
    final api = ClienteApi(
      base: configuracao.resolverBase(Uri.base),
      http: http.Client(),
      identidade: sessao.cabecalhos,
    );
    return Dependencias(configuracao: configuracao, api: api, sessao: sessao);
  }

  final ConfiguracaoApp configuracao;
  final ClienteApi api;
  final Sessao sessao;
  final RepositorioCatalogo repositorioCatalogo;
  final RepositorioMoradores repositorioMoradores;
  final RepositorioSolicitacoes repositorioSolicitacoes;
  final FusoNegocio fuso;
  late final ValidadorAtor validadorAtor;

  /// Último destino aberto, para a direção das transições entre páginas.
  late final navegacao = MemoriaDestinos(() => sessao.perfil);
}
