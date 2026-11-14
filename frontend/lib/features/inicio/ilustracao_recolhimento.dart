import 'dart:math' as math;

import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import '../../ui/compartilhado/material_visual.dart';

/// Ilustração vetorial original: um operador leva uma caixa de recicláveis
/// até os coletores coloridos em frente a uma casa. Decorativa.
///
/// A animação de entrada roda uma vez (o operador chega e os coletores
/// aparecem) e é desligada com a preferência de movimento reduzido.
class IlustracaoRecolhimento extends StatefulWidget {
  const IlustracaoRecolhimento({super.key, this.altura = 280});

  final double altura;

  @override
  State<IlustracaoRecolhimento> createState() => _IlustracaoRecolhimentoState();
}

class _IlustracaoRecolhimentoState extends State<IlustracaoRecolhimento> with SingleTickerProviderStateMixin {
  late final _controle = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400));

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _controle.value = 1;
    } else if (!_controle.isAnimating && _controle.value == 0) {
      _controle.forward();
    }
  }

  @override
  void dispose() {
    _controle.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: SizedBox(
        height: widget.altura,
        child: AnimatedBuilder(
          animation: _controle,
          builder: (context, _) => CustomPaint(
            size: Size.infinite,
            painter: _PintorRecolhimento(Curves.easeOutCubic.transform(_controle.value)),
          ),
        ),
      ),
    );
  }
}

class _PintorRecolhimento extends CustomPainter {
  _PintorRecolhimento(this.t);

  /// Progresso da entrada, de 0 a 1.
  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    // Cena desenhada num quadro lógico de 400 × 280, ajustado ao espaço.
    final escala = math.min(size.width / 400, size.height / 280);
    canvas.save();
    canvas.translate((size.width - 400 * escala) / 2, (size.height - 280 * escala) / 2);
    canvas.scale(escala);

    final p = Paint();

    // Fundo: céu suave e chão.
    final ceu = RRect.fromRectAndRadius(const Rect.fromLTWH(0, 0, 400, 280), const Radius.circular(24));
    canvas.drawRRect(
      ceu,
      Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFFE6F4EC), Color(0xFFF7F9F7)],
        ).createShader(const Rect.fromLTWH(0, 0, 400, 280)),
    );
    canvas.save();
    canvas.clipRRect(ceu);
    canvas.drawCircle(const Offset(340, 56), 26, p..color = const Color(0xFFF6D88A));
    canvas.drawOval(const Rect.fromLTWH(-40, 214, 480, 120), p..color = const Color(0xFFCFE6D7));

    // Árvore.
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(356, 150, 10, 70), const Radius.circular(4)),
      p..color = const Color(0xFF8A6A4F),
    );
    canvas.drawCircle(const Offset(361, 140), 30, p..color = const Color(0xFF5FA77E));
    canvas.drawCircle(const Offset(343, 154), 20, p..color = const Color(0xFF4E9670));

    // Casa.
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(28, 112, 132, 110), const Radius.circular(6)),
      p..color = Colors.white,
    );
    canvas.drawPath(
      Path()
        ..moveTo(16, 118)
        ..lineTo(94, 60)
        ..lineTo(172, 118)
        ..close(),
      p..color = Cores.primario,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(78, 160, 32, 62), const Radius.circular(4)),
      p..color = const Color(0xFF8A6A4F),
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(42, 134, 26, 24), const Radius.circular(3)),
      p..color = const Color(0xFFBFE0F2),
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(122, 134, 26, 24), const Radius.circular(3)),
      p..color = const Color(0xFFBFE0F2),
    );

    // Coletores nas cores da coleta seletiva, surgindo em sequência.
    final cores = [
      VisualMaterial.papel.cor,
      VisualMaterial.plastico.cor,
      VisualMaterial.vidro.cor,
      VisualMaterial.metal.cor,
    ];
    for (var i = 0; i < 4; i++) {
      final aparecer = ((t - i * 0.12) / 0.5).clamp(0.0, 1.0);
      if (aparecer == 0) continue;
      final x = 176.0 + i * 30;
      canvas.save();
      canvas.translate(x + 12, 222);
      canvas.scale(1, aparecer);
      canvas.drawRRect(
        RRect.fromRectAndRadius(const Rect.fromLTWH(-12, -40, 24, 40), const Radius.circular(4)),
        p..color = cores[i],
      );
      canvas.drawRRect(
        RRect.fromRectAndRadius(const Rect.fromLTWH(-14, -46, 28, 8), const Radius.circular(3)),
        p..color = Color.lerp(cores[i], Colors.black, 0.25)!,
      );
      canvas.restore();
    }

    // Operador chegando pela direita com a caixa de recicláveis.
    final x = 320 - 24 * t;
    final passo = math.sin(t * math.pi * 3) * (1 - t) * 4;
    canvas.save();
    canvas.translate(x, 0);
    // Pernas.
    canvas.drawRRect(
      RRect.fromRectAndRadius(Rect.fromLTWH(-10, 180 + passo, 9, 42 - passo), const Radius.circular(4)),
      p..color = const Color(0xFF304E67),
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(Rect.fromLTWH(2, 180 - passo, 9, 42 + passo), const Radius.circular(4)),
      p..color = const Color(0xFF304E67),
    );
    // Corpo com colete verde e faixa refletiva.
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(-16, 130, 32, 56), const Radius.circular(12)),
      p..color = Cores.primario,
    );
    canvas.drawRect(const Rect.fromLTWH(-16, 160, 32, 5), p..color = const Color(0xFFF2C230));
    // Cabeça e boné.
    canvas.drawCircle(const Offset(0, 116), 13, p..color = const Color(0xFFC98E62));
    canvas.drawArc(const Rect.fromLTWH(-14, 101, 28, 24), math.pi, math.pi, true, p..color = const Color(0xFF176044));
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(-20, 111, 14, 4), const Radius.circular(2)),
      p..color = const Color(0xFF176044),
    );
    // Caixa com materiais e braços.
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(-40, 146, 34, 26), const Radius.circular(4)),
      p..color = const Color(0xFFD9B98C),
    );
    canvas.drawRect(const Rect.fromLTWH(-34, 140, 8, 10), p..color = VisualMaterial.papel.cor);
    canvas.drawRect(const Rect.fromLTWH(-24, 137, 6, 13), p..color = VisualMaterial.vidro.cor);
    canvas.drawRect(const Rect.fromLTWH(-16, 141, 7, 9), p..color = VisualMaterial.plastico.cor);
    canvas.drawRRect(
      RRect.fromRectAndRadius(const Rect.fromLTWH(-30, 150, 22, 7), const Radius.circular(4)),
      p..color = const Color(0xFFC98E62),
    );
    canvas.restore();

    canvas.restore();
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _PintorRecolhimento antigo) => antigo.t != t;
}
