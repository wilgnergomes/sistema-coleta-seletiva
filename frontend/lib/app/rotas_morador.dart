import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../features/solicitacoes/data/solicitacoes.dart';
import '../features/solicitacoes/ui/formulario_solicitacao.dart';
import '../features/solicitacoes/ui/recentes_morador.dart';
import '../features/solicitacoes/ui/solicitacoes_morador.dart';
import '../features/solicitacoes/ui/view_models.dart';
import 'dependencias.dart';
import 'rotas.dart';

String urlSolicitacoesMorador(FiltroSolicitacoes f) {
  final q = f.paraUrl();
  return Uri(path: '/morador/solicitacoes', queryParameters: q.isEmpty ? null : q).toString();
}

Widget secoesRecentes(BuildContext context, Dependencias d, String? municipio) => SecoesRecentesMorador(
  key: ValueKey('recentes-${d.sessao.geracao}'),
  municipio: municipio,
  solicitacoes: d.repositorioSolicitacoes,
  catalogo: d.repositorioCatalogo,
  fuso: d.fuso,
  aoAbrirPonto: (id) => context.push('/pontos/$id'),
  aoAbrirSolicitacao: (id) => context.push('/morador/solicitacoes/$id'),
  aoVerTodas: () => context.go('/morador/solicitacoes'),
);

/// Rotas das solicitações do morador (etapa 05).
List<RouteBase> rotasSolicitacoesMorador(Dependencias d) {
  final sessao = d.sessao;
  return [
    GoRoute(
      path: '/morador/solicitacoes',
      builder: (context, estado) => PaginaSolicitacoes(
        viewModel: ListaSolicitacoesViewModel(
          repositorio: d.repositorioSolicitacoes,
          catalogo: d.repositorioCatalogo,
          fuso: d.fuso,
        ),
        filtro: FiltroSolicitacoes.deQuery(estado.uri.queryParameters),
        aoMudarFiltro: (f) => context.go(urlSolicitacoesMorador(f)),
        aoAbrir: (id) => context.push('/morador/solicitacoes/$id'),
        acaoVazia: FilledButton(onPressed: () => context.go('/pontos'), child: const Text('Solicitar recolhimento')),
      ),
    ),
    GoRoute(
      path: '/morador/solicitacoes/nova',
      builder: (context, estado) {
        final pontoId = int.tryParse(estado.uri.queryParameters['pontoId'] ?? '');
        return PaginaFormularioSolicitacao(
          key: ValueKey('nova-$pontoId-${sessao.geracao}'),
          moradorId: sessao.usuarioId!,
          solicitacoes: d.repositorioSolicitacoes,
          catalogo: d.repositorioCatalogo,
          moradores: d.repositorioMoradores,
          pontoInicial: pontoId != null && pontoId > 0 ? pontoId : null,
          enderecoPreferido: sessao.enderecoSelecionadoId,
          aoVoltar: () => voltarOu(context, '/morador/inicio'),
          aoConsultarPontos: () => context.go('/pontos'),
          aoConcluir: (r, {required criada}) =>
              context.go('/morador/solicitacoes/${r.id}?mensagem=${criada ? 'criada' : 'corrigida'}'),
        );
      },
    ),
    GoRoute(
      path: '/morador/solicitacoes/:id',
      builder: (context, estado) {
        final id = idDaRota(estado);
        if (id == null) return rotaInvalida(context, destino: '/morador/inicio');
        final mensagem = switch (estado.uri.queryParameters['mensagem']) {
          'criada' => 'Solicitação criada. Guarde o protocolo para acompanhar o pedido.',
          'corrigida' => 'Correção salva.',
          _ => null,
        };
        return PaginaDetalheSolicitacaoMorador(
          key: ValueKey('sol-$id-${sessao.geracao}'),
          viewModel: DetalheSolicitacaoViewModel(
            repositorio: d.repositorioSolicitacoes,
            catalogo: d.repositorioCatalogo,
            fuso: d.fuso,
            id: id,
          ),
          mensagemInicial: mensagem,
          aoVoltar: () => voltarOu(context, '/morador/solicitacoes'),
          aoCorrigir: (id) => context.push('/morador/solicitacoes/$id/corrigir'),
        );
      },
    ),
    GoRoute(
      path: '/morador/solicitacoes/:id/corrigir',
      builder: (context, estado) {
        final id = idDaRota(estado);
        if (id == null) return rotaInvalida(context, destino: '/morador/inicio');
        return PaginaFormularioSolicitacao(
          key: ValueKey('corrigir-$id-${sessao.geracao}'),
          moradorId: sessao.usuarioId!,
          solicitacaoId: id,
          solicitacoes: d.repositorioSolicitacoes,
          catalogo: d.repositorioCatalogo,
          moradores: d.repositorioMoradores,
          aoVoltar: () => voltarOu(context, '/morador/solicitacoes/$id'),
          aoConsultarPontos: () => context.go('/pontos'),
          aoConcluir: (r, {required criada}) => context.go('/morador/solicitacoes/${r.id}?mensagem=corrigida'),
        );
      },
    ),
  ];
}
