import '../../../core/api/cliente_api.dart';
import '../../../core/api/json.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/datas.dart';
import '../../../core/dominio/enums.dart';
import '../../../core/dominio/quantidade.dart';

class SolicitacaoResumo {
  const SolicitacaoResumo({
    required this.id,
    required this.protocolo,
    required this.moradorId,
    required this.pontoId,
    required this.enderecoCopia,
    required this.situacao,
    required this.criadaEm,
    required this.versao,
    this.agendadaPara,
    this.concluidaEm,
    this.observacoes,
  });

  factory SolicitacaoResumo.deJson(Json json) => SolicitacaoResumo(
    id: json.inteiro('id'),
    protocolo: json.texto('protocolo'),
    moradorId: json.inteiro('moradorId'),
    pontoId: json.inteiro('pontoId'),
    enderecoCopia: json.textoOpcional('enderecoCopia') ?? '',
    situacao: SituacaoSolicitacao.deApi(json.textoOpcional('situacao')),
    criadaEm: json.instante('criadaEm'),
    agendadaPara: json.instanteOpcional('agendadaPara'),
    concluidaEm: json.instanteOpcional('concluidaEm'),
    observacoes: json.textoOpcional('observacoes'),
    versao: json.inteiro('versao'),
  );

  final int id;
  final String protocolo;
  final int moradorId;
  final int pontoId;
  final String enderecoCopia;
  final SituacaoSolicitacao situacao;
  final DateTime criadaEm;
  final DateTime? agendadaPara;
  final DateTime? concluidaEm;
  final String? observacoes;
  final int versao;
}

class ItemSolicitacao {
  const ItemSolicitacao({
    required this.materialId,
    required this.nomeMaterial,
    required this.unidade,
    required this.quantidadeEstimada,
    this.quantidadeEfetiva,
  });

  factory ItemSolicitacao.deJson(Json json) => ItemSolicitacao(
    materialId: json.inteiro('materialId'),
    nomeMaterial: json.texto('nomeMaterial'),
    unidade: Unidade.deApi(json.textoOpcional('unidade')),
    quantidadeEstimada: Quantidade.deNumero(json.numero('quantidadeEstimada')),
    quantidadeEfetiva: json['quantidadeEfetiva'] == null ? null : Quantidade.deNumero(json.numero('quantidadeEfetiva')),
  );

  final int materialId;
  final String nomeMaterial;

  /// Unidade copiada na criação (RN04).
  final Unidade unidade;
  final Quantidade quantidadeEstimada;

  /// Nula até a conclusão; zero é um valor informado.
  final Quantidade? quantidadeEfetiva;
}

class Historico {
  const Historico({
    required this.id,
    required this.situacaoNova,
    required this.data,
    required this.tipoResponsavel,
    required this.idResponsavel,
    this.situacaoAnterior,
    this.motivo,
    this.agendamento,
    this.informacoesTentativa,
  });

  factory Historico.deJson(Json json) {
    final responsavel = json.mapaOpcional('responsavel');
    return Historico(
      id: json.inteiro('id'),
      situacaoAnterior: json['situacaoAnterior'] == null
          ? null
          : SituacaoSolicitacao.deApi(json.texto('situacaoAnterior')),
      situacaoNova: SituacaoSolicitacao.deApi(json.textoOpcional('situacaoNova')),
      data: json.instante('data'),
      tipoResponsavel: TipoResponsavel.deApi(responsavel?.textoOpcional('tipo')),
      idResponsavel: responsavel?.inteiroOpcional('id'),
      motivo: json.textoOpcional('motivo'),
      agendamento: json.instanteOpcional('agendamento'),
      informacoesTentativa: json.textoOpcional('informacoesTentativa'),
    );
  }

  final int id;
  final SituacaoSolicitacao? situacaoAnterior;
  final SituacaoSolicitacao situacaoNova;
  final DateTime data;
  final TipoResponsavel tipoResponsavel;
  final int? idResponsavel;
  final String? motivo;
  final DateTime? agendamento;
  final String? informacoesTentativa;

  String get responsavel =>
      idResponsavel == null ? tipoResponsavel.rotulo : '${tipoResponsavel.rotulo} #$idResponsavel';
}

