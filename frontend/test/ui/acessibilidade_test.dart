import 'package:coleta_seletiva_web/app/app.dart';
import 'package:coleta_seletiva_web/app/rotas.dart';
import 'package:coleta_seletiva_web/app/sessao.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import '../features/api_falsa.dart';
import '../features/morador_test.dart' show dependencias, enderecoJson, moradorJson;
import '../features/solicitacoes_morador_test.dart' show detalheJson;

/// Diretrizes automáticas (alvo de toque, rótulos e contraste) nas telas
/// principais, nas duas larguras de referência. A conferência com leitor
/// de tela continua manual.
ApiFalsa apiCompleta() => ApiFalsa()
  ..quando(
    'GET /materiais',
    (_) => (
      200,
      pagina([
        materialJson(1, 'Papel'),
        materialJson(2, 'Plástico'),
        materialJson(3, 'Vidro'),
        materialJson(4, 'Metal'),
      ]),
    ),
  )
  ..quando(
    'GET /pontos-coleta',
    (_) => (200, pagina([pontoJson(4, 'Cooperativa Verde Vida'), pontoJson(3, 'Ecoponto Centro', domiciliar: false)])),
  )
  ..quando(
    'GET /pontos-coleta/4',
    (_) => (200, pontoJson(4, 'Cooperativa Verde Vida', materiais: [vinculoJson(1, 'Papel')])),
  )
  ..quando('GET /moradores/3', (_) => (200, moradorJson(3)))
  ..quando('GET /moradores/3/enderecos', (_) => (200, [enderecoJson(7, 3)]))
  ..quando('GET /solicitacoes', (_) => (200, pagina([])))
  ..quando('GET /solicitacoes/5', (_) => (200, detalheJson(5)));

void main() {
  for (final (nome, tela) in [('360 px', celular), ('1366 px', computador)]) {
    for (final (rota, ator) in [
      ('/', null),
      ('/pontos', null),
      ('/pontos/4', null),
      ('/cadastro', null),
      ('/demonstracao', null),
      ('/morador/inicio', const Ator(Perfil.morador, 3, nome: 'Maria Silva')),
      ('/morador/solicitacoes/5', const Ator(Perfil.morador, 3, nome: 'Maria Silva')),
      ('/operador/fila/5', const Ator(Perfil.operador, 1)),
    ]) {
      testWidgets('$rota a $nome atende às diretrizes de alvo, rótulo e contraste', (tester) async {
        final semantica = tester.ensureSemantics();
        definirTela(tester, tela);
        final d = dependencias(apiCompleta());
        if (ator != null) d.sessao.entrar(ator);
        final rotas = criarRotas(d);
        await tester.pumpWidget(AppColetaSeletiva(rotas: rotas));
        rotas.go(rota);
        await tester.pumpAndSettle();
        expect(rotas.state.uri.path, rota);
        expect(tester.takeException(), isNull);
        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        semantica.dispose();
      });
    }
  }

  testWidgets('catálogo a 360 px com texto a 200% não transborda', (tester) async {
    definirTela(tester, celular);
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    final rotas = criarRotas(dependencias(apiCompleta()));
    await tester.pumpWidget(AppColetaSeletiva(rotas: rotas));
    rotas.go('/pontos');
    await tester.pumpAndSettle();
    expect(find.text('Cooperativa Verde Vida'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('navegação por teclado alcança a busca e os filtros do catálogo', (tester) async {
    definirTela(tester, computador);
    final rotas = criarRotas(dependencias(apiCompleta()));
    await tester.pumpWidget(AppColetaSeletiva(rotas: rotas));
    rotas.go('/pontos');
    await tester.pumpAndSettle();
    bool focoDentro(Finder alvo) {
      final contexto = FocusManager.instance.primaryFocus?.context;
      if (contexto == null) return false;
      final elementos = alvo.evaluate().toSet();
      var dentro = false;
      contexto.visitAncestorElements((e) {
        dentro = elementos.contains(e);
        return !dentro;
      });
      return dentro;
    }

    final alcancados = <String>{};
    for (var i = 0; i < 15; i++) {
      await tester.sendKeyEvent(LogicalKeyboardKey.tab);
      await tester.pump();
      for (final dica in ['Buscar', 'Filtros']) {
        if (focoDentro(find.byTooltip(dica))) alcancados.add(dica);
      }
    }
    expect(alcancados, containsAll(['Buscar', 'Filtros']));
  });
}
