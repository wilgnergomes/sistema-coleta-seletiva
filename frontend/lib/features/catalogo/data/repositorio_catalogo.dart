import '../../../core/api/cliente_api.dart';
import '../../../core/api/json.dart';
import '../../../core/api/pagina.dart';
import '../../../core/estado/cache.dart';
import 'modelos.dart';

/// Materiais ativos do catálogo público, lidos numa única página.
class CatalogoMateriais {
  const CatalogoMateriais({required this.itens, required this.completo});

  final List<MaterialColeta> itens;

  /// A página trouxe todos os materiais ativos. Só então a ausência de um
  /// material comprova que ele está inativo.
  final bool completo;

  MaterialColeta? porId(int id) {
    for (final m in itens) {
      if (m.id == id) return m;
    }
    return null;
  }
}

/// Pontos, materiais e vínculos (RF03 a RF06).
class RepositorioCatalogo {
  RepositorioCatalogo(this._api);

  final ClienteApi _api;
  final _pontos = CacheMemoria<String, Pagina<PontoColeta>>(maximo: 30, validade: const Duration(minutes: 1));
  final _detalhes = CacheMemoria<String, PontoColeta>(maximo: 50, validade: const Duration(minutes: 1));
  Future<CatalogoMateriais>? _ativos;
  final _materialPendente = <int, Future<MaterialColeta>>{};

  static const tamanhoPagina = 20;

  String _chave(Acesso acesso, Object consulta) => '${acesso.name}|$consulta';

  Future<Pagina<PontoColeta>> pesquisarPontos(
    FiltroPontos filtro, {
    Acesso acesso = Acesso.publico,
    int tamanho = tamanhoPagina,
    bool forcar = false,
  }) async {
    final query = filtro.paraApi(tamanho: tamanho);
    final chave = _chave(acesso, query);
    if (!forcar) {
      final guardada = _pontos.ler(chave);
      if (guardada != null) return guardada;
    }
    final json = await _api.get('/pontos-coleta', query: query, acesso: acesso);
    final pagina = Pagina.deJson(json, PontoColeta.deJson);
    _pontos.gravar(chave, pagina);
    return pagina;
  }

  Future<PontoColeta> ponto(int id, {Acesso acesso = Acesso.publico, bool forcar = false}) async {
    final chave = _chave(acesso, id);
    if (!forcar) {
      final guardado = _detalhes.ler(chave);
      if (guardado != null) return guardado;
    }
    final ponto = PontoColeta.deJson(comoMapa(await _api.get('/pontos-coleta/$id', acesso: acesso)));
    _detalhes.gravar(chave, ponto);
    return ponto;
  }

  Future<Pagina<MaterialColeta>> materiais({
    int pagina = 0,
    int tamanho = tamanhoPagina,
    Acesso acesso = Acesso.publico,
    String? situacao,
  }) async {
    final json = await _api.get(
      '/materiais',
      query: {'page': pagina, 'size': tamanho, 'situacao': situacao},
      acesso: acesso,
    );
    return Pagina.deJson(json, MaterialColeta.deJson);
  }

  /// Catálogo de materiais ativos para seletores e confirmação de
  /// disponibilidade. Uma leitura de até 100 itens, reaproveitada.
  Future<CatalogoMateriais> materiaisAtivos() {
    return _ativos ??= () async {
      try {
        final pagina = await materiais(tamanho: 100);
        return CatalogoMateriais(itens: pagina.conteudo, completo: !pagina.temProxima);
      } catch (_) {
        _ativos = null;
        rethrow;
      }
    }();
  }

  /// Detalhe de um material, com deduplicação de leituras simultâneas.
  Future<MaterialColeta> material(int id, {Acesso acesso = Acesso.publico}) {
    if (acesso == Acesso.publico) {
      return _materialPendente[id] ??= _api
          .get('/materiais/$id')
          .then((json) => MaterialColeta.deJson(comoMapa(json)))
          .whenComplete(() => _materialPendente.remove(id));
    }
    return _api.get('/materiais/$id', acesso: acesso).then((json) => MaterialColeta.deJson(comoMapa(json)));
  }

  /// Esquece o que foi lido, após mutação administrativa.
  void invalidar() {
    _pontos.limpar();
    _detalhes.limpar();
    _ativos = null;
  }
}
