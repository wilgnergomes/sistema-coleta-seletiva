import 'dart:math' as math;

import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import 'superficie.dart';

/// Linguagem de movimento do produto: valores de referência.
abstract final class Movimento {
  static const hover = Duration(milliseconds: 180);
  static const entrada = Duration(milliseconds: 420);
  static const rota = Duration(milliseconds: 260);
  static const rotaDestino = Duration(milliseconds: 340);
  static const rotaDetalhe = Duration(milliseconds: 320);
  static const rolagem = Duration(milliseconds: 560);
  static const curvaEntrada = Curves.easeOutCubic;
  static const curvaSaida = Curves.easeInCubic;
}

/// Entrada coordenada: um único controlador, com cada filho num intervalo
/// próprio (atraso pequeno e intencional), deslocamento de 24 dp e fade.
/// Roda uma vez; com movimento reduzido, mostra tudo de imediato.
class EntradaCoordenada extends StatefulWidget {
  const EntradaCoordenada({super.key, required this.construir, this.duracaoTotal = const Duration(milliseconds: 1100)});

  /// Recebe uma função que embrulha cada parte com o seu intervalo.
  final Widget Function(BuildContext context, Widget Function(int ordem, Widget filho, {Offset deslocamento}) entrada)
  construir;
  final Duration duracaoTotal;

  @override
  State<EntradaCoordenada> createState() => _EntradaCoordenadaState();
}

class _EntradaCoordenadaState extends State<EntradaCoordenada> with SingleTickerProviderStateMixin {
  late final _controle = AnimationController(vsync: this, duration: widget.duracaoTotal);

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _controle.value = 1;
    } else if (_controle.value == 0 && !_controle.isAnimating) {
      _controle.forward();
    }
  }

  @override
  void dispose() {
    _controle.dispose();
    super.dispose();
  }

  Widget _entrada(int ordem, Widget filho, {Offset deslocamento = const Offset(0, 24)}) {
    final inicio = (ordem * 0.12).clamp(0.0, 0.6);
    final curva = CurvedAnimation(
      parent: _controle,
      curve: Interval(inicio, (inicio + 0.45).clamp(0.0, 1.0), curve: Movimento.curvaEntrada),
    );
    return AnimatedBuilder(
      animation: curva,
      builder: (context, child) => Opacity(
        opacity: curva.value,
        child: Transform.translate(offset: deslocamento * (1 - curva.value), child: child),
      ),
      child: filho,
    );
  }

  @override
  Widget build(BuildContext context) => widget.construir(context, _entrada);
}

/// Botão de destaque da página pública: maior presença, elevação no hover,
/// compressão ao pressionar e anel de foco visível.
class BotaoDestaque extends StatefulWidget {
  const BotaoDestaque({
    super.key,
    required this.rotulo,
    required this.aoPressionar,
    this.icone = Icons.arrow_forward,
    this.claro = false,
  });

  final String rotulo;
  final VoidCallback aoPressionar;
  final IconData icone;

  /// Versão clara para fundos escuros.
  final bool claro;

  @override
  State<BotaoDestaque> createState() => _BotaoDestaqueState();
}

class _BotaoDestaqueState extends State<BotaoDestaque> {
  bool _hover = false;
  bool _pressionado = false;
  bool _foco = false;

