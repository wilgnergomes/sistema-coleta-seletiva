import 'dart:math' as math;
import 'dart:ui' show lerpDouble;

import 'package:material_ui/material_ui.dart';

import 'material_visual.dart';

/// Logomarca: símbolo universal da reciclagem (laço de Möbius), com três
/// setas que se dobram nos cantos de um triângulo. A dobra mostra o lado de
/// baixo da faixa e a parte de cima passa sobre ela, separada por um recorte.
///
/// Cores da Resolução CONAMA 275/2001: azul (papel), vermelho (plástico) e
/// verde (vidro) nas setas; amarelo (metal) na dobra da seta verde.
class Marca extends StatelessWidget {
  const Marca({super.key, this.tamanho = 32, this.comNome = true});

  final double tamanho;
  final bool comNome;

  @override
  Widget build(BuildContext context) {
    final simbolo = SimboloReciclagem(tamanho: tamanho);
    if (!comNome) return ExcludeSemantics(child: simbolo);
    return Semantics(
      label: 'Coleta Seletiva',
      excludeSemantics: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          simbolo,
          const SizedBox(width: 8),
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

/// Só o símbolo, decorativo.
class SimboloReciclagem extends StatelessWidget {
  const SimboloReciclagem({super.key, required this.tamanho, this.rotacao = 0});

  final double tamanho;

  /// Rotação em voltas, para animações discretas.
  final double rotacao;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: tamanho,
      child: CustomPaint(painter: PintorReciclagem(rotacao: rotacao)),
    );
  }
}

class _Seta {
  const _Seta(this.principal, this.dobra);

  final Color principal;
  final Color dobra;
}

/// Desenha o símbolo num quadrado de lado `size.shortestSide`.
class PintorReciclagem extends CustomPainter {
  const PintorReciclagem({this.rotacao = 0});

  final double rotacao;

  static Color _escurecer(Color c, double f) => Color.from(alpha: c.a, red: c.r * f, green: c.g * f, blue: c.b * f);

  static final _setas = [
    _Seta(VisualMaterial.papel.cor, _escurecer(VisualMaterial.papel.cor, 0.62)),
    _Seta(VisualMaterial.plastico.cor, _escurecer(VisualMaterial.plastico.cor, 0.62)),
    _Seta(VisualMaterial.vidro.cor, const Color(0xFFD9A600)),
  ];

  // Cada seta contorna um canto do triângulo: chega pela aresta anterior
  // (lado de baixo da faixa, mais escuro), dobra no canto e segue pela aresta
  // seguinte (lado de cima), terminando na cabeça. Proporções sobre a aresta.
  static const _inicioCauda = 0.50; // na aresta anterior, a partir do vértice anterior
  static const _fimFaixa = 0.22; // na aresta seguinte, a partir do canto
  static const _pontaSeta = 0.42; // ponta da cabeça
  static const _largura = 0.27; // largura da faixa, em unidades do raio
  static const _abaSeta = 0.27; // meia largura da cabeça da seta
  static const _recorte = 0.05; // separação que mostra a sobreposição

  @override
  void paint(Canvas canvas, Size size) {
    final lado = size.shortestSide;
    const raio = 1.0;
    canvas.save();
    canvas.translate(size.width / 2, size.height / 2);
    canvas.scale(lado / 2 * 0.80);
    // O triângulo tem o centroide na origem; desce um quarto do raio para
    // centralizar a caixa visual (topo em -r, base em r/2).
    canvas.translate(0, raio * 0.25);
    canvas.rotate(rotacao * 2 * math.pi);

    final vertices = [
      for (var k = 0; k < 3; k++)
        Offset(
          raio * math.cos(-math.pi / 2 + k * 2 * math.pi / 3),
          raio * math.sin(-math.pi / 2 + k * 2 * math.pi / 3),
        ),
    ];

    for (var k = 0; k < 3; k++) {
      final canto = vertices[k];
      final seguinte = vertices[(k + 1) % 3];
      final anterior = vertices[(k + 2) % 3];
      _desenharSeta(canvas, anterior, canto, seguinte, _setas[k]);
    }
    canvas.restore();
  }

  Offset _ponto(Offset de, Offset para, double t) =>
      Offset(lerpDouble(de.dx, para.dx, t)!, lerpDouble(de.dy, para.dy, t)!);

  void _desenharSeta(Canvas canvas, Offset anterior, Offset canto, Offset seguinte, _Seta seta) {
    final cauda = _ponto(anterior, canto, _inicioCauda);
    final fim = _ponto(canto, seguinte, _fimFaixa);
    final ponta = _ponto(canto, seguinte, _pontaSeta);
    final saida = (seguinte - canto) / (seguinte - canto).distance;
    final normal = Offset(-saida.dy, saida.dx);

    canvas.saveLayer(null, Paint());

    // 1. Lado de baixo: a faixa chega ao canto e dobra (junção em ponta),
    // preenchendo a dobra que fica visível ao redor da parte de cima.
    final aposCanto = canto + saida * (_largura * 0.5);
    canvas.drawPath(
      Path()
        ..moveTo(cauda.dx, cauda.dy)
        ..lineTo(canto.dx, canto.dy)
        ..lineTo(aposCanto.dx, aposCanto.dy),
      Paint()
        ..color = seta.dobra
        ..style = PaintingStyle.stroke
        ..strokeWidth = _largura
        ..strokeJoin = StrokeJoin.miter
        ..strokeMiterLimit = 2.2,
    );

    // 2. Recorte transparente: a parte de cima passa sobre a dobra.
    final inicio = canto - saida * (_largura * 0.62);
    final faixa = Path()
      ..moveTo(inicio.dx, inicio.dy)
      ..lineTo(fim.dx, fim.dy);
    canvas.drawPath(
      faixa,
      Paint()
        ..blendMode = BlendMode.clear
        ..style = PaintingStyle.stroke
        ..strokeWidth = _largura + _recorte * 2,
    );

    // 3. Lado de cima e cabeça da seta.
    canvas.drawPath(
      faixa,
      Paint()
        ..color = seta.principal
        ..style = PaintingStyle.stroke
        ..strokeWidth = _largura,
    );
    final base = fim - saida * 0.01;
    canvas.drawPath(
      Path()
        ..moveTo(base.dx + normal.dx * _abaSeta, base.dy + normal.dy * _abaSeta)
        ..lineTo(ponta.dx, ponta.dy)
        ..lineTo(base.dx - normal.dx * _abaSeta, base.dy - normal.dy * _abaSeta)
        ..close(),
      Paint()..color = seta.principal,
    );
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant PintorReciclagem antigo) => antigo.rotacao != rotacao;
}
