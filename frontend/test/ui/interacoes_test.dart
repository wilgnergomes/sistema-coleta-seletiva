import 'package:coleta_seletiva_web/app/tema/tema.dart';
import 'package:coleta_seletiva_web/app/tema/tokens.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/modelos.dart';
import 'package:coleta_seletiva_web/features/catalogo/data/repositorio_catalogo.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/componentes_catalogo.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/material_visual.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/superficie.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import '../features/api_falsa.dart' show materialJson;

/// Deslocamento vertical aplicado pelo [Elevavel] (negativo: subiu).
double elevacao(WidgetTester tester) {
  final container = tester.widget<AnimatedContainer>(
    find.descendant(of: find.byType(Elevavel), matching: find.byType(AnimatedContainer)).first,
  );
  return container.transform!.getTranslation().y;
}

Future<TestGesture> passarMouse(WidgetTester tester, Finder alvo) async {
  final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
  await mouse.addPointer(location: Offset.zero);
  addTearDown(mouse.removePointer);
  await mouse.moveTo(tester.getCenter(alvo));
  await tester.pumpAndSettle();
  return mouse;
}

MaterialColeta material(int id, String nome) => MaterialColeta.deJson(materialJson(id, nome));

void main() {
  group('ações elevadas', () {
    testWidgets('o botão principal sobe no hover, com sombra maior e seta que avança', (tester) async {
      await tester.pumpWidget(
        comTema(
          Center(
            child: Elevavel(
              child: FilledButton.icon(
                onPressed: () {},
                icon: const IconeDirecional(Icons.arrow_forward),
                label: const Text('Continuar'),
              ),
            ),
          ),
        ),
      );
      expect(elevacao(tester), 0);
      final repouso = tester.widget<Material>(
        find.descendant(of: find.byType(FilledButton), matching: find.byType(Material)),
      );
      await passarMouse(tester, find.byType(FilledButton));
      expect(elevacao(tester), -2);
      final hover = tester.widget<Material>(
        find.descendant(of: find.byType(FilledButton), matching: find.byType(Material)),
      );
      expect(hover.elevation, greaterThan(repouso.elevation));
      expect(hover.color, Cores.primarioHover);
      expect(tester.widget<AnimatedSlide>(find.byType(AnimatedSlide)).offset.dx, greaterThan(0));
      expect(tester.widget<Material>(find.byType(Material).last).shape, isA<StadiumBorder>());
    });

    testWidgets('botão desabilitado não sobe', (tester) async {
      await tester.pumpWidget(
        comTema(
          const Center(
            child: Elevavel(child: FilledButton(onPressed: null, child: Text('Indisponível'))),
          ),
        ),
      );
      await passarMouse(tester, find.byType(FilledButton));
      expect(elevacao(tester), 0);
    });

    testWidgets('ação destrutiva mantém o vermelho, com hover próprio', (tester) async {
      await tester.pumpWidget(
        comTema(
          Center(
            child: FilledButton(style: estiloDestrutivo, onPressed: () {}, child: const Text('Recusar')),
          ),
        ),
      );
      Color? cor() => tester
          .widget<Material>(find.descendant(of: find.byType(FilledButton), matching: find.byType(Material)))
          .color;
      expect(cor(), Cores.erro);
      await passarMouse(tester, find.byType(FilledButton));
      expect(cor(), Cores.erroHover);
    });
  });

  group('atalhos circulares de materiais', () {
    final catalogo = CatalogoMateriais(
      itens: [material(1, 'Papel'), material(2, 'Plástico'), material(3, 'Vidro'), material(4, 'Metal')],
      completo: true,
    );

    Decoration circulo(WidgetTester tester, String nome) => tester
        .widget<AnimatedContainer>(
          find.descendant(of: find.bySemanticsLabel(RegExp('^$nome')), matching: find.byType(AnimatedContainer)),
        )
        .decoration!;

    testWidgets('hover: sobe com borda fina na cor do material; seleção: anel espesso e selo', (tester) async {
      final semantica = tester.ensureSemantics();
      await tester.pumpWidget(
        comTema(
          Scaffold(
            body: AtalhosMateriais(catalogo: catalogo, selecionado: 3, aoSelecionar: (_) {}),
          ),
        ),
      );
      final papel = circulo(tester, 'Papel') as BoxDecoration;
      expect((papel.border! as Border).top.color, Colors.transparent);
      expect(papel.boxShadow, Sombra.repouso);

      await passarMouse(tester, find.bySemanticsLabel('Papel'));
      final hover = circulo(tester, 'Papel') as BoxDecoration;
      expect((hover.border! as Border).top.color, VisualMaterial.papel.cor);
      expect((hover.border! as Border).top.width, 2);
      expect(hover.boxShadow, Sombra.elevada);

      final vidro = circulo(tester, 'Vidro') as BoxDecoration;
      expect((vidro.border! as Border).top.color, VisualMaterial.vidro.cor);
      expect((vidro.border! as Border).top.width, 3);
      expect(find.byIcon(Icons.check), findsOneWidget);
      expect(find.bySemanticsLabel('Vidro, selecionado'), findsOneWidget);
      // Continuam circulares.
      expect(hover.shape, BoxShape.circle);
      semantica.dispose();
    });
  });
}
