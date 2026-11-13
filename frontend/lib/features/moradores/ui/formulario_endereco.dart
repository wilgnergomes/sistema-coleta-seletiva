import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../catalogo/data/modelos.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/envio.dart';

/// Campos do `EnderecoForm`, reutilizados no endereço do morador e no
/// endereço embutido do ponto de coleta.
class CamposEndereco {
  CamposEndereco([Endereco? inicial])
    : logradouro = TextEditingController(text: inicial?.logradouro),
      numero = TextEditingController(text: inicial?.numero == 'S/N' ? '' : inicial?.numero),
      semNumero = ValueNotifier(inicial?.numero == 'S/N'),
      complemento = TextEditingController(text: inicial?.complemento),
      bairro = TextEditingController(text: inicial?.bairro),
      municipio = TextEditingController(text: inicial?.municipio),
      uf = TextEditingController(text: inicial?.uf),
      cep = TextEditingController(text: inicial == null ? null : cepFormatado(inicial.cep));

  final TextEditingController logradouro;
  final TextEditingController numero;
  final ValueNotifier<bool> semNumero;
  final TextEditingController complemento;
  final TextEditingController bairro;
  final TextEditingController municipio;
  final TextEditingController uf;
  final TextEditingController cep;

  Endereco valor() => Endereco(
    logradouro: logradouro.text.trim(),
    numero: semNumero.value ? 'S/N' : numero.text.trim(),
    bairro: bairro.text.trim(),
    municipio: municipio.text.trim(),
    uf: uf.text.trim().toUpperCase(),
    cep: cep.text.replaceAll(RegExp(r'\D'), ''),
    complemento: complemento.text.trim().isEmpty ? null : complemento.text.trim(),
  );

  void dispose() {
    for (final c in [logradouro, numero, complemento, bairro, municipio, uf, cep]) {
      c.dispose();
    }
    semNumero.dispose();
  }
}

/// Grupo de campos de endereço. [prefixo] associa erros aninhados do
/// servidor, como `endereco.cep`.
class GrupoEndereco extends StatelessWidget {
  const GrupoEndereco({super.key, required this.campos, required this.envio, this.prefixo = ''});

  final CamposEndereco campos;
  final EstadoEnvio envio;
  final String prefixo;

  @override
  Widget build(BuildContext context) {
    String? erro(String campo) => envio.erro('$prefixo$campo');
    return ListenableBuilder(
      listenable: Listenable.merge([envio, campos.semNumero]),
      builder: (context, _) => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          CampoTexto(
            controlador: campos.logradouro,
            rotulo: 'Logradouro',
            obrigatorio: true,
            limite: 150,
            ajuda: 'Rua, avenida, travessa…',
            erroServidor: erro('logradouro'),
            autofill: const [AutofillHints.streetAddressLine1],
          ),
          LinhaCampos(
            flex: const [1, 2],
            filhos: [
              CampoTexto(
                controlador: campos.numero,
                rotulo: 'Número',
                obrigatorio: !campos.semNumero.value,
                habilitado: !campos.semNumero.value,
                limite: 10,
                erroServidor: erro('numero'),
                validar: (texto) => texto.toUpperCase() == 'S/N' ? 'Use a opção Sem número.' : null,
              ),
              CampoTexto(
                controlador: campos.complemento,
                rotulo: 'Complemento',
                limite: 100,
                erroServidor: erro('complemento'),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: Espaco.m),
            child: CheckboxListTile(
              value: campos.semNumero.value,
              onChanged: (v) {
                campos.semNumero.value = v ?? false;
                if (campos.semNumero.value) campos.numero.clear();
              },
              title: const Text('Sem número (S/N)'),
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
            ),
          ),
          LinhaCampos(
            filhos: [
              CampoTexto(
                controlador: campos.bairro,
                rotulo: 'Bairro',
                obrigatorio: true,
                limite: 100,
                erroServidor: erro('bairro'),
              ),
              CampoTexto(
                controlador: campos.municipio,
                rotulo: 'Município',
                obrigatorio: true,
                limite: 100,
                erroServidor: erro('municipio'),
                autofill: const [AutofillHints.addressCity],
              ),
            ],
          ),
          LinhaCampos(
            filhos: [
              CampoTexto(
                controlador: campos.uf,
                rotulo: 'UF',
                obrigatorio: true,
                ajuda: 'Duas letras, como SE',
                erroServidor: erro('uf'),
                formatadores: [
                  FilteringTextInputFormatter.allow(RegExp('[A-Za-z]')),
                  LengthLimitingTextInputFormatter(2),
                  _Maiusculas(),
                ],
                validar: (t) => t.isNotEmpty && !RegExp(r'^[A-Z]{2}$').hasMatch(t) ? 'Use duas letras.' : null,
              ),
              CampoTexto(
                controlador: campos.cep,
                rotulo: 'CEP',
                obrigatorio: true,
                ajuda: 'Oito dígitos',
                teclado: TextInputType.number,
                erroServidor: erro('cep'),
                autofill: const [AutofillHints.postalCode],
                formatadores: [
                  FilteringTextInputFormatter.allow(RegExp(r'[\d-]')),
                  LengthLimitingTextInputFormatter(9),
                ],
                validar: (t) =>
                    t.isNotEmpty && t.replaceAll(RegExp(r'\D'), '').length != 8 ? 'O CEP deve ter 8 dígitos.' : null,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Maiusculas extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue anterior, TextEditingValue novo) =>
      novo.copyWith(text: novo.text.toUpperCase());
}

/// Formulário completo de endereço do morador, para diálogo ou tela cheia.
class FormularioEndereco extends StatefulWidget {
  const FormularioEndereco({super.key, required this.salvar, this.inicial, this.rotuloSalvar = 'Salvar endereço'});

  final Endereco? inicial;

  /// Envia ao servidor e devolve o resultado, ou lança `FalhaApi`.
  final Future<Object?> Function(Endereco) salvar;
  final String rotuloSalvar;

  @override
  State<FormularioEndereco> createState() => _FormularioEnderecoState();
}

class _FormularioEnderecoState extends State<FormularioEndereco> {
  final _form = GlobalKey<FormState>();
  late final _campos = CamposEndereco(widget.inicial);
  final _envio = EstadoEnvio();

  @override
  void dispose() {
    _campos.dispose();
    _envio.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (!_form.currentState!.validate()) return;
    final resultado = await _envio.executar(() => widget.salvar(_campos.valor()));
    if (resultado != null && mounted) Navigator.of(context).pop(resultado);
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _form,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(Espaco.l),
        child: ListenableBuilder(
          listenable: _envio,
          builder: (context, _) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const LegendaObrigatorio(),
              MensagemEnvio(estado: _envio),
              GrupoEndereco(campos: _campos, envio: _envio),
              Wrap(
                alignment: WrapAlignment.end,
                spacing: Espaco.s,
                runSpacing: Espaco.s,
                children: [
                  TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Voltar')),
                  BotaoEnvio(rotulo: widget.rotuloSalvar, enviando: _envio.enviando, aoPressionar: _salvar),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
