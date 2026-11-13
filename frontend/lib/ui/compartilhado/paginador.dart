import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import '../../core/api/pagina.dart';

/// Controles `Anterior` e `Próxima` com página atual e total.
class Paginador extends StatelessWidget {
  const Paginador({super.key, required this.pagina, required this.aoMudar, this.desabilitado = false});

  final Pagina<Object?> pagina;
  final ValueChanged<int> aoMudar;
  final bool desabilitado;

  @override
  Widget build(BuildContext context) {
    if (pagina.totalPaginas <= 1 && !pagina.alemDaUltima) return const SizedBox.shrink();
    final total = pagina.totalPaginas == 0 ? 1 : pagina.totalPaginas;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espaco.m),
      child: Wrap(
        alignment: WrapAlignment.center,
        crossAxisAlignment: WrapCrossAlignment.center,
        spacing: Espaco.s,
        runSpacing: Espaco.xs,
        children: [
          OutlinedButton.icon(
            onPressed: desabilitado || !pagina.temAnterior ? null : () => aoMudar(pagina.pagina - 1),
            icon: const Icon(Icons.chevron_left),
            label: const Text('Anterior'),
          ),
          Semantics(
            liveRegion: true,
            child: Text('Página ${pagina.pagina + 1} de $total', style: Theme.of(context).textTheme.bodyMedium),
          ),
          OutlinedButton.icon(
            onPressed: desabilitado || !pagina.temProxima ? null : () => aoMudar(pagina.pagina + 1),
            iconAlignment: IconAlignment.end,
            icon: const Icon(Icons.chevron_right),
            label: const Text('Próxima'),
          ),
        ],
      ),
    );
  }
}
