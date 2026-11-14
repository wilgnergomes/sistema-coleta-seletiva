import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../features/catalogo/data/modelos.dart';
import '../features/catalogo/ui/pagina_detalhe_ponto.dart';
import '../features/catalogo/ui/pagina_materiais.dart';
import '../features/catalogo/ui/pagina_pontos.dart';
import '../features/demonstracao/pagina_demonstracao.dart';
import '../features/inicio/pagina_inicio_publica.dart';
import '../features/moradores/ui/pagina_inicio_morador.dart';
import '../features/moradores/ui/paginas_morador.dart';
import '../ui/compartilhado/estados.dart';
import '../ui/compartilhado/layout.dart';
import '../ui/compartilhado/marca.dart';
import '../ui/compartilhado/movimento.dart';
import 'conexao.dart';
import 'rotas_morador.dart';
import 'rotas_operador.dart';
import 'rotas_admin.dart';
import 'dependencias.dart';
import 'sessao.dart';
import 'shell_app.dart';
import 'tema/tokens.dart';

/// Rota com transição adequada à função: destinos principais com fade curto
/// e detalhes com deslizamento e fade (o retorno faz o caminho inverso).
GoRoute rotaComTransicao({required String path, required GoRouterWidgetBuilder builder, bool detalhe = false}) =>
    GoRoute(
      path: path,
      pageBuilder: (context, estado) => (detalhe ? paginaDetalhe : paginaDestino)(estado, builder(context, estado)),
    );

/// Volta ao item anterior do histórico ou, sem histórico, ao destino dado.
void voltarOu(BuildContext context, String destino) {
  if (context.canPop()) {
    context.pop();
  } else {
    context.go(destino);
  }
}

int? idDaRota(GoRouterState estado, [String parametro = 'id']) {
  final id = int.tryParse(estado.pathParameters[parametro] ?? '');
  return id != null && id > 0 && id <= 2147483647 ? id : null;
}

String urlPontos(FiltroPontos f) =>
    Uri(path: '/pontos', queryParameters: f.paraUrl().isEmpty ? null : f.paraUrl()).toString();

Widget rotaInvalida(BuildContext context, {String? destino}) => PaginaRolavel(
  filhos: [
    EstadoVazio(
      icone: Icons.link_off,
      titulo: 'Página não encontrada',
      mensagem: 'O endereço aberto não corresponde a uma página do aplicativo.',
      acao: FilledButton(
        onPressed: () => context.go(destino ?? '/pontos'),
        child: Text(destino == null ? 'Voltar para pontos' : 'Ir para o início'),
      ),
    ),
  ],
);

/// Retorno aceito após a seleção do ator: só rotas internas do perfil.
String destinoAposSelecao(Ator ator, String? retorno) {
  if (retorno != null && retorno.startsWith('/') && !retorno.startsWith('//')) {
    final caminho = Uri.tryParse(retorno)?.path;
    if (caminho != null && Perfil.daRota(caminho) == ator.perfil) return retorno;
  }
  return ator.perfil.rotaInicial;
}

/// Rotas extras de cada área, acrescentadas pelas etapas seguintes.
typedef RotasArea = List<RouteBase> Function(Dependencias d);

