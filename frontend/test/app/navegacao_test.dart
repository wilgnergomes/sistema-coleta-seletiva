import 'package:coleta_seletiva_web/app/navegacao.dart';
import 'package:coleta_seletiva_web/app/sessao.dart';
import 'package:coleta_seletiva_web/app/shell_app.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/pagina_detalhe_ponto.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/pagina_materiais.dart';
import 'package:coleta_seletiva_web/features/catalogo/ui/pagina_pontos.dart';
import 'package:coleta_seletiva_web/features/inicio/pagina_inicio_publica.dart';
import 'package:coleta_seletiva_web/features/moradores/ui/pagina_inicio_morador.dart';
import 'package:coleta_seletiva_web/features/moradores/ui/paginas_morador.dart';
import 'package:coleta_seletiva_web/features/solicitacoes/ui/solicitacoes_morador.dart';
import 'package:coleta_seletiva_web/ui/compartilhado/movimento.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../apoio.dart';
import '../features/refinamento_test.dart' show abrir;

/// Posição horizontal da página no meio da transição (40% de 340 ms):
/// positiva quando entra pela direita, negativa quando entra pela esquerda.
Future<double> meioDaTransicao(WidgetTester tester, GoRouter rotas, String destino, Finder pagina) async {
  rotas.go(destino);
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 136));
  final x = tester.getTopLeft(pagina).dx;
  await tester.pumpAndSettle();
  return x;
}

/// Início da área de conteúdo: à direita da navegação lateral, quando houver.
double inicioConteudo(WidgetTester tester, Finder pagina) => tester.getTopLeft(pagina).dx;

void main() {
  group('direção entre destinos', () {
    test('segue a ordem da navegação e é neutra fora dela', () {
      expect(direcaoEntre(destinosPublicos, 0, destinosPublicos, 1), DirecaoNavegacao.avanca);
      expect(direcaoEntre(destinosPublicos, 2, destinosPublicos, 1), DirecaoNavegacao.recua);
      expect(direcaoEntre(destinosPublicos, 0, destinosPublicos, 2), DirecaoNavegacao.avanca);
      expect(direcaoEntre(destinosPublicos, 1, destinosPublicos, 1), DirecaoNavegacao.neutra);
      expect(direcaoEntre(destinosPublicos, null, destinosPublicos, 1), DirecaoNavegacao.neutra);
      expect(direcaoEntre(destinosPublicos, 1, destinosPublicos, null), DirecaoNavegacao.neutra);
      // Troca de área (perfil): sem direção presumida.
      expect(direcaoEntre(destinosPublicos, 0, destinosMorador, 1), DirecaoNavegacao.neutra);
    });

    testWidgets('público: Início → Pontos → Materiais pela direita, voltando pela esquerda', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester);

      expect(await meioDaTransicao(tester, rotas, '/pontos', find.byType(PaginaPontos)), greaterThan(40));
      expect(await meioDaTransicao(tester, rotas, '/materiais', find.byType(PaginaMateriais)), greaterThan(40));
      expect(await meioDaTransicao(tester, rotas, '/pontos', find.byType(PaginaPontos)), lessThan(-40));
      expect(await meioDaTransicao(tester, rotas, '/', find.byType(PaginaInicioPublica)), lessThan(-40));
      // Início → Materiais direto, sem passar por Pontos.
      expect(await meioDaTransicao(tester, rotas, '/materiais', find.byType(PaginaMateriais)), greaterThan(40));
      expect(await meioDaTransicao(tester, rotas, '/', find.byType(PaginaInicioPublica)), lessThan(-40));
      // Ao fim, a página ocupa a sua posição.
      expect(tester.getTopLeft(find.byType(PaginaInicioPublica)).dx, 0);
    });

    testWidgets('a página anterior continua visível embaixo e recua no sentido oposto', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester);
      rotas.go('/pontos');
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 136));
      expect(find.byType(PaginaInicioPublica), findsOneWidget);
      expect(tester.getTopLeft(find.byType(PaginaInicioPublica)).dx, lessThan(0));
      await tester.pumpAndSettle();
      expect(find.byType(PaginaInicioPublica), findsNothing);
    });

    testWidgets('mudar só o filtro não anima como troca de página', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester, rota: '/pontos');
      rotas.go('/pontos?nome=Verde');
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 136));
      expect(tester.getTopLeft(find.byType(PaginaPontos)).dx, 0);
      expect(find.byType(PaginaPontos), findsOneWidget);
      await tester.pumpAndSettle();
    });

    testWidgets('detalhe entra pela direita e volta pelo mesmo caminho', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester, rota: '/pontos');
      rotas.push('/pontos/4');
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      final entrando = find.byType(PaginaDetalhePonto);
      expect(tester.getTopLeft(entrando).dx, greaterThan(0));
      await tester.pumpAndSettle();
      expect(tester.getTopLeft(entrando).dx, 0);
      rotas.pop();
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 120));
      expect(tester.getTopLeft(entrando).dx, greaterThan(0));
      // A lista, embaixo, não se desloca ao abrir ou fechar um detalhe.
      expect(tester.getTopLeft(find.byType(PaginaPontos)).dx, 0);
      await tester.pumpAndSettle();
      expect(entrando, findsNothing);
    });

    testWidgets('morador: Início → Pontos → Solicitações → Conta e o caminho de volta', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester, ator: const Ator(Perfil.morador, 3, nome: 'Ana'), rota: '/morador/inicio');
      final base = inicioConteudo(tester, find.byType(PaginaInicioMorador));
      Future<double> relativo(String destino, Finder pagina) async =>
          await meioDaTransicao(tester, rotas, destino, pagina) - base;

      expect(await relativo('/pontos', find.byType(PaginaPontos)), greaterThan(40));
      expect(await relativo('/morador/solicitacoes', find.byType(PaginaSolicitacoes)), greaterThan(40));
      expect(await relativo('/morador/conta', find.byType(PaginaConta)), greaterThan(40));
      expect(await relativo('/morador/inicio', find.byType(PaginaInicioMorador)), lessThan(-40));
    });

    testWidgets('troca de perfil é neutra: sem deslocamento horizontal', (tester) async {
      definirTela(tester, computador);
      await abrir(tester, ator: const Ator(Perfil.morador, 3, nome: 'Ana'), rota: '/morador/conta');
      // Sair da demonstração leva à página inicial pública, fora da ordem anterior.
      await tester.tap(find.text('Sair da demonstração').first);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 136));
      // Só a escala mínima do fade neutro (poucos pixels), sem deslizamento.
      expect(tester.getTopLeft(find.byType(PaginaInicioPublica)).dx.abs(), lessThan(12));
      await tester.pumpAndSettle();
    });

    testWidgets('com movimento reduzido, a troca é imediata', (tester) async {
      definirTela(tester, computador);
      final rotas = await abrir(tester);
      tester.platformDispatcher.accessibilityFeaturesTestValue = const FakeAccessibilityFeatures(
        disableAnimations: true,
      );
      addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);
      await tester.pump();
      rotas.go('/pontos');
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 136));
      expect(tester.getTopLeft(find.byType(PaginaPontos)).dx, 0);
      await tester.pumpAndSettle();
    });
  });
}
