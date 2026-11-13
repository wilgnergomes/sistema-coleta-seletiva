import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';

/// Confirmação simples com `Voltar` e a ação pelo nome.
Future<bool> confirmar(
  BuildContext context, {
  required String titulo,
  required String mensagem,
  required String rotuloAcao,
  bool destrutiva = false,
}) async {
  final resposta = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(titulo),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: Largura.dialogo),
        child: Text(mensagem),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Voltar')),
        FilledButton(
          style: destrutiva ? FilledButton.styleFrom(backgroundColor: Cores.erro) : null,
          onPressed: () => Navigator.of(context).pop(true),
          child: Text(rotuloAcao),
        ),
      ],
    ),
  );
  return resposta ?? false;
}

/// Abre um formulário em diálogo no amplo e em tela cheia no compacto ou
/// em pouca altura, mantendo título e ações.
Future<T?> abrirFormulario<T>(BuildContext context, {required String titulo, required WidgetBuilder construir}) {
  final tamanho = MediaQuery.sizeOf(context);
  final telaCheia = tamanho.width < 600 || tamanho.height < 560;
  if (telaCheia) {
    return Navigator.of(context).push<T>(
      MaterialPageRoute(
        fullscreenDialog: true,
        builder: (context) => Scaffold(
          appBar: AppBar(title: Text(titulo)),
          body: SafeArea(child: construir(context)),
        ),
      ),
    );
  }
  return showDialog<T>(
    context: context,
    builder: (context) => Dialog(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: Largura.dialogo, maxHeight: tamanho.height * 0.9),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(Espaco.l, Espaco.l, Espaco.l, 0),
              child: Semantics(header: true, child: Text(titulo, style: Theme.of(context).textTheme.titleLarge)),
            ),
            Flexible(child: construir(context)),
          ],
        ),
      ),
    ),
  );
}

void mostrarMensagem(BuildContext context, String mensagem) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(mensagem)));
}
