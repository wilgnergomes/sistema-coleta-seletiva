import 'dart:math' as math;

import 'package:material_ui/material_ui.dart';

import '../../app/tema/tokens.dart';
import 'material_visual.dart';

/// Logomarca: anel com as quatro cores da coleta seletiva em volta de um
/// centro verde.
class Marca extends StatelessWidget {
  const Marca({super.key, this.tamanho = 32, this.comNome = true});

  final double tamanho;
  final bool comNome;

  @override
  Widget build(BuildContext context) {
    final simbolo = SizedBox.square(
      dimension: tamanho,
      child: const CustomPaint(painter: _PintorMarca()),
    );
    if (!comNome) return ExcludeSemantics(child: simbolo);
    return Semantics(
      label: 'Coleta Seletiva',
      excludeSemantics: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          simbolo,
          const SizedBox(width: Espaco.xs),
          Flexible(
            child: Text(
              'Coleta Seletiva',
              style: Theme.of(context).textTheme.titleMedium,
              overflow: TextOverflow.ellipsis,
              maxLines: 1,
            ),
          ),
        ],
      ),
    );
  }
}

class _PintorMarca extends CustomPainter {
  const _PintorMarca();

  @override
  void paint(Canvas canvas, Size size) {
    final centro = size.center(Offset.zero);
    final raio = size.shortestSide / 2;
    final anel = raio * 0.28;
    final cores = [
      VisualMaterial.papel.cor,
      VisualMaterial.plastico.cor,
      VisualMaterial.vidro.cor,
      VisualMaterial.metal.cor,
    ];
    final retangulo = Rect.fromCircle(center: centro, radius: raio - anel / 2);
    const vao = 0.18;
    for (var i = 0; i < 4; i++) {
      canvas.drawArc(
        retangulo,
        -math.pi / 2 + i * math.pi / 2 + vao / 2,
        math.pi / 2 - vao,
        false,
        Paint()
          ..color = cores[i]
          ..style = PaintingStyle.stroke
          ..strokeWidth = anel
          ..strokeCap = StrokeCap.round,
      );
    }
    canvas.drawCircle(centro, raio * 0.42, Paint()..color = Cores.primario);
    // Folha estilizada no centro.
    final folha = Path()
      ..moveTo(centro.dx, centro.dy - raio * 0.24)
      ..quadraticBezierTo(centro.dx + raio * 0.26, centro.dy, centro.dx, centro.dy + raio * 0.24)
      ..quadraticBezierTo(centro.dx - raio * 0.26, centro.dy, centro.dx, centro.dy - raio * 0.24);
    canvas.drawPath(folha, Paint()..color = Colors.white);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
