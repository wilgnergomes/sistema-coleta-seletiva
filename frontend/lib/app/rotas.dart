import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../features/catalogo/data/modelos.dart';
import '../features/catalogo/ui/pagina_detalhe_ponto.dart';
import '../features/catalogo/ui/pagina_materiais.dart';
import '../features/catalogo/ui/pagina_pontos.dart';
import '../ui/compartilhado/estados.dart';
import '../ui/compartilhado/layout.dart';
import '../ui/compartilhado/marca.dart';
import '../ui/compartilhado/shell.dart';
import 'conexao.dart';
import 'dependencias.dart';

const destinosPublicos = [
  DestinoNavegacao(rotulo: 'Pontos', icone: Icons.place_outlined, iconeSelecionado: Icons.place, rota: '/pontos'),
  DestinoNavegacao(
    rotulo: 'Materiais',
    icone: Icons.recycling_outlined,
    iconeSelecionado: Icons.recycling,
    rota: '/materiais',
  ),
];

int? indiceDestino(List<DestinoNavegacao> destinos, String caminho) {
  for (var i = 0; i < destinos.length; i++) {
    final rota = destinos[i].rota;
    if (caminho == rota || caminho.startsWith('$rota/')) return i;
  }
  return null;
}

/// Volta ao item anterior do histórico ou, sem histórico, ao destino dado.
void voltarOu(BuildContext context, String destino) {
  if (context.canPop()) {
    context.pop();
  } else {
    context.go(destino);
  }
}

int? _id(GoRouterState estado, [String parametro = 'id']) {
  final id = int.tryParse(estado.pathParameters[parametro] ?? '');
  return id != null && id > 0 ? id : null;
}

Widget _rotaInvalida(BuildContext context) => PaginaRolavel(
  filhos: [
    EstadoVazio(
      icone: Icons.link_off,
      titulo: 'Página não encontrada',
      mensagem: 'O endereço aberto não corresponde a uma página do aplicativo.',
      acao: FilledButton(onPressed: () => context.go('/pontos'), child: const Text('Voltar para pontos')),
    ),
  ],
);

GoRouter criarRotas(Dependencias d) {
  GoRouter.optionURLReflectsImperativeAPIs = true;
  final catalogo = d.repositorioCatalogo;
  return GoRouter(
    initialLocation: '/pontos',
    errorBuilder: (context, estado) => Scaffold(
      appBar: AppBar(title: const Marca()),
      body: _rotaInvalida(context),
    ),
    routes: [
      GoRoute(path: '/', redirect: (_, _) => '/pontos'),
      ShellRoute(
        builder: (context, estado, filho) => ShellAdaptativo(
          titulo: const Marca(),
          destinos: destinosPublicos,
          indiceSelecionado: indiceDestino(destinosPublicos, estado.uri.path),
          aoSelecionar: (i) => context.go(destinosPublicos[i].rota),
          acoes: [
            if (MediaQuery.sizeOf(context).width < 600)
              PopupMenuButton<String>(
                tooltip: 'Menu',
                icon: const Icon(Icons.menu),
                onSelected: (rota) => context.go(rota),
                itemBuilder: (context) => [
                  for (final destino in destinosPublicos)
                    PopupMenuItem(value: destino.rota, child: Text(destino.rotulo)),
                ],
              ),
          ],
          corpo: filho,
        ),
        routes: [
          GoRoute(
            path: '/pontos',
            builder: (context, estado) => PaginaPontos(
              filtro: FiltroPontos.deQuery(estado.uri.queryParameters),
              repositorio: catalogo,
              aoMudarFiltro: (f) => context.go(Uri(path: '/pontos', queryParameters: f.paraUrl()).toString()),
              aoAbrirPonto: (id) => context.push('/pontos/$id'),
            ),
          ),
          GoRoute(
            path: '/pontos/:id',
            builder: (context, estado) {
              final id = _id(estado);
              if (id == null) return _rotaInvalida(context);
              return PaginaDetalhePonto(
                key: ValueKey('ponto-$id'),
                id: id,
                repositorio: catalogo,
                aoVoltar: () => voltarOu(context, '/pontos'),
                aoAbrirMaterial: (m) => context.push('/materiais/$m'),
              );
            },
          ),
          GoRoute(
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
          GoRoute(
            path: '/materiais/:id',
            builder: (context, estado) {
              final id = _id(estado);
              if (id == null) return _rotaInvalida(context);
              return PaginaDetalheMaterial(
                key: ValueKey('material-$id'),
                id: id,
                repositorio: catalogo,
                aoVoltar: () => voltarOu(context, '/materiais'),
                aoVerPontos: (m) => context.go('/pontos?materialId=$m'),
              );
            },
          ),
          GoRoute(
            path: '/conexao',
            builder: (context, estado) => PaginaConexao(viewModel: ConexaoViewModel(d.api), base: d.api.base),
          ),
        ],
      ),
    ],
  );
}
