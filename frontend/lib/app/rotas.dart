import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../ui/compartilhado/marca.dart';
import '../ui/compartilhado/shell.dart';
import 'conexao.dart';
import 'dependencias.dart';

const _destinosPublicos = [
  DestinoNavegacao(rotulo: 'Início', icone: Icons.home_outlined, iconeSelecionado: Icons.home, rota: '/'),
];

GoRouter criarRotas(Dependencias dependencias) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      ShellRoute(
        builder: (context, estado, filho) => ShellAdaptativo(
          titulo: const Marca(),
          destinos: _destinosPublicos,
          indiceSelecionado: 0,
          aoSelecionar: (i) => context.go(_destinosPublicos[i].rota),
          corpo: filho,
        ),
        routes: [
          GoRoute(
            path: '/',
            builder: (context, estado) =>
                PaginaConexao(viewModel: ConexaoViewModel(dependencias.api), base: dependencias.api.base),
          ),
        ],
      ),
    ],
  );
}
