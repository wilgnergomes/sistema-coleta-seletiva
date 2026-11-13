import 'package:material_ui/material_ui.dart';

import '../core/api/cliente_api.dart';
import '../core/api/falhas.dart';
import '../core/api/json.dart';
import '../core/api/pagina.dart';

/// Verifica a entrada do app: uma leitura pública real pela base `/api`.
class ConexaoViewModel extends ChangeNotifier {
  ConexaoViewModel(this._api);

  final ClienteApi _api;

  bool verificando = false;
  int? totalPontos;
  String? falha;

  Future<void> verificar() async {
    verificando = true;
    falha = null;
    notifyListeners();
    try {
      final json = await _api.get('/pontos-coleta', query: {'page': 0, 'size': 1});
      totalPontos = Pagina<Json>.deJson(json, (item) => item).totalElementos;
    } on FalhaApi catch (erro) {
      totalPontos = null;
      falha = erro.mensagem;
    } finally {
      verificando = false;
      notifyListeners();
    }
  }
}

class PaginaConexao extends StatefulWidget {
  const PaginaConexao({super.key, required this.viewModel, required this.base});

  final ConexaoViewModel viewModel;
  final Uri base;

  @override
  State<PaginaConexao> createState() => _PaginaConexaoState();
}

class _PaginaConexaoState extends State<PaginaConexao> {
  @override
  void initState() {
    super.initState();
    widget.viewModel.verificar();
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 520),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: ListenableBuilder(
            listenable: widget.viewModel,
            builder: (context, _) {
              final vm = widget.viewModel;
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Estado da conexão', style: texto.headlineSmall),
                  const SizedBox(height: 8),
                  Text('API: ${widget.base}', style: texto.bodyMedium),
                  const SizedBox(height: 24),
                  if (vm.verificando)
                    const Row(
                      children: [
                        SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2)),
                        SizedBox(width: 12),
                        Text('Verificando a conexão com a API…'),
                      ],
                    )
                  else if (vm.falha != null)
                    Text(vm.falha!, style: texto.bodyLarge?.copyWith(color: Theme.of(context).colorScheme.error))
                  else
                    Text(
                      'Conexão com a API funcionando. ${vm.totalPontos} ponto(s) de coleta ativo(s) no catálogo.',
                      style: texto.bodyLarge,
                    ),
                  const SizedBox(height: 24),
                  OutlinedButton.icon(
                    onPressed: vm.verificando ? null : vm.verificar,
                    icon: const Icon(Icons.refresh),
                    label: const Text('Verificar novamente'),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}
