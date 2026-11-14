import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';

/// Campo de texto com rótulo persistente, marca de obrigatório, contador
/// quando há limite e erro do servidor associado ao campo.
class CampoTexto extends StatelessWidget {
  const CampoTexto({
    super.key,
    required this.controlador,
    required this.rotulo,
    this.obrigatorio = false,
    this.limite,
    this.ajuda,
    this.erroServidor,
    this.validar,
    this.teclado,
    this.linhas = 1,
    this.formatadores,
    this.aoEnviar,
    this.acaoTeclado,
    this.habilitado = true,
    this.autofill,
    this.prefixo,
    this.foco,
  });

  final TextEditingController controlador;
  final String rotulo;
  final bool obrigatorio;
  final int? limite;
  final String? ajuda;
  final String? erroServidor;
  final String? Function(String texto)? validar;
  final TextInputType? teclado;
  final int linhas;
  final List<TextInputFormatter>? formatadores;
  final ValueChanged<String>? aoEnviar;
  final TextInputAction? acaoTeclado;
  final bool habilitado;
  final Iterable<String>? autofill;
  final Widget? prefixo;
  final FocusNode? foco;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: Espaco.m),
      child: TextFormField(
        controller: controlador,
        focusNode: foco,
        enabled: habilitado,
        keyboardType: linhas > 1 ? TextInputType.multiline : teclado,
        textInputAction: linhas > 1 ? TextInputAction.newline : acaoTeclado,
        minLines: linhas > 1 ? linhas : null,
        maxLines: linhas > 1 ? null : 1,
        maxLength: limite,
        inputFormatters: formatadores,
        onFieldSubmitted: aoEnviar,
        autofillHints: autofill,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        decoration: InputDecoration(
          labelText: obrigatorio ? '$rotulo *' : rotulo,
          helperText: ajuda,
          prefixIcon: prefixo,
          alignLabelWithHint: linhas > 1,
          // Erro do servidor pela decoração: aparece junto ao campo sem
          // bloquear a validação local de um novo envio (forceErrorText
          // faria validate() falhar até o próximo envio).
          errorText: erroServidor,
        ),
        validator: (valor) {
          final texto = valor?.trim() ?? '';
          if (obrigatorio && texto.isEmpty) return 'Informe ${rotulo.toLowerCase()}.';
          if (limite != null && texto.length > limite!) return 'Use no máximo $limite caracteres.';
          return validar?.call(texto);
        },
      ),
    );
  }
}

/// Legenda da marca de obrigatório.
class LegendaObrigatorio extends StatelessWidget {
  const LegendaObrigatorio({super.key});

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: Espaco.m),
    child: Text('Campos marcados com * são obrigatórios.', style: Theme.of(context).textTheme.bodySmall),
  );
}

/// Validação de e-mail equivalente à aceitação comum do servidor; a
/// decisão final é do backend.
String? validarEmail(String texto) {
  if (texto.isEmpty) return null;
  final valido = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(texto);
  return valido ? null : 'Informe um e-mail válido.';
}

/// Dispõe dois campos lado a lado quando há 560 dp úteis.
class LinhaCampos extends StatelessWidget {
  const LinhaCampos({super.key, required this.filhos, this.flex});

  final List<Widget> filhos;
  final List<int>? flex;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, restricoes) {
        if (restricoes.maxWidth < 560) {
          return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: filhos);
        }
        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (var i = 0; i < filhos.length; i++) ...[
              if (i > 0) const SizedBox(width: Espaco.m),
              Expanded(flex: flex?[i] ?? 1, child: filhos[i]),
            ],
          ],
        );
      },
    );
  }
}

/// Abre um cadastro pelo número, usando a consulta por ID da API. Não é uma
/// pesquisa por nome: a API atual não oferece esse filtro.
class AbrirPorNumero extends StatefulWidget {
  const AbrirPorNumero({super.key, required this.rotulo, required this.aoAbrir});

  final String rotulo;
  final ValueChanged<int> aoAbrir;

  @override
  State<AbrirPorNumero> createState() => _AbrirPorNumeroState();
}

class _AbrirPorNumeroState extends State<AbrirPorNumero> {
  final _numero = TextEditingController();
  String? _erro;

  @override
  void dispose() {
    _numero.dispose();
    super.dispose();
  }

  void _abrir() {
    final id = int.tryParse(_numero.text.trim());
    if (id == null || id < 1 || id > 2147483647) {
      setState(() => _erro = 'Informe um número inteiro positivo.');
      return;
    }
    setState(() => _erro = null);
    widget.aoAbrir(id);
  }

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 360),
      child: TextField(
        controller: _numero,
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(10)],
        onSubmitted: (_) => _abrir(),
        decoration: InputDecoration(
          labelText: widget.rotulo,
          errorText: _erro,
          suffixIcon: IconButton(tooltip: 'Abrir cadastro', icon: const Icon(Icons.arrow_forward), onPressed: _abrir),
        ),
      ),
    );
  }
}
