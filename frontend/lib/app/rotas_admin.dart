import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../core/api/falhas.dart';
import '../core/dominio/datas.dart';
import '../core/dominio/enums.dart';
import '../features/catalogo/ui/seletor_ponto.dart';
import '../features/resumo_gerencial/resumo_gerencial.dart';
import '../features/solicitacoes/data/solicitacoes.dart';
import '../features/solicitacoes/ui/componentes_solicitacao.dart';
import '../features/solicitacoes/ui/solicitacoes_morador.dart';
import '../features/solicitacoes/ui/view_models.dart';
import '../ui/compartilhado/estados.dart';
import '../ui/compartilhado/layout.dart';
import 'dependencias.dart';
import 'rotas.dart';
import 'tema/tokens.dart';

String urlSolicitacoesAdmin(FiltroSolicitacoes f) {
  final q = f.paraUrl();
  return Uri(path: '/administracao/solicitacoes', queryParameters: q.isEmpty ? null : q).toString();
}

/// Rotas da área administrativa: resumo e consulta (etapa 08).
List<RouteBase> rotasAdministracao(Dependencias d) {
  final resumo = RepositorioResumo(d.api);

  ResumoGerencial? resumoDaValidacao() {
    final r = d.sessao.consumirRespostaValidacao();
    if (r == null) return null;
    try {
      return ResumoGerencial.deJson(r);
    } on FalhaApi {
      return null;
    }
  }

  return [
    GoRoute(path: '/administracao', redirect: (_, _) => '/administracao/resumo'),
    GoRoute(
      path: '/administracao/resumo',
      builder: (context, estado) {
        final de = DataCivil.ler(estado.uri.queryParameters['de']);
        final ate = DataCivil.ler(estado.uri.queryParameters['ate']);
        return PaginaResumo(
          repositorio: resumo,
          de: de,
          ate: ate,
          reaproveitar: resumoDaValidacao(),
          aoMudarPeriodo: (de, ate) => context.go(
            Uri(
              path: '/administracao/resumo',
              queryParameters: {'de': ?de?.iso, 'ate': ?ate?.iso}.isEmpty ? null : {'de': ?de?.iso, 'ate': ?ate?.iso},
            ).toString(),
          ),
          aoAbrirSituacao: (s) => context.go(urlSolicitacoesAdmin(FiltroSolicitacoes(situacao: s, de: de, ate: ate))),
        );
      },
    ),
    GoRoute(
      path: '/administracao/solicitacoes',
      builder: (context, estado) {
        final filtro = FiltroSolicitacoes.deQuery(estado.uri.queryParameters);
        return PaginaSolicitacoes(
          titulo: 'Solicitações',
          mostrarMorador: true,
          viewModel: ListaSolicitacoesViewModel(
            repositorio: d.repositorioSolicitacoes,
            catalogo: d.repositorioCatalogo,
            fuso: d.fuso,
          ),
          filtro: filtro,
          aoMudarFiltro: (f) => context.go(urlSolicitacoesAdmin(f)),
          aoAbrir: (id) => context.push('/administracao/solicitacoes/$id'),
          filtrosExtras: OutlinedButton.icon(
            icon: const Icon(Icons.place_outlined),
            label: Text(
              filtro.pontoId == null ? 'Ponto: todos' : 'Ponto: ${d.repositorioCatalogo.rotuloPonto(filtro.pontoId!)}',
            ),
            onPressed: () async {
              final p = await escolherPontoAdmin(context, d.repositorioCatalogo, selecionado: filtro.pontoId);
              if (p != null && context.mounted) {
                context.go(
                  urlSolicitacoesAdmin(
                    FiltroSolicitacoes(situacao: filtro.situacao, pontoId: p.id, de: filtro.de, ate: filtro.ate),
                  ),
                );
              }
            },
          ),
        );
      },
    ),
    GoRoute(
      path: '/administracao/solicitacoes/:id',
      builder: (context, estado) {
        final id = idDaRota(estado);
        if (id == null) return rotaInvalida(context, destino: '/administracao/resumo');
        return PaginaDetalheSolicitacaoAdmin(
          key: ValueKey('adm-sol-$id'),
          viewModel: DetalheSolicitacaoViewModel(
            repositorio: d.repositorioSolicitacoes,
            catalogo: d.repositorioCatalogo,
            fuso: d.fuso,
            id: id,
          ),
          aoVoltar: () => voltarOu(context, '/administracao/solicitacoes'),
          aoAbrirResponsavel: (tipo, idResponsavel) => context.push(
            tipo == TipoResponsavel.morador
                ? '/administracao/moradores/$idResponsavel'
                : '/administracao/operadores/$idResponsavel',
          ),
        );
      },
    ),
  ];
}

/// Detalhe administrativo: mesmo conteúdo base, sem comandos de situação,
/// com acesso ao cadastro do morador e do operador.
class PaginaDetalheSolicitacaoAdmin extends StatefulWidget {
  const PaginaDetalheSolicitacaoAdmin({
    super.key,
    required this.viewModel,
    required this.aoVoltar,
    required this.aoAbrirResponsavel,
  });

  final DetalheSolicitacaoViewModel viewModel;
  final VoidCallback aoVoltar;
  final void Function(TipoResponsavel tipo, int id) aoAbrirResponsavel;

  @override
  State<PaginaDetalheSolicitacaoAdmin> createState() => _PaginaDetalheSolicitacaoAdminState();
}

class _PaginaDetalheSolicitacaoAdminState extends State<PaginaDetalheSolicitacaoAdmin> {
  late final DetalheSolicitacaoViewModel _vm = widget.viewModel;

  @override
  void initState() {
    super.initState();
    _vm.carregar();
  }

  @override
  void dispose() {
    _vm.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = _vm;
    return ListenableBuilder(
      listenable: vm,
      builder: (context, _) => PaginaRolavel(
        larguraMaxima: Largura.formulario,
        filhos: [
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton.icon(
              onPressed: widget.aoVoltar,
              icon: const Icon(Icons.arrow_back),
              label: const Text('Voltar'),
            ),
          ),
          if (vm.indisponivel)
            EstadoVazio(icone: Icons.search_off, titulo: 'Solicitação indisponível', mensagem: vm.carga.falha?.mensagem)
          else
            ConteudoCarga<SolicitacaoDetalhe>(
              carga: vm.carga,
              aoTentarNovamente: vm.carregar,
              construir: (context, d) => DetalheSolicitacaoView(
                detalhe: d,
                nomePonto: vm.nomePonto,
                fuso: vm.fuso,
                mostrarMorador: true,
                aoAbrirResponsavel: widget.aoAbrirResponsavel,
                acoes: Text(
                  'Consulta administrativa: as mudanças de situação são feitas pelo morador e pelo operador do ponto.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
