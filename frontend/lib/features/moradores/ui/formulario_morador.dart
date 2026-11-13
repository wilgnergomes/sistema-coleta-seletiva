import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/dominio/enums.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/envio.dart';
import '../data/moradores.dart';

/// Campos do `MoradorForm`. A situação só aparece para o administrador.
class FormularioMorador extends StatefulWidget {
  const FormularioMorador({
    super.key,
    required this.salvar,
    required this.rotuloSalvar,
    this.inicial,
    this.permitirSituacao = false,
    this.aoSalvar,
    this.aoVoltar,
    this.confirmarAntes,
  });

  final Morador? inicial;
  final Future<Morador> Function(DadosMorador) salvar;
  final String rotuloSalvar;
  final bool permitirSituacao;
  final ValueChanged<Morador>? aoSalvar;
  final VoidCallback? aoVoltar;

  /// Confirmação opcional antes do envio, como a inativação pelo administrador.
  final Future<bool> Function(DadosMorador dados)? confirmarAntes;

  @override
  State<FormularioMorador> createState() => _FormularioMoradorState();
}

class _FormularioMoradorState extends State<FormularioMorador> {
  final _form = GlobalKey<FormState>();
  late final _nome = TextEditingController(text: widget.inicial?.nome);
  late final _email = TextEditingController(text: widget.inicial?.email);
  late final _fone = TextEditingController(text: widget.inicial?.fone);
  late SituacaoCadastro _situacao = widget.inicial?.situacao ?? SituacaoCadastro.ativo;
  final _envio = EstadoEnvio();

  @override
  void dispose() {
    for (final c in [_nome, _email, _fone]) {
      c.dispose();
    }
    _envio.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (!_form.currentState!.validate()) return;
    final dados = DadosMorador(
      nome: _nome.text,
      email: _email.text,
      fone: _fone.text,
      situacao: widget.permitirSituacao ? _situacao : null,
    );
    if (widget.confirmarAntes != null && !await widget.confirmarAntes!(dados)) return;
    final salvo = await _envio.executar(() => widget.salvar(dados));
    if (salvo != null) widget.aoSalvar?.call(salvo);
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _form,
      child: AutofillGroup(
        child: ListenableBuilder(
          listenable: _envio,
          builder: (context, _) => Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const LegendaObrigatorio(),
              MensagemEnvio(estado: _envio),
              CampoTexto(
                controlador: _nome,
                rotulo: 'Nome',
                obrigatorio: true,
                limite: 80,
                erroServidor: _envio.erro('nome'),
                autofill: const [AutofillHints.name],
              ),
              CampoTexto(
                controlador: _email,
                rotulo: 'E-mail',
                obrigatorio: true,
                limite: 150,
                teclado: TextInputType.emailAddress,
                erroServidor: _envio.erro('email'),
                validar: validarEmail,
                autofill: const [AutofillHints.email],
              ),
              CampoTexto(
                controlador: _fone,
                rotulo: 'Telefone',
                limite: 20,
                teclado: TextInputType.phone,
                erroServidor: _envio.erro('fone'),
                formatadores: [FilteringTextInputFormatter.allow(RegExp(r'[\d\s()+-]'))],
                autofill: const [AutofillHints.telephoneNumber],
              ),
              if (widget.permitirSituacao)
                Padding(
                  padding: const EdgeInsets.only(bottom: Espaco.m),
                  child: DropdownButtonFormField<SituacaoCadastro>(
                    initialValue: _situacao,
                    decoration: InputDecoration(labelText: 'Situação *', errorText: _envio.erro('situacao')),
                    items: const [
                      DropdownMenuItem(value: SituacaoCadastro.ativo, child: Text('Ativo')),
                      DropdownMenuItem(value: SituacaoCadastro.inativo, child: Text('Inativo')),
                    ],
                    onChanged: (v) => setState(() => _situacao = v ?? _situacao),
                  ),
                ),
              Wrap(
                alignment: WrapAlignment.end,
                spacing: Espaco.s,
                runSpacing: Espaco.s,
                children: [
                  if (widget.aoVoltar != null) TextButton(onPressed: widget.aoVoltar, child: const Text('Voltar')),
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
