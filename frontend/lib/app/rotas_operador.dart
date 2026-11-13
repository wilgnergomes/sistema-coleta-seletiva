import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../core/api/falhas.dart';
import '../core/api/pagina.dart';
import '../core/dominio/enums.dart';
import '../features/solicitacoes/data/solicitacoes.dart';
import '../features/solicitacoes/ui/fila_operador.dart';
import '../features/solicitacoes/ui/view_models.dart';
import 'dependencias.dart';
import 'rotas.dart';

const situacaoPadraoFila = SituacaoSolicitacao.solicitada;

String urlFila(FiltroSolicitacoes f, {int? id}) {
  final q = f.paraUrl(situacaoPadrao: situacaoPadraoFila);
  return Uri(
    path: id == null ? '/operador/fila' : '/operador/fila/$id',
    queryParameters: q.isEmpty ? null : q,
  ).toString();
}

/// Rotas da fila do operador (etapas 06 e 07). As duas rotas usam a mesma
/// chave de página: abrir um detalhe atualiza a tela sem recriá-la. A lista
/// é mantida enquanto o ator for o mesmo, e abrir ou fechar detalhes
/// preserva a consulta.
List<RouteBase> rotasOperador(Dependencias d) {
  ListaSolicitacoesViewModel? lista;
  var geracao = -1;

  ListaSolicitacoesViewModel listaAtual() {
    if (lista == null || geracao != d.sessao.geracao) {
      lista = ListaSolicitacoesViewModel(
        repositorio: d.repositorioSolicitacoes,
        catalogo: d.repositorioCatalogo,
        fuso: d.fuso,
      );
      geracao = d.sessao.geracao;
    }
    return lista!;
  }

  Pagina<SolicitacaoResumo>? validacaoReaproveitavel(FiltroSolicitacoes f) {
    final resposta = d.sessao.consumirRespostaValidacao();
    if (resposta == null || f != const FiltroSolicitacoes(situacao: situacaoPadraoFila)) return null;
    try {
      return Pagina.deJson(resposta, SolicitacaoResumo.deJson);
    } on FalhaApi {
      return null;
    }
  }

  Widget fila(BuildContext context, GoRouterState estado) {
    final filtro = FiltroSolicitacoes.deQuery(estado.uri.queryParameters, situacaoPadrao: situacaoPadraoFila);
    final selecionado = idDaRota(estado);
    return PaginaFila(
      key: ValueKey('fila-${d.sessao.geracao}'),
      lista: listaAtual(),
      filtro: filtro,
      selecionado: selecionado,
      reaproveitar: validacaoReaproveitavel(filtro),
      criarDetalhe: (id) => DetalheSolicitacaoViewModel(
        repositorio: d.repositorioSolicitacoes,
        catalogo: d.repositorioCatalogo,
        fuso: d.fuso,
        id: id,
      ),
      aoMudarFiltro: (f) => context.go(urlFila(f, id: selecionado)),
      aoSelecionar: (id) => context.go(urlFila(filtro, id: id)),
      aoFecharDetalhe: () => context.go(urlFila(filtro)),
    );
  }

  return [
    GoRoute(
      path: '/operador/fila',
      pageBuilder: (context, estado) => NoTransitionPage(key: const ValueKey('fila'), child: fila(context, estado)),
    ),
    GoRoute(
      path: '/operador/fila/:id',
      pageBuilder: (context, estado) => NoTransitionPage(key: const ValueKey('fila'), child: fila(context, estado)),
    ),
  ];
}
