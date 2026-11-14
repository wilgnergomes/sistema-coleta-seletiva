import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/dominio/datas.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/superficie.dart';

/// Dados de uma ação com data futura: agendamento, reagendamento ou falha.
class DadosDataHora {
  const DadosDataHora({required this.instante, this.motivo, this.informacoes});

  final DateTime instante;
  final String? motivo;
  final String? informacoes;
}

/// Formulário de data e hora no fuso de negócio, com motivo e informações
/// da tentativa quando a ação exige. O relógio do servidor decide se a data
/// é futura; aqui só se evita o envio de uma data já passada.
Future<DadosDataHora?> pedirDataHora(
  BuildContext context, {
  required FusoNegocio fuso,
  required String titulo,
  required String descricao,
  required String rotuloAcao,
  bool pedirMotivo = false,
  bool pedirInformacoes = false,
  DateTime? atual,
  DateTime? diferenteDe,
  DateTime Function()? agora,
}) {
  return abrirFormulario<DadosDataHora>(
    context,
    titulo: titulo,
    construir: (_) => _FormularioDataHora(
      fuso: fuso,
      descricao: descricao,
      rotuloAcao: rotuloAcao,
      pedirMotivo: pedirMotivo,
      pedirInformacoes: pedirInformacoes,
      atual: atual,
      diferenteDe: diferenteDe,
      agora: agora ?? DateTime.now,
    ),
  );
}

class _FormularioDataHora extends StatefulWidget {
  const _FormularioDataHora({
    required this.fuso,
    required this.descricao,
    required this.rotuloAcao,
    required this.pedirMotivo,
    required this.pedirInformacoes,
    required this.agora,
    this.atual,
    this.diferenteDe,
  });

  final FusoNegocio fuso;
  final String descricao;
  final String rotuloAcao;
  final bool pedirMotivo;
  final bool pedirInformacoes;
  final DateTime? atual;

  /// O reagendamento precisa mudar o instante marcado.
  final DateTime? diferenteDe;
  final DateTime Function() agora;

  @override
  State<_FormularioDataHora> createState() => _FormularioDataHoraState();
}

class _FormularioDataHoraState extends State<_FormularioDataHora> {
  final _form = GlobalKey<FormState>();
  final _motivo = TextEditingController();
  final _informacoes = TextEditingController();
  DataCivil? _dia;
  TimeOfDay? _hora;
  String? _erroData;

  @override
  void dispose() {
    _motivo.dispose();
    _informacoes.dispose();
    super.dispose();
  }

  Future<void> _escolherDia() async {
    final hoje = widget.fuso.hoje(widget.agora());
    final escolhido = await showDatePicker(
      context: context,
      initialDate: (_dia ?? hoje).comoDateTime,
      firstDate: hoje.comoDateTime,
      lastDate: DateTime(hoje.ano + 2, 12, 31),
      helpText: 'Data',
    );
    if (escolhido != null) setState(() => _dia = DataCivil.deDateTime(escolhido));
  }

  Future<void> _escolherHora() async {
    final escolhida = await showTimePicker(
      context: context,
      initialTime: _hora ?? const TimeOfDay(hour: 9, minute: 0),
      helpText: 'Hora',
    );
    if (escolhida != null) setState(() => _hora = escolhida);
  }

  void _confirmar() {
    final camposValidos = _form.currentState!.validate();
    String? erro;
    DateTime? instante;
    if (_dia == null || _hora == null) {
      erro = 'Escolha a data e a hora.';
    } else {
      try {
        instante = widget.fuso.instante(_dia!, _hora!.hour, _hora!.minute);
        if (!instante.isAfter(widget.agora())) {
          erro = 'Escolha uma data e hora futuras.';
        } else if (widget.diferenteDe != null && instante.isAtSameMomentAs(widget.diferenteDe!)) {
          erro = 'Escolha uma data ou hora diferente da já agendada.';
        }
      } on ArgumentError catch (e) {
        erro = '${e.message}';
      }
    }
    setState(() => _erroData = erro);
    if (!camposValidos || erro != null) return;
    Navigator.of(context).pop(
      DadosDataHora(
        instante: instante!,
        motivo: widget.pedirMotivo ? _motivo.text.trim() : null,
        informacoes: widget.pedirInformacoes ? _informacoes.text.trim() : null,
      ),
    );
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
            Text(widget.descricao, style: texto.bodyMedium),
            if (widget.atual != null) ...[
              const SizedBox(height: Espaco.xs),
              Text('Data atual: ${widget.fuso.dataHora(widget.atual!)}', style: texto.bodySmall),
            ],
            const SizedBox(height: Espaco.m),
            Wrap(
              spacing: Espaco.s,
              runSpacing: Espaco.s,
              children: [
                OutlinedButton.icon(
                  onPressed: _escolherDia,
                  icon: const Icon(Icons.calendar_today_outlined),
                  label: Text(_dia == null ? 'Escolher data *' : 'Data: ${_dia!.exibicao}'),
                ),
                OutlinedButton.icon(
                  onPressed: _escolherHora,
                  icon: const Icon(Icons.schedule),
                  label: Text(
                    _hora == null
                        ? 'Escolher hora *'
                        : 'Hora: ${_hora!.hour.toString().padLeft(2, '0')}:${_hora!.minute.toString().padLeft(2, '0')}',
                  ),
                ),
              ],
            ),
            const SizedBox(height: Espaco.xs),
            Text('Horário de ${widget.fuso.nome}.', style: texto.bodySmall),
            if (_erroData != null)
              Semantics(
                liveRegion: true,
                child: Padding(
                  padding: const EdgeInsets.only(top: Espaco.xs),
                  child: Text(_erroData!, style: texto.bodySmall?.copyWith(color: Cores.erro)),
                ),
              ),
            const SizedBox(height: Espaco.m),
            if (widget.pedirMotivo)
              CampoTexto(controlador: _motivo, rotulo: 'Motivo', obrigatorio: true, limite: 500, linhas: 2),
            if (widget.pedirInformacoes)
              CampoTexto(
                controlador: _informacoes,
                rotulo: 'Informações da tentativa',
                obrigatorio: true,
                limite: 500,
                linhas: 2,
                ajuda: 'O que aconteceu no local.',
              ),
            Wrap(
              alignment: WrapAlignment.end,
              spacing: Espaco.s,
              runSpacing: Espaco.s,
              children: [
                TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Voltar')),
                Elevavel(
                  child: FilledButton(onPressed: _confirmar, child: Text(widget.rotuloAcao)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
