import 'dart:async';

import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';

/// Duração de animação que respeita a preferência de movimento reduzido.
Duration duracao(BuildContext context, [int milissegundos = 240]) =>
    MediaQuery.disableAnimationsOf(context) ? Duration.zero : Duration(milliseconds: milissegundos);

/// Sombras suaves do design system (profundidade sem peso visual).
abstract final class Sombra {
  static const repouso = [BoxShadow(color: Color(0x1424352D), blurRadius: 6, offset: Offset(0, 2))];
  static const elevada = [
    BoxShadow(color: Color(0x1F24352D), blurRadius: 18, offset: Offset(0, 8)),
    BoxShadow(color: Color(0x0F24352D), blurRadius: 4, offset: Offset(0, 1)),
  ];
}

/// Cartão com sombra, hover e pressionamento, usado por cartões clicáveis.
///
/// [altura] fixa a altura dentro da mesma grade, multiplicada pela escala de
/// texto para que o conteúdo continue cabendo com fonte ampliada.
class CartaoInterativo extends StatefulWidget {
  const CartaoInterativo({
    super.key,
    required this.child,
    this.aoTocar,
    this.selecionado = false,
    this.altura,
    this.preenchimento = const EdgeInsets.all(Espaco.m),
    this.rotuloSemantico,
  });

  final Widget child;
  final VoidCallback? aoTocar;
  final bool selecionado;
  final double? altura;
  final EdgeInsetsGeometry preenchimento;
  final String? rotuloSemantico;

  @override
  State<CartaoInterativo> createState() => _CartaoInterativoState();
}

class _CartaoInterativoState extends State<CartaoInterativo> {
  bool _hover = false;
  bool _pressionado = false;

  @override
  Widget build(BuildContext context) {
    final elevado = (_hover || widget.selecionado) && widget.aoTocar != null;
    final escala = MediaQuery.textScalerOf(context).scale(16) / 16;
    final altura = widget.altura == null ? null : widget.altura! * escala.clamp(1.0, 2.4);
    final borda = widget.selecionado ? Cores.primario : (_hover ? Cores.secundarioSuave : Cores.divisor);
    final cartao = AnimatedContainer(
      duration: duracao(context, 220),
      curve: Curves.easeOutCubic,
      height: altura,
      transform: Matrix4.translationValues(0, elevado && !_pressionado ? -2 : 0, 0),
      decoration: BoxDecoration(
        color: Cores.superficie,
        borderRadius: BorderRadius.circular(Raio.cartao),
        border: Border.all(color: borda, width: widget.selecionado ? 2 : 1),
        boxShadow: elevado ? Sombra.elevada : Sombra.repouso,
      ),
      child: Material(
        type: MaterialType.transparency,
        child: InkWell(
          borderRadius: BorderRadius.circular(Raio.cartao),
          onTap: widget.aoTocar,
          onHover: (v) => setState(() => _hover = v),
          onHighlightChanged: (v) => setState(() => _pressionado = v),
          child: Padding(padding: widget.preenchimento, child: widget.child),
        ),
      ),
    );
    if (widget.rotuloSemantico == null) return cartao;
    return Semantics(
      button: widget.aoTocar != null,
      selected: widget.selecionado,
      label: widget.rotuloSemantico,
      child: cartao,
    );
  }
}

/// Efeito de entrada do [Revelar]: cada seção pode usar um diferente.
enum EfeitoEntrada { subir, daEsquerda, daDireita, escala }

/// Faz o filho surgir quando entra na área visível, com fade combinado a
/// deslocamento vertical, horizontal ou escala leve. Com movimento reduzido,
/// aparece imediatamente.
class Revelar extends StatefulWidget {
  const Revelar({
    super.key,
    required this.child,
    this.atraso = Duration.zero,
    this.deslocamento = 24,
    this.efeito = EfeitoEntrada.subir,
  });

  final Widget child;
  final Duration atraso;
  final double deslocamento;
  final EfeitoEntrada efeito;

  @override
  State<Revelar> createState() => _RevelarState();
}

class _RevelarState extends State<Revelar> {
  bool _visivel = false;
  ScrollPosition? _posicao;
  Timer? _atraso;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _posicao?.removeListener(_verificar);
    _posicao = Scrollable.maybeOf(context)?.position;
    _posicao?.addListener(_verificar);
    WidgetsBinding.instance.addPostFrameCallback((_) => _verificar());
  }

  @override
  void dispose() {
    _posicao?.removeListener(_verificar);
    _atraso?.cancel();
    super.dispose();
  }

  void _verificar() {
    if (_visivel || !mounted) return;
    final caixa = context.findRenderObject();
    if (caixa is! RenderBox || !caixa.hasSize) return;
    final topo = caixa.localToGlobal(Offset.zero).dy;
    final alturaTela = MediaQuery.sizeOf(context).height;
    if (topo < alturaTela * 0.92) {
      _atraso = Timer(widget.atraso, () {
        if (mounted) setState(() => _visivel = true);
      });
      _posicao?.removeListener(_verificar);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (MediaQuery.disableAnimationsOf(context)) return widget.child;
    const duracaoEntrada = Duration(milliseconds: 440);
    final d = widget.deslocamento / 400;
    final deslocamento = switch (widget.efeito) {
      EfeitoEntrada.subir => Offset(0, d),
      EfeitoEntrada.daEsquerda => Offset(-d, 0),
      EfeitoEntrada.daDireita => Offset(d, 0),
      EfeitoEntrada.escala => Offset.zero,
    };
    return AnimatedOpacity(
      opacity: _visivel ? 1 : 0,
      // O conteúdo ainda não revelado continua disponível ao leitor de tela.
      alwaysIncludeSemantics: true,
      duration: duracaoEntrada,
      curve: Curves.easeOut,
      child: AnimatedSlide(
        offset: _visivel ? Offset.zero : deslocamento,
        duration: duracaoEntrada,
        curve: Curves.easeOutCubic,
        child: AnimatedScale(
          scale: _visivel || widget.efeito != EfeitoEntrada.escala ? 1 : 0.94,
          duration: duracaoEntrada,
          curve: Curves.easeOutCubic,
          child: widget.child,
        ),
      ),
    );
  }
}
