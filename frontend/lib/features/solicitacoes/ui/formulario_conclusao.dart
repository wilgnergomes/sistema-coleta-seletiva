import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/dominio/quantidade.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../data/solicitacoes.dart';

/// Conclusão (RF13, RN02): uma quantidade efetiva explícita para cada item
/// gravado, não negativa, com pelo menos uma positiva e `UN` inteira.
Future<Map<int, Quantidade>?> pedirQuantidadesEfetivas(BuildContext context, SolicitacaoDetalhe detalhe) {
  return abrirFormulario<Map<int, Quantidade>>(
    context,
    titulo: 'Concluir recolhimento',
    construir: (_) => _FormularioConclusao(detalhe: detalhe),
  );
}

class _FormularioConclusao extends StatefulWidget {
  const _FormularioConclusao({required this.detalhe});

  final SolicitacaoDetalhe detalhe;

  @override
  State<_FormularioConclusao> createState() => _FormularioConclusaoState();
}

class _FormularioConclusaoState extends State<_FormularioConclusao> {
  final _form = GlobalKey<FormState>();
  late final _campos = {for (final i in widget.detalhe.itens) i.materialId: TextEditingController()};
  String? _erroGeral;

  @override
  void dispose() {
    for (final c in _campos.values) {
      c.dispose();
    }
    super.dispose();
  }

  void _confirmar() {
    if (!_form.currentState!.validate()) return;
    final valores = {
      for (final i in widget.detalhe.itens)
        i.materialId: LeituraQuantidade.de(_campos[i.materialId]!.text, unidade: i.unidade, permitirZero: true).valor!,
    };
    if (!valores.values.any((q) => q.positiva)) {
      setState(() => _erroGeral = 'Informe pelo menos uma quantidade recolhida maior que zero.');
      return;
    }
    Navigator.of(context).pop(valores);
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Form(
      key: _form,
      child: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(Espaco.l, Espaco.m, Espaco.l, Espaco.l + MediaQuery.viewInsetsOf(context).bottom),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Informe o que foi recolhido em ${widget.detalhe.protocolo}. Use 0 para o material não recolhido. '
              'A data e o responsável da conclusão são registrados pelo servidor.',
              style: texto.bodyMedium,
            ),
            const SizedBox(height: Espaco.m),
            for (final item in widget.detalhe.itens)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: Espaco.s),
                    child: IconeMaterial(nome: item.nomeMaterial, tamanho: 28),
                  ),
                  const SizedBox(width: Espaco.s),
                  Expanded(
                    child: CampoTexto(
                      controlador: _campos[item.materialId]!,
                      rotulo: '${item.nomeMaterial} recolhido (${item.unidade.simbolo})',
                      obrigatorio: true,
                      ajuda: 'Estimado: ${item.quantidadeEstimada.comUnidade(item.unidade)}',
                      teclado: const TextInputType.numberWithOptions(decimal: true),
                      validar: (t) => LeituraQuantidade.de(t, unidade: item.unidade, permitirZero: true).erro,
                    ),
                  ),
                ],
              ),
            if (_erroGeral != null)
              Semantics(
                liveRegion: true,
                child: Padding(
                  padding: const EdgeInsets.only(bottom: Espaco.s),
                  child: Text(_erroGeral!, style: texto.bodySmall?.copyWith(color: Cores.erro)),
                ),
              ),
            Wrap(
              alignment: WrapAlignment.end,
              spacing: Espaco.s,
              runSpacing: Espaco.s,
              children: [
                TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Voltar')),
                FilledButton(onPressed: _confirmar, child: const Text('Concluir recolhimento')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
