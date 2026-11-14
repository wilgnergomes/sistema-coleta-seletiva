import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../ui/compartilhado/movimento.dart';
import '../ui/compartilhado/shell.dart';
import 'sessao.dart';
import 'shell_app.dart';

/// Lembra o último destino principal aberto para decidir a direção da
/// próxima transição, pela ordem em que os destinos aparecem na navegação.
///
/// O go_router chama o `pageBuilder` a cada reconstrução; enquanto a chave
/// da página for a mesma, a direção já calculada é mantida. Mudar só a
/// consulta (filtro, paginação) não muda a chave e, portanto, não anima.
class MemoriaDestinos {
  MemoriaDestinos(this._perfil);

  final Perfil? Function() _perfil;

  LocalKey? _chave;
  List<DestinoNavegacao>? _lista;
  int? _indice;
  DirecaoNavegacao _direcao = DirecaoNavegacao.neutra;
  DirecaoNavegacao _saida = DirecaoNavegacao.neutra;

  /// Direção da troca de destino em andamento, usada pela página que sai.
  /// Neutra quando a última página aberta foi um detalhe.
  DirecaoNavegacao get direcaoDaSaida => _saida;

  /// Direção de entrada do destino de [estado].
  DirecaoNavegacao direcaoPara(GoRouterState estado) {
    if (estado.pageKey == _chave) return _direcao;
    final caminho = estado.uri.path;
    final lista = _ordemPublica(caminho) ?? destinosDaArea(areaDaRota(caminho, _perfil()));
    final indice = lista.indexWhere((d) => d.rota == caminho);
    final novo = indice < 0 ? null : indice;
    _direcao = direcaoEntre(_lista, _indice, lista, novo);
    _saida = _direcao;
    _chave = estado.pageKey;
    _lista = lista;
    _indice = novo;
    return _direcao;
  }

  /// Um detalhe aberto sobre o destino: a página de baixo não se desloca.
  void registrarDetalhe() => _saida = DirecaoNavegacao.neutra;

  /// Na área pública a ordem é a do cabeçalho (Início, Pontos, Materiais).
  List<DestinoNavegacao>? _ordemPublica(String caminho) =>
      areaDaRota(caminho, _perfil()) == null ? destinosPublicos : null;
}

/// Direção entre dois destinos: só há direção dentro da mesma navegação e
/// entre destinos conhecidos; trocas de perfil e páginas fora da ordem são
/// neutras.
DirecaoNavegacao direcaoEntre(
  List<DestinoNavegacao>? listaAnterior,
  int? anterior,
  List<DestinoNavegacao> lista,
  int? atual,
) {
  if (anterior == null || atual == null || !identical(listaAnterior, lista) || anterior == atual) {
    return DirecaoNavegacao.neutra;
  }
  return atual > anterior ? DirecaoNavegacao.avanca : DirecaoNavegacao.recua;
}