class SolicitacaoDetalhe extends SolicitacaoResumo {
  const SolicitacaoDetalhe({
    required super.id,
    required super.protocolo,
    required super.moradorId,
    required super.pontoId,
    required super.enderecoCopia,
    required super.situacao,
    required super.criadaEm,
    required super.versao,
    required this.itens,
    required this.historico,
    super.agendadaPara,
    super.concluidaEm,
    super.observacoes,
  });

  factory SolicitacaoDetalhe.deJson(Json json) {
    final r = SolicitacaoResumo.deJson(json);
    return SolicitacaoDetalhe(
      id: r.id,
      protocolo: r.protocolo,
      moradorId: r.moradorId,
      pontoId: r.pontoId,
      enderecoCopia: r.enderecoCopia,
      situacao: r.situacao,
      criadaEm: r.criadaEm,
      agendadaPara: r.agendadaPara,
      concluidaEm: r.concluidaEm,
      observacoes: r.observacoes,
      versao: r.versao,
      itens: json.listaOuVazia('itens', ItemSolicitacao.deJson),
      historico: json.listaOuVazia('historico', Historico.deJson),
    );
  }

  final List<ItemSolicitacao> itens;
  final List<Historico> historico;
}

/// Filtros do RF09, refletidos na URL. Período pela data de criação.
class FiltroSolicitacoes {
  const FiltroSolicitacoes({this.situacao, this.pontoId, this.de, this.ate, this.pagina = 0});

  factory FiltroSolicitacoes.deQuery(Map<String, String> q, {SituacaoSolicitacao? situacaoPadrao}) {
    final situacao = q['situacao'] == 'TODAS'
        ? null
        : (q.containsKey('situacao') ? SituacaoSolicitacao.deApi(q['situacao']) : situacaoPadrao);
    final pontoId = int.tryParse(q['pontoId'] ?? '');
    final pagina = int.tryParse(q['pagina'] ?? '') ?? 1;
    return FiltroSolicitacoes(
      situacao: situacao == SituacaoSolicitacao.desconhecida ? null : situacao,
      pontoId: pontoId != null && pontoId > 0 ? pontoId : null,
      de: DataCivil.ler(q['de']),
      ate: DataCivil.ler(q['ate']),
      pagina: pagina < 1 ? 0 : pagina - 1,
    );
  }

  final SituacaoSolicitacao? situacao;
  final int? pontoId;
  final DataCivil? de;
  final DataCivil? ate;
  final int pagina;

  bool get temFiltros => situacao != null || pontoId != null || de != null || ate != null;

  /// [situacaoPadrao] marca `TODAS` quando o padrão da tela é uma situação.
  Map<String, String> paraUrl({SituacaoSolicitacao? situacaoPadrao}) => {
    if (situacao != null) 'situacao': situacao!.api,
    if (situacao == null && situacaoPadrao != null) 'situacao': 'TODAS',
    if (pontoId != null) 'pontoId': '$pontoId',
    if (de != null) 'de': de!.iso,
    if (ate != null) 'ate': ate!.iso,
    if (pagina > 0) 'pagina': '${pagina + 1}',
  };

  Map<String, Object?> paraApi({int tamanho = 20}) => {
    'situacao': situacao?.api,
    'pontoId': pontoId,
    'de': de?.iso,
    'ate': ate?.iso,
    'page': pagina,
    'size': tamanho,
  };

  FiltroSolicitacoes naPagina(int p) =>
      FiltroSolicitacoes(situacao: situacao, pontoId: pontoId, de: de, ate: ate, pagina: p);

  FiltroSolicitacoes comSituacao(SituacaoSolicitacao? s) =>
      FiltroSolicitacoes(situacao: s, pontoId: pontoId, de: de, ate: ate);

  @override
  bool operator ==(Object other) =>
      other is FiltroSolicitacoes &&
      other.situacao == situacao &&
      other.pontoId == pontoId &&
      other.de == de &&
      other.ate == ate &&
      other.pagina == pagina;

  @override
  int get hashCode => Object.hash(situacao, pontoId, de, ate, pagina);
}

/// Item enviado na criação ou na correção.
class ItemPedido {
  const ItemPedido(this.materialId, this.quantidade);

