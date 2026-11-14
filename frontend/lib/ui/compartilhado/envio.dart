import 'package:material_ui/material_ui.dart';

import '../../core/api/falhas.dart';
import 'estados.dart';
import 'superficie.dart';

/// Estado de um envio de formulário: progresso, erros por campo e mensagem
/// geral. Preserva o rascunho em qualquer falha.
class EstadoEnvio extends ChangeNotifier {
  bool enviando = false;
  String? mensagem;
  TipoAviso tipoMensagem = TipoAviso.erro;
  Map<String, String> errosCampo = const {};
  FalhaApi? falha;

  /// Executa [acao] impedindo envio duplicado. Devolve o resultado ou nulo
  /// em falha, deixando a falha registrada para a tela.
  Future<T?> executar<T>(Future<T> Function() acao) async {
    if (enviando) return null;
    enviando = true;
    mensagem = null;
    errosCampo = const {};
    falha = null;
    notifyListeners();
    try {
      return await acao();
    } on ErroHttp catch (erro) {
      falha = erro;
      errosCampo = erro.errosPorCampo;
      tipoMensagem = erro.conflito ? TipoAviso.alerta : TipoAviso.erro;
      mensagem = erro.mensagem;
      // Erros sem campo (como enum inválido) entram no resumo.
      final semCampo = erro.erros.where((e) => e.campo == null || e.campo!.isEmpty).map((e) => e.mensagem);
      if (semCampo.isNotEmpty) mensagem = [erro.mensagem, ...semCampo].join(' ');
      return null;
    } on FalhaApi catch (erro) {
      falha = erro;
      tipoMensagem = TipoAviso.erro;
      mensagem = erro.mensagem;
      return null;
    } finally {
      enviando = false;
      notifyListeners();
    }
  }

  /// Erro do servidor para o campo, com prefixo opcional (`endereco.`).
  String? erro(String campo) => errosCampo[campo];

  void limpar() {
    mensagem = null;
    errosCampo = const {};
    falha = null;
    notifyListeners();
  }
}

/// Mensagem geral do envio, quando houver.
class MensagemEnvio extends StatelessWidget {
  const MensagemEnvio({super.key, required this.estado, this.acao});

  final EstadoEnvio estado;
  final Widget? acao;

  @override
  Widget build(BuildContext context) {
    final mensagem = estado.mensagem;
    if (mensagem == null) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Aviso(tipo: estado.tipoMensagem, mensagem: mensagem, acao: acao),
    );
  }
}

/// Botão principal que mostra o progresso sem perder o rótulo.
class BotaoEnvio extends StatelessWidget {
  const BotaoEnvio({
    super.key,
    required this.rotulo,
    required this.enviando,
    required this.aoPressionar,
    this.icone,
    this.autofocus = false,
  });

  final String rotulo;
  final bool enviando;
  final VoidCallback? aoPressionar;
  final IconData? icone;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    final indicador = const SizedBox.square(
      dimension: 18,
      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
    );
    return Semantics(
      liveRegion: enviando,
      child: Elevavel(
        child: FilledButton.icon(
          autofocus: autofocus,
          onPressed: enviando ? null : aoPressionar,
          icon: enviando ? indicador : (icone == null ? null : Icon(icone)),
          label: Text(enviando ? '$rotulo…' : rotulo),
        ),
      ),
    );
  }
}