  @override
  Widget build(BuildContext context) {
    final fundo = widget.claro ? Colors.white : (_hover ? Cores.primarioHover : Cores.primario);
    final frente = widget.claro ? Cores.primario : Colors.white;
    final escala = _pressionado ? 0.97 : 1.0;
    return Semantics(
      button: true,
      label: widget.rotulo,
      excludeSemantics: true,
      child: FocusableActionDetector(
        onShowFocusHighlight: (v) => setState(() => _foco = v),
        onShowHoverHighlight: (v) => setState(() => _hover = v),
        actions: {ActivateIntent: CallbackAction<ActivateIntent>(onInvoke: (_) => widget.aoPressionar())},
        mouseCursor: SystemMouseCursors.click,
        child: GestureDetector(
          onTapDown: (_) => setState(() => _pressionado = true),
          onTapCancel: () => setState(() => _pressionado = false),
          onTapUp: (_) => setState(() => _pressionado = false),
          onTap: widget.aoPressionar,
          child: AnimatedScale(
            scale: escala,
            duration: duracao(context, 120),
            child: AnimatedContainer(
              duration: duracao(context, Movimento.hover.inMilliseconds),
              curve: Curves.easeOut,
              transform: Matrix4.translationValues(0, _hover && !_pressionado ? -2 : 0, 0),
              constraints: const BoxConstraints(minHeight: 56),
              padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 16),
              decoration: BoxDecoration(
                color: fundo,
                borderRadius: BorderRadius.circular(Raio.pilula),
                border: Border.all(color: _foco ? Cores.foco : Colors.transparent, width: 2),
                boxShadow: [
                  BoxShadow(
                    color: (widget.claro ? Colors.black : Cores.primario).withValues(alpha: _hover ? 0.28 : 0.18),
                    blurRadius: _hover ? 22 : 12,
                    offset: Offset(0, _hover ? 10 : 5),
                  ),
                ],
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Flexible(
                    child: Text(
                      widget.rotulo,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(color: frente),
                      textAlign: TextAlign.center,
                    ),
                  ),
                  const SizedBox(width: Espaco.s),
                  AnimatedSlide(
                    offset: Offset(_hover ? 0.25 : 0, 0),
                    duration: duracao(context, Movimento.hover.inMilliseconds),
                    child: Icon(widget.icone, color: frente, size: 22),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Link secundário discreto, com sublinhado que cresce no hover.
class LinkDiscreto extends StatefulWidget {
  const LinkDiscreto({super.key, required this.rotulo, required this.aoPressionar, this.claro = false});

  final String rotulo;
  final VoidCallback aoPressionar;
  final bool claro;

  @override
  State<LinkDiscreto> createState() => _LinkDiscretoState();
}

class _LinkDiscretoState extends State<LinkDiscreto> {
  bool _ativo = false;

  @override
  Widget build(BuildContext context) {
    final cor = widget.claro ? Colors.white : Cores.texto;
    return TextButton(
      onPressed: widget.aoPressionar,
      onHover: (v) => setState(() => _ativo = v),
      onFocusChange: (v) => setState(() => _ativo = v),
      style: TextButton.styleFrom(
        foregroundColor: cor,
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 14),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(widget.rotulo, style: Theme.of(context).textTheme.titleSmall?.copyWith(color: cor)),
          AnimatedContainer(
            duration: duracao(context, Movimento.hover.inMilliseconds),
            margin: const EdgeInsets.only(top: 3),
            height: 2,
            width: _ativo ? 40 : 16,
            color: widget.claro ? Colors.white : Cores.primario,
          ),
        ],
      ),
    );
  }
}

/// Direção espacial de uma troca de destino principal.
enum DirecaoNavegacao {
  /// Destino à direita na ordem da navegação: a nova página entra pela direita.
  avanca,

  /// Destino à esquerda: a nova página entra pela esquerda.
  recua,

  /// Sem ordem espacial (troca de perfil, redirecionamento, página avulsa).
  neutra,
}

/// Transição entre destinos principais. Com [direcao] definida, a nova página
/// desliza sobre a anterior a partir do lado do destino, com sombra durante o
/// movimento, enquanto a anterior recua um pouco e perde destaque. Sem
/// direção, faz um fade com escala mínima. [saida] informa, à página que sai,
/// a direção da troca em andamento.
CustomTransitionPage<void> paginaDestino(
  GoRouterState estado,
  Widget filho, {
  DirecaoNavegacao direcao = DirecaoNavegacao.neutra,
  DirecaoNavegacao Function()? saida,
}) => CustomTransitionPage<void>(
  key: estado.pageKey,
  child: filho,
  transitionDuration: Movimento.rotaDestino,
  reverseTransitionDuration: Movimento.rotaDestino,
  transitionsBuilder: (context, animacao, secundaria, child) => TransicaoDestino(
    entrada: animacao,
    saida: secundaria,
    direcao: direcao,
    direcaoDaSaida: saida ?? () => DirecaoNavegacao.neutra,
    child: child,
  ),
);

/// Movimento de uma página de destino: entrada pela [entrada] e saída pela
/// [saida] (a animação da página que a cobre).
class TransicaoDestino extends StatelessWidget {
  const TransicaoDestino({
    super.key,
    required this.entrada,
    required this.saida,
    required this.direcao,
    required this.direcaoDaSaida,
    required this.child,
  });

  final Animation<double> entrada;
  final Animation<double> saida;
  final DirecaoNavegacao direcao;
  final DirecaoNavegacao Function() direcaoDaSaida;
  final Widget child;

  /// Deslocamento de entrada: perceptível sem virar um carrossel.
  static double deslocamento(double largura) => math.min(largura * 0.3, 360);

  static double _sinal(DirecaoNavegacao d) => switch (d) {
    DirecaoNavegacao.avanca => 1,
    DirecaoNavegacao.recua => -1,
    DirecaoNavegacao.neutra => 0,
  };

  @override
  Widget build(BuildContext context) {
    if (MediaQuery.disableAnimationsOf(context)) return child;
    final largura = MediaQuery.sizeOf(context).width;
    // O fundo opaco faz a nova página cobrir a anterior em vez de misturar-se a ela.
    final pagina = ColoredBox(color: Cores.fundo, child: child);
    return ClipRect(
      child: AnimatedBuilder(
        animation: Listenable.merge([entrada, saida]),
        child: pagina,
        builder: (context, pagina) {
          final e = entrada.value;
          final curva = entrada.status == AnimationStatus.reverse ? Movimento.curvaSaida : Movimento.curvaEntrada;
          final t = curva.transform(e);
          final s = Movimento.curvaEntrada.transform(saida.value);
          final sinal = _sinal(direcao);
          final sinalSaida = _sinal(direcaoDaSaida());
          final direcional = sinal != 0;
          final opacidade = Curves.easeOut.transform(direcional ? (e / 0.55).clamp(0.0, 1.0) : e);
          final dx = sinal * deslocamento(largura) * (1 - t) - sinalSaida * math.min(largura * 0.08, 96) * s;
          final escala = (direcional ? 1.0 : 0.985 + 0.015 * t) - (sinalSaida == 0 ? 0 : 0.02 * s);
          final sombra = direcional ? 0.18 * math.sin(math.pi * e) : 0.0;
          return Opacity(
            opacity: (opacidade * (1 - (sinalSaida == 0 ? 0 : 0.4 * s))).clamp(0.0, 1.0),
            child: Transform.translate(
              offset: Offset(dx, 0),
              child: Transform.scale(
                scale: escala,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    boxShadow: [
                      BoxShadow(
                        color: Cores.texto.withValues(alpha: sombra),
                        blurRadius: 32,
                        offset: Offset(-sinal * 8, 0),
                      ),
                    ],
                  ),
                  child: pagina,
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// Lista para detalhe: o detalhe entra deslizando 32 dp da direita com fade,
/// e sai pelo mesmo caminho ao voltar (continuidade de direção).
CustomTransitionPage<void> paginaDetalhe(GoRouterState estado, Widget filho) => CustomTransitionPage<void>(
  key: estado.pageKey,
  child: filho,
  transitionDuration: Movimento.rotaDetalhe,
  reverseTransitionDuration: Movimento.rota,
  transitionsBuilder: (context, animacao, secundaria, child) {
    if (MediaQuery.disableAnimationsOf(context)) return child;
    final curva = CurvedAnimation(parent: animacao, curve: Movimento.curvaEntrada, reverseCurve: Movimento.curvaSaida);
    final largura = MediaQuery.sizeOf(context).width;
    return FadeTransition(
      opacity: curva,
      child: SlideTransition(
        position: Tween(begin: Offset(32 / (largura == 0 ? 1 : largura), 0), end: Offset.zero).animate(curva),
        child: ColoredBox(color: Cores.fundo, child: child),
      ),
    );
  },
);