  final int materialId;
  final Quantidade quantidade;

  Json paraJson() => {'materialId': materialId, 'quantidadeEstimada': quantidade.comoNumero};
}

/// Solicitações e transições (RF08 a RF14).
class RepositorioSolicitacoes {
  RepositorioSolicitacoes(this._api);

  final ClienteApi _api;

  Future<Pagina<SolicitacaoResumo>> listar(FiltroSolicitacoes filtro, {int tamanho = 20}) async => Pagina.deJson(
    await _api.get(
      '/solicitacoes',
      query: filtro.paraApi(tamanho: tamanho),
      acesso: Acesso.ator,
    ),
    SolicitacaoResumo.deJson,
  );

  Future<SolicitacaoDetalhe> detalhe(int id) async =>
      SolicitacaoDetalhe.deJson(comoMapa(await _api.get('/solicitacoes/$id', acesso: Acesso.ator)));

  Future<SolicitacaoDetalhe> criar({
    required int pontoId,
    required int enderecoId,
    required List<ItemPedido> itens,
    String? observacoes,
  }) => _enviar('POST', '/solicitacoes', {
    'pontoId': pontoId,
    'enderecoId': enderecoId,
    'observacoes': _texto(observacoes),
    'itens': [for (final i in itens) i.paraJson()],
  });

  Future<SolicitacaoDetalhe> corrigir(
    int id, {
    required int enderecoId,
    required List<ItemPedido> itens,
    required int versao,
    String? observacoes,
  }) => _enviar('PUT', '/solicitacoes/$id', {
    'enderecoId': enderecoId,
    'observacoes': _texto(observacoes),
    'itens': [for (final i in itens) i.paraJson()],
    'versao': versao,
  });

  Future<SolicitacaoDetalhe> agendar(int id, DateTime dataHora, int versao) =>
      _acao(id, 'agendamento', {'dataHora': _iso(dataHora), 'versao': versao});

  Future<SolicitacaoDetalhe> recusar(int id, String motivo, int versao) =>
      _acao(id, 'recusa', {'motivo': motivo.trim(), 'versao': versao});

  Future<SolicitacaoDetalhe> cancelar(int id, String motivo, int versao) =>
      _acao(id, 'cancelamento', {'motivo': motivo.trim(), 'versao': versao});

  Future<SolicitacaoDetalhe> iniciar(int id, int versao) => _acao(id, 'inicio', {'versao': versao});

  Future<SolicitacaoDetalhe> concluir(int id, Map<int, Quantidade> efetivas, int versao) => _acao(id, 'conclusao', {
    'versao': versao,
    'itens': [
      for (final e in efetivas.entries) {'materialId': e.key, 'quantidadeEfetiva': e.value.comoNumero},
    ],
  });

  Future<SolicitacaoDetalhe> reagendar(int id, DateTime dataHora, String motivo, int versao) =>
      _acao(id, 'reagendamento', {'dataHora': _iso(dataHora), 'motivo': motivo.trim(), 'versao': versao});

  Future<SolicitacaoDetalhe> registrarFalha(
    int id, {
    required String motivo,
    required String informacoesTentativa,
    required DateTime novaDataHora,
    required int versao,
  }) => _acao(id, 'falha', {
    'motivo': motivo.trim(),
    'informacoesTentativa': informacoesTentativa.trim(),
    'dataHora': _iso(novaDataHora),
    'versao': versao,
  });

  Future<SolicitacaoDetalhe> _acao(int id, String acao, Json corpo) =>
      _enviar('POST', '/solicitacoes/$id/$acao', corpo);

  Future<SolicitacaoDetalhe> _enviar(String metodo, String caminho, Json corpo) async {
    final json = metodo == 'PUT' ? await _api.put(caminho, corpo: corpo) : await _api.post(caminho, corpo: corpo);
    return SolicitacaoDetalhe.deJson(comoMapa(json));
  }

  /// Instante em UTC com `Z`, aceito como `OffsetDateTime` pelo backend.
  static String _iso(DateTime instante) => instante.toUtc().toIso8601String();

  static String? _texto(String? t) => t == null || t.trim().isEmpty ? null : t.trim();
}
