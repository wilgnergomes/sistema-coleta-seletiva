import 'package:material_ui/material_ui.dart';

import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/datas.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../catalogo/data/repositorio_catalogo.dart';
import '../data/solicitacoes.dart';

/// Lista de solicitações no escopo do ator (RF09).
class ListaSolicitacoesViewModel extends ChangeNotifier {
  ListaSolicitacoesViewModel({required this.repositorio, required this.catalogo, required this.fuso});

  final RepositorioSolicitacoes repositorio;
  final RepositorioCatalogo catalogo;
  final FusoNegocio fuso;
  final carga = Carga<Pagina<SolicitacaoResumo>>();
  FiltroSolicitacoes? filtro;

  Future<void> carregar(FiltroSolicitacoes novo, {Pagina<SolicitacaoResumo>? reaproveitar}) {
    filtro = novo;
    if (reaproveitar != null) {
      return Future.wait([fuso.garantir(), catalogo.carregarNomes()]).then((_) {
        carga.definir(reaproveitar);
        notifyListeners();
      });
    }
    return carga.executar(() async {
      final resultado = await Future.wait<Object?>([
        repositorio.listar(novo),
        fuso.garantir(),
        catalogo.carregarNomes(),
      ]);
      return resultado.first as Pagina<SolicitacaoResumo>;
    }, notifyListeners);
  }

  String nomePonto(int id) => catalogo.rotuloPonto(id);
}

/// Detalhe com execução de ações e política de conflito (RN06).
class DetalheSolicitacaoViewModel extends ChangeNotifier {
  DetalheSolicitacaoViewModel({
    required this.repositorio,
    required this.catalogo,
    required this.fuso,
    required this.id,
  });

  final RepositorioSolicitacoes repositorio;
  final RepositorioCatalogo catalogo;
  final FusoNegocio fuso;
  final int id;
  final carga = Carga<SolicitacaoDetalhe>();
  bool enviando = false;
  String? aviso;
  TipoAviso tipoAviso = TipoAviso.info;

  /// Detalhe indisponível no contexto (403 ou 404): o conteúdo foi retirado.
  bool get indisponivel {
    final f = carga.falha;
    return f is ErroHttp && (f.naoEncontrado || f.acessoNegado);
  }

  Future<void> carregar({bool limpar = true}) async {
    await carga.executar(
      () async {
        final r = await Future.wait<Object?>([repositorio.detalhe(id), fuso.garantir()]);
        return r.first as SolicitacaoDetalhe;
      },
      notifyListeners,
      limpar: limpar,
    );
    final d = carga.dados;
    if (d != null && !catalogo.nomesConhecidos.containsKey(d.pontoId)) {
      try {
        await catalogo.ponto(d.pontoId);
      } on FalhaApi {
        // Ponto inativo ou indisponível: mantém o rótulo `Ponto #id`.
      }
      notifyListeners();
    }
  }

  String get nomePonto => carga.dados == null ? '' : catalogo.rotuloPonto(carga.dados!.pontoId);

  /// Executa uma ação com a versão lida. Em conflito, relê o detalhe para
  /// apresentar estado, versão e ações atuais; nenhuma repetição é automática.
  Future<bool> executar(
    Future<SolicitacaoDetalhe> Function(SolicitacaoDetalhe atual) acao, {
    required String sucesso,
  }) async {
    final atual = carga.dados;
    if (atual == null || enviando) return false;
    enviando = true;
    aviso = null;
    notifyListeners();
    try {
      carga.definir(await acao(atual));
      aviso = sucesso;
      tipoAviso = TipoAviso.sucesso;
      return true;
    } on ErroHttp catch (e) {
      tipoAviso = e.conflito ? TipoAviso.alerta : TipoAviso.erro;
      if (e.conflitoVersao) {
        aviso = '${e.mensagem} A solicitação foi atualizada por outra ação. Revise a situação e as ações disponíveis.';
      } else {
        aviso = e.mensagem;
      }
      if (e.conflito) {
        await carga.executar(() => repositorio.detalhe(id), notifyListeners);
      } else if (e.naoEncontrado || e.acessoNegado) {
        carga
          ..invalidar()
          ..falha = e;
      }
      return false;
    } on FalhaApi catch (e) {
      aviso = e.mensagem;
      tipoAviso = TipoAviso.erro;
      return false;
    } finally {
      enviando = false;
      notifyListeners();
    }
  }
}
