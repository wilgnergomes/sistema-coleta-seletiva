import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/estados.dart';
import '../data/moradores.dart';
import 'formulario_endereco.dart';
import '../../../ui/compartilhado/superficie.dart';

/// Lista e edição dos endereços de um morador. Reutilizada na conta do
/// morador e na administração.
class SecaoEnderecos extends StatefulWidget {
  const SecaoEnderecos({super.key, required this.moradorId, required this.repositorio, this.aoCriar});

  final int moradorId;
  final RepositorioMoradores repositorio;

  /// Chamado com o endereço recém-criado.
  final ValueChanged<EnderecoMorador>? aoCriar;

  @override
  State<SecaoEnderecos> createState() => _SecaoEnderecosState();
}

class _SecaoEnderecosState extends State<SecaoEnderecos> {
  final _carga = Carga<List<EnderecoMorador>>();

  void _carregar() => _carga.executar(() => widget.repositorio.enderecos(widget.moradorId), () {
    if (mounted) setState(() {});
  });

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _criar() async {
    final criado = await abrirFormulario<EnderecoMorador>(
      context,
      titulo: 'Cadastrar endereço',
      construir: (_) => FormularioEndereco(
        rotuloSalvar: 'Cadastrar endereço',
        salvar: (e) => widget.repositorio.criarEndereco(widget.moradorId, e),
      ),
    );
    if (criado == null || !mounted) return;
    mostrarMensagem(context, 'Endereço cadastrado.');
    widget.aoCriar?.call(criado);
    _carregar();
  }

  Future<void> _editar(EnderecoMorador atual) async {
    final salvo = await abrirFormulario<EnderecoMorador>(
      context,
      titulo: 'Editar endereço',
      construir: (_) => FormularioEndereco(
        inicial: atual.endereco,
        salvar: (e) => widget.repositorio.atualizarEndereco(widget.moradorId, atual.id, e),
      ),
    );
    if (salvo == null || !mounted) return;
    mostrarMensagem(context, 'Endereço atualizado.');
    _carregar();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: Elevavel(
            child: FilledButton.icon(
              onPressed: _criar,
              icon: const Icon(Icons.add_location_alt_outlined),
              label: const Text('Cadastrar endereço'),
            ),
          ),
        ),
        const SizedBox(height: Espaco.m),
        ConteudoCarga<List<EnderecoMorador>>(
          carga: _carga,
          rotuloCarregando: 'Carregando endereços…',
          aoTentarNovamente: _carregar,
          construir: (context, lista) {
            if (lista.isEmpty) {
              return const EstadoVazio(
                icone: Icons.home_work_outlined,
                titulo: 'Nenhum endereço cadastrado',
                mensagem: 'Cadastre um endereço para solicitar recolhimento domiciliar.',
              );
            }
            return Column(
              children: [
                for (final e in lista) ...[
                  Card(
                    key: ValueKey(e.id),
                    child: ListTile(
                      contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xs),
                      leading: const Icon(Icons.home_outlined),
                      title: Text(e.endereco.linha1),
                      subtitle: Text(e.endereco.linha2),
                      trailing: IconButton(
                        tooltip: 'Editar endereço ${e.endereco.linha1}',
                        icon: const Icon(Icons.edit_outlined),
                        onPressed: () => _editar(e),
                      ),
                    ),
                  ),
                  const SizedBox(height: Espaco.s),
                ],
              ],
            );
          },
        ),
      ],
    );
  }
}