GoRouter criarRotas(Dependencias d, {List<RotasArea> areas = const []}) {
  GoRouter.optionURLReflectsImperativeAPIs = true;
  final catalogo = d.repositorioCatalogo;
  final sessao = d.sessao;
  final demo = d.configuracao.demonstracaoHabilitada;

  return GoRouter(
    initialLocation: '/',
    refreshListenable: sessao,
    errorBuilder: (context, estado) => Scaffold(
      appBar: AppBar(title: const Marca()),
      body: rotaInvalida(context),
    ),
    redirect: (context, estado) {
      final caminho = estado.uri.path;
      if (caminho == '/demonstracao' && !demo) return '/pontos';
      final area = Perfil.daRota(caminho);
      if (area == null) return null;
      if (!demo) return '/pontos';
      final perfil = sessao.perfil;
      if (perfil == null) {
        return Uri(
          path: '/demonstracao',
          queryParameters: {'retorno': estado.uri.toString(), 'perfil': area.name},
        ).toString();
      }
      if (perfil != area) return Uri(path: '/acesso-restrito', queryParameters: {'area': area.name}).toString();
      return null;
    },
    routes: [
      ShellRoute(
        builder: (context, estado, filho) => ShellApp(sessao: sessao, estado: estado, filho: filho, demonstracao: demo),
        routes: [
          rotaComTransicao(
            path: '/',
            builder: (context, estado) => PaginaInicioPublica(
              catalogo: catalogo,
              navegar: NavegacaoInicio(
                pontos: (f) => context.go(urlPontos(f)),
                filtros: (nome) =>
                    context.go(Uri(path: '/pontos', queryParameters: {'filtros': '1', 'nome': ?nome}).toString()),
                materiais: () => context.go('/materiais'),
                material: (id) => context.push('/materiais/$id'),
                cadastro: () => context.go('/cadastro'),
                demonstracao: demo ? () => context.go('/demonstracao') : null,
                solicitar: () {
                  if (sessao.perfil == Perfil.morador) {
                    context.go('/pontos');
                  } else if (demo) {
                    context.go('/demonstracao?perfil=morador&retorno=%2Fmorador%2Finicio');
                  } else {
                    context.go('/cadastro');
                  }
                },
              ),
            ),
          ),
          rotaComTransicao(
            path: '/pontos',
            builder: (context, estado) => PaginaPontos(
              filtro: FiltroPontos.deQuery(estado.uri.queryParameters),
              repositorio: catalogo,
              aoMudarFiltro: (f) => context.go(urlPontos(f)),
              aoAbrirPonto: (id) => context.push('/pontos/$id'),
              abrirFiltrosAoEntrar: estado.uri.queryParameters['filtros'] == '1',
            ),
          ),
          rotaComTransicao(
            detalhe: true,
            path: '/pontos/:id',
            builder: (context, estado) {
              final id = idDaRota(estado);
              if (id == null) return rotaInvalida(context);
              return PaginaDetalhePonto(
                key: ValueKey('ponto-$id'),
                id: id,
                repositorio: catalogo,
                aoVoltar: () => voltarOu(context, '/pontos'),
                aoAbrirMaterial: (m) => context.push('/materiais/$m'),
                acoes: (context, ponto) => _AcaoSolicitar(ponto: ponto, sessao: sessao),
              );
            },
          ),
          rotaComTransicao(
            path: '/materiais',
            builder: (context, estado) {
              final pagina = (int.tryParse(estado.uri.queryParameters['pagina'] ?? '') ?? 1) - 1;
              return PaginaMateriais(
                repositorio: catalogo,
                pagina: pagina < 0 ? 0 : pagina,
                aoMudarPagina: (p) => context.go(p == 0 ? '/materiais' : '/materiais?pagina=${p + 1}'),
                aoAbrir: (id) => context.push('/materiais/$id'),
              );
            },
          ),
          rotaComTransicao(
            detalhe: true,
            path: '/materiais/:id',
            builder: (context, estado) {
              final id = idDaRota(estado);
              if (id == null) return rotaInvalida(context);
              return PaginaDetalheMaterial(
                key: ValueKey('material-$id'),
                id: id,
                repositorio: catalogo,
                aoVoltar: () => voltarOu(context, '/materiais'),
                aoVerPontos: (m) => context.go('/pontos?materialId=$m'),
              );
            },
          ),
          rotaComTransicao(
            path: '/cadastro',
            builder: (context, estado) => PaginaCadastro(
              repositorio: d.repositorioMoradores,
              demonstracao: demo,
              aoVoltarAoCatalogo: () => context.go('/pontos'),
              aoUsarNaDemonstracao: (m) {
                sessao.entrar(Ator(Perfil.morador, m.id, nome: m.nome), respostaValidacao: m);
                context.go('/morador/conta/enderecos');
              },
            ),
          ),
          rotaComTransicao(
            path: '/demonstracao',
            builder: (context, estado) {
              final retorno = estado.uri.queryParameters['retorno'];
              return PaginaDemonstracao(
                key: ValueKey(estado.uri.toString()),
                sessao: sessao,
                validador: d.validadorAtor,
                perfilInicial: Perfil.values.where((p) => p.name == estado.uri.queryParameters['perfil']).firstOrNull,
                aoVoltarAoPublico: () => context.go('/pontos'),
                aoConcluir: (ator, resposta) {
                  sessao.entrar(ator, respostaValidacao: resposta);
                  context.go(destinoAposSelecao(ator, retorno));
                },
              );
            },
          ),
          GoRoute(
            path: '/acesso-restrito',
            builder: (context, estado) {
              final area = Perfil.values.where((p) => p.name == estado.uri.queryParameters['area']).firstOrNull;
              return PaginaRolavel(
                larguraMaxima: Largura.formulario,
                filhos: [
                  EstadoVazio(
                    icone: Icons.lock_outline,
                    titulo: 'Área de outro perfil',
                    mensagem:
                        'Esta página pertence ao perfil ${area?.rotulo ?? 'selecionado'}. '
                        'Volte à sua área ou troque a identidade de demonstração.',
                    acao: Wrap(
                      spacing: Espaco.s,
                      runSpacing: Espaco.s,
                      alignment: WrapAlignment.center,
                      children: [
                        if (sessao.perfil != null)
                          FilledButton(
                            onPressed: () => context.go(sessao.perfil!.rotaInicial),
                            child: const Text('Ir para minha área'),
                          ),
                        OutlinedButton(
                          onPressed: () => context.go('/demonstracao?perfil=${area?.name ?? ''}'),
                          child: const Text('Trocar identidade'),
                        ),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
          GoRoute(
            path: '/morador/inicio',
            builder: (context, estado) => PaginaInicioMorador(
              viewModel: InicioMoradorViewModel(sessao: sessao, moradores: d.repositorioMoradores, catalogo: catalogo),
              aoPesquisar: (f) => context.go(urlPontos(f)),
              aoGerenciarEnderecos: () => context.push('/morador/conta/enderecos'),
              extras: (context, municipio) => secoesRecentes(context, d, municipio),
            ),
          ),
          GoRoute(
            path: '/morador/conta',
            builder: (context, estado) => PaginaConta(
              sessao: sessao,
              repositorio: d.repositorioMoradores,
              aoAbrirEnderecos: () => context.push('/morador/conta/enderecos'),
              aoTrocarIdentidade: () {
                sessao.sair();
                context.go('/demonstracao');
              },
              aoSairDaDemonstracao: () {
                sessao.sair();
                context.go('/');
              },
            ),
          ),
          GoRoute(
            path: '/morador/conta/enderecos',
            builder: (context, estado) => PaginaEnderecos(
              moradorId: sessao.usuarioId!,
              repositorio: d.repositorioMoradores,
              aoVoltar: () => voltarOu(context, '/morador/conta'),
            ),
          ),
          ...rotasSolicitacoesMorador(d),
          ...rotasOperador(d),
          ...rotasAdministracao(d),
          for (final area in areas) ...area(d),
          GoRoute(
            path: '/conexao',
            builder: (context, estado) => PaginaConexao(viewModel: ConexaoViewModel(d.api), base: d.api.base),
          ),
        ],
      ),
    ],
  );
}

/// Ação do detalhe do ponto conforme o contexto: só o morador elegível
/// inicia uma solicitação, e só em ponto ativo com recolhimento domiciliar.
class _AcaoSolicitar extends StatelessWidget {
  const _AcaoSolicitar({required this.ponto, required this.sessao});

  final PontoColeta ponto;
  final Sessao sessao;

  @override
  Widget build(BuildContext context) {
    if (sessao.perfil != Perfil.morador) return const SizedBox.shrink();
    if (!ponto.aceitaSolicitacao) {
      return Text(
        'Este ponto recebe materiais no local e não faz recolhimento domiciliar.',
        style: Theme.of(context).textTheme.bodyMedium,
      );
    }
    return Align(
      alignment: Alignment.centerLeft,
      child: FilledButton.icon(
        onPressed: () => context.push('/morador/solicitacoes/nova?pontoId=${ponto.id}'),
        icon: const Icon(Icons.local_shipping_outlined),
        label: const Text('Solicitar recolhimento'),
      ),
    );
  }
}
